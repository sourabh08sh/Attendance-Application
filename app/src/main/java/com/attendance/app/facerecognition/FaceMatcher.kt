package com.attendance.app.facerecognition

import kotlin.math.sqrt

/**
 * Compares two face embeddings by cosine similarity. Pure math, no Android/ML Kit/TFLite
 * dependency, so it's covered by plain JUnit tests that run on the JVM with no emulator needed.
 */
object FaceMatcher {

    // Starting point only — needs empirical tuning once real enrollment/attendance captures
    // exist (lighting, camera quality, and the specific model all affect the right value).
    // Easy to revisit without touching detection or embedding code.
    const val DEFAULT_MATCH_THRESHOLD = 0.70f

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) {
            "Embeddings must be the same size to compare (got ${a.size} vs ${b.size})"
        }
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        if (normA == 0f || normB == 0f) return 0f
        return dot / (sqrt(normA) * sqrt(normB))
    }

    fun isMatch(similarity: Float, threshold: Float = DEFAULT_MATCH_THRESHOLD): Boolean =
        similarity >= threshold
}
