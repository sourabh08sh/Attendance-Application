package com.attendance.app.facerecognition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceMatcherTest {

    @Test
    fun identicalEmbeddings_haveSimilarityOfOne() {
        val embedding = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)
        val similarity = FaceMatcher.cosineSimilarity(embedding, embedding)
        assertEquals(1.0f, similarity, 0.0001f)
    }

    @Test
    fun oppositeEmbeddings_haveSimilarityOfNegativeOne() {
        val a = floatArrayOf(1f, 0f)
        val b = floatArrayOf(-1f, 0f)
        assertEquals(-1.0f, FaceMatcher.cosineSimilarity(a, b), 0.0001f)
    }

    @Test
    fun orthogonalEmbeddings_haveSimilarityOfZero() {
        val a = floatArrayOf(1f, 0f)
        val b = floatArrayOf(0f, 1f)
        assertEquals(0.0f, FaceMatcher.cosineSimilarity(a, b), 0.0001f)
    }

    @Test
    fun zeroVector_returnsZeroRatherThanDividingByZero() {
        val zero = floatArrayOf(0f, 0f, 0f)
        val other = floatArrayOf(1f, 2f, 3f)
        assertEquals(0.0f, FaceMatcher.cosineSimilarity(zero, other), 0.0001f)
    }

    @Test
    fun similarityAboveThreshold_isMatch() {
        assertTrue(FaceMatcher.isMatch(0.85f, threshold = 0.70f))
    }

    @Test
    fun similarityBelowThreshold_isNotMatch() {
        assertFalse(FaceMatcher.isMatch(0.5f, threshold = 0.70f))
    }

    @Test
    fun similarityExactlyAtThreshold_isMatch() {
        assertTrue(FaceMatcher.isMatch(0.70f, threshold = 0.70f))
    }

    @Test(expected = IllegalArgumentException::class)
    fun mismatchedEmbeddingSizes_throws() {
        FaceMatcher.cosineSimilarity(floatArrayOf(1f, 2f), floatArrayOf(1f))
    }
}
