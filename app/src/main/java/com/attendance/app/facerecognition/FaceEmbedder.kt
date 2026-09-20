package com.attendance.app.facerecognition

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Wraps a TFLite face-embedding model (MobileFaceNet architecture). Community .tflite exports of
 * this model vary more than you'd expect: some use TFLite's usual NHWC layout, others keep the
 * original NCHW layout from a PyTorch/ONNX source; some are float32, others are UINT8-quantized
 * (with a correspondingly quantized output that needs dequantizing). This class detects all of
 * that from the model's own tensor metadata at load time instead of assuming one combination —
 * see the crash this replaced: assuming NHWC+float32 against an NCHW+UINT8 model silently built
 * a buffer 1/1394th the size TFLite expected.
 *
 * If the model file is missing or its shape can't be interpreted, this degrades gracefully
 * (isModelLoaded = false) instead of crashing the app. See
 * app/src/main/assets/PLACE_MODEL_HERE.txt for where to get a model file.
 */
class FaceEmbedder(context: Context) {

    private var interpreter: Interpreter? = null

    // Input tensor properties, read from the model rather than assumed.
    private var inputWidth: Int = 0
    private var inputHeight: Int = 0
    private var inputIsChannelsFirst: Boolean = false // true = NCHW [1,3,H,W], false = NHWC [1,H,W,3]
    private var inputIsQuantized: Boolean = false

    // Output tensor properties.
    private var outputBatchSize: Int = 1
    private var embeddingSize: Int = 0
    private var outputDataType: DataType = DataType.FLOAT32
    private var outputScale: Float = 1f
    private var outputZeroPoint: Int = 0

    init {
        interpreter = try {
            val model = loadModelFile(context, MODEL_FILENAME)
            Interpreter(model).also { interp ->
                val inputTensor = interp.getInputTensor(0)
                val inputShape = inputTensor.shape() // 4 dims, but axis order isn't guaranteed

                inputIsChannelsFirst = when {
                    inputShape.size == 4 && inputShape[3] == 3 -> false // NHWC: [1, H, W, 3]
                    inputShape.size == 4 && inputShape[1] == 3 -> true  // NCHW: [1, 3, H, W]
                    else -> {
                        Log.w(TAG, "Unrecognized input shape ${inputShape.toList()}; assuming NHWC")
                        false
                    }
                }
                if (inputIsChannelsFirst) {
                    inputHeight = inputShape[2]
                    inputWidth = inputShape[3]
                } else {
                    inputHeight = inputShape[1]
                    inputWidth = inputShape[2]
                }
                inputIsQuantized = inputTensor.dataType() == DataType.UINT8

                val outputTensor = interp.getOutputTensor(0)
                val outputShape = outputTensor.shape() // e.g. [1, 128] or, as seen in the wild, [2, 128]
                outputBatchSize = outputShape[0]
                embeddingSize = outputShape.last()
                if (outputBatchSize != 1) {
                    Log.w(
                        TAG,
                        "Output tensor batch size is $outputBatchSize (shape ${outputShape.toList()}), " +
                            "not 1. Allocating for the full shape and using row 0 as the embedding for " +
                            "the single face processed per call."
                    )
                }
                outputDataType = outputTensor.dataType()
                if (outputDataType == DataType.UINT8 || outputDataType == DataType.INT8) {
                    val params = outputTensor.quantizationParams()
                    outputScale = params.scale
                    outputZeroPoint = params.zeroPoint
                }

                Log.i(
                    TAG,
                    "Loaded $MODEL_FILENAME — input ${inputWidth}x$inputHeight " +
                        "(${if (inputIsChannelsFirst) "NCHW" else "NHWC"}, " +
                        "${if (inputIsQuantized) "UINT8" else "FLOAT32"}), " +
                        "output batch=$outputBatchSize, embedding size $embeddingSize ($outputDataType)"
                )
            }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Face embedding model not available or incompatible — face verification will not " +
                    "work until a compatible $MODEL_FILENAME is in app/src/main/assets/. " +
                    "See app/src/main/assets/PLACE_MODEL_HERE.txt.",
                e
            )
            null
        }
    }

    val isModelLoaded: Boolean
        get() = interpreter != null

    /** Returns null if the model isn't loaded. [faceBitmap] should already be cropped to the face. */
    fun embed(faceBitmap: Bitmap): FloatArray? {
        val interp = interpreter ?: return null
        val inputBuffer = preprocess(faceBitmap)

        return if (outputDataType == DataType.UINT8 || outputDataType == DataType.INT8) {
            val output = Array(outputBatchSize) { ByteArray(embeddingSize) }
            interp.run(inputBuffer, output)
            FloatArray(embeddingSize) { i ->
                val raw = output[0][i]
                val intValue = if (outputDataType == DataType.UINT8) (raw.toInt() and 0xFF) else raw.toInt()
                (intValue - outputZeroPoint) * outputScale
            }
        } else {
            val output = Array(outputBatchSize) { FloatArray(embeddingSize) }
            interp.run(inputBuffer, output)
            output[0]
        }
    }

    private fun preprocess(bitmap: Bitmap): ByteBuffer {
        val resized = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
        val pixels = IntArray(inputWidth * inputHeight)
        resized.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)

        val bytesPerChannel = if (inputIsQuantized) 1 else 4
        val buffer = ByteBuffer
            .allocateDirect(bytesPerChannel * inputWidth * inputHeight * 3)
            .order(ByteOrder.nativeOrder())

        if (inputIsChannelsFirst) {
            // NCHW: all R values for the whole image, then all G, then all B.
            writeChannelPlanar(buffer, pixels, shift = 16) // R
            writeChannelPlanar(buffer, pixels, shift = 8)  // G
            writeChannelPlanar(buffer, pixels, shift = 0)  // B
        } else {
            // NHWC: R, G, B interleaved per pixel.
            for (pixel in pixels) {
                writeChannelValue(buffer, (pixel shr 16) and 0xFF)
                writeChannelValue(buffer, (pixel shr 8) and 0xFF)
                writeChannelValue(buffer, pixel and 0xFF)
            }
        }

        buffer.rewind()
        return buffer
    }

    private fun writeChannelPlanar(buffer: ByteBuffer, pixels: IntArray, shift: Int) {
        for (pixel in pixels) {
            writeChannelValue(buffer, (pixel shr shift) and 0xFF)
        }
    }

    private fun writeChannelValue(buffer: ByteBuffer, value: Int) {
        if (inputIsQuantized) {
            buffer.put(value.toByte())
        } else {
            // Normalize to [-1, 1] — standard preprocessing for MobileFaceNet-style float models.
            buffer.putFloat((value - 127.5f) / 128f)
        }
    }

    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    companion object {
        private const val TAG = "FaceEmbedder"
        const val MODEL_FILENAME = "mobilefacenet.tflite"
    }
}
