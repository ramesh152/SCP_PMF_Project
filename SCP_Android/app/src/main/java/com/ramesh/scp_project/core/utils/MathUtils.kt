package com.ramesh.scp_project.core.utils

import kotlin.math.sqrt

object MathUtils {

    fun cosineSimilarity(vectorA: FloatArray, vectorB: FloatArray): Float {
        require(vectorA.size == vectorB.size) {
            "Vectors must have the same size."
        }

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (index in vectorA.indices) {
            val a = vectorA[index]
            val b = vectorB[index]

            dotProduct += a * b
            normA += a * a
            normB += b * b
        }

        if (normA == 0f || normB == 0f) {
            return 0f
        }

        return (dotProduct / (sqrt(normA.toDouble()) * sqrt(normB.toDouble()))).toFloat()
    }
}