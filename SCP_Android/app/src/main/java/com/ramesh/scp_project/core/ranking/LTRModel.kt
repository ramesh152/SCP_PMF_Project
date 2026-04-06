package com.ramesh.scp_project.core.ranking

data class LTRFeatures(
    val embeddingScore: Float,
    val keywordScore: Float,
    val recencyScore: Float,
    val sourceBoost: Float
)

class LTRModel(
    var embeddingWeight: Float = 0.5f,
    var keywordWeight: Float = 0.3f,
    var recencyWeight: Float = 0.1f,
    var sourceWeight: Float = 0.1f
) {

    fun score(features: LTRFeatures): Float {
        return (embeddingWeight * features.embeddingScore) +
                (keywordWeight * features.keywordScore) +
                (recencyWeight * features.recencyScore) +
                (sourceWeight * features.sourceBoost)
    }

    fun update(
        features: LTRFeatures,
        targetScore: Float,
        learningRate: Float
    ) {
        val prediction = score(features)
        val error = prediction - targetScore

        embeddingWeight -= learningRate * error * features.embeddingScore
        keywordWeight -= learningRate * error * features.keywordScore
        recencyWeight -= learningRate * error * features.recencyScore
        sourceWeight -= learningRate * error * features.sourceBoost
    }
}
