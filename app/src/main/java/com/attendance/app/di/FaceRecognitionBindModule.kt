package com.attendance.app.di

import com.attendance.app.facerecognition.FaceRecognitionEngine
import com.attendance.app.facerecognition.MlKitTfLiteFaceRecognitionEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Consumers (ViewModels for Face Enrollment / Mark Attendance) depend only on
 * FaceRecognitionEngine — never on MlKitFaceDetector or FaceEmbedder directly. If the detection
 * or embedding model ever changes, only this binding and the *Impl class change.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FaceRecognitionBindModule {

    @Binds
    abstract fun bindFaceRecognitionEngine(impl: MlKitTfLiteFaceRecognitionEngine): FaceRecognitionEngine
}
