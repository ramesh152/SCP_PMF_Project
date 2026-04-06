package com.ramesh.scp_project.core.evaluation

import com.ramesh.scp_project.core.ranking.RankedMedia
import kotlin.math.ln

data class EvaluationQuery(
    val query: String,
    val relevanceById: Map<String, Int>
)

data class QueryEvaluationResult(
    val query: String,
    val precisionAtK: Float,
    val ndcgAtK: Float,
    val rankedIds: List<String>
)

data class EvaluationSummary(
    val k: Int,
    val queryCount: Int,
    val averagePrecisionAtK: Float,
    val averageNdcgAtK: Float,
    val perQueryResults: List<QueryEvaluationResult>
)

class Evaluator {

    fun precisionAtK(
        rankedIds: List<String>,
        relevantIds: Set<String>,
        k: Int
    ): Float {
        if (k <= 0 || rankedIds.isEmpty()) return 0f

        val topK = rankedIds.take(k)
        val relevantCount = topK.count { it in relevantIds }
        return relevantCount.toFloat() / k.toFloat()
    }

    fun ndcgAtK(
        rankedIds: List<String>,
        relevanceById: Map<String, Int>,
        k: Int
    ): Float {
        if (k <= 0 || rankedIds.isEmpty() || relevanceById.isEmpty()) return 0f

        val dcg = rankedIds
            .take(k)
            .mapIndexed { index, id ->
                discountedGain(
                    relevance = relevanceById[id] ?: 0,
                    rank = index + 1
                )
            }
            .sum()

        val idealDcg = relevanceById.values
            .sortedDescending()
            .take(k)
            .mapIndexed { index, relevance ->
                discountedGain(
                    relevance = relevance,
                    rank = index + 1
                )
            }
            .sum()

        if (idealDcg == 0.0) return 0f
        return (dcg / idealDcg).toFloat()
    }

    suspend fun evaluate(
        queries: List<EvaluationQuery>,
        k: Int,
        ranker: suspend (String) -> List<RankedMedia>
    ): EvaluationSummary {
        if (queries.isEmpty()) {
            return EvaluationSummary(
                k = k,
                queryCount = 0,
                averagePrecisionAtK = 0f,
                averageNdcgAtK = 0f,
                perQueryResults = emptyList()
            )
        }

        val perQueryResults = queries.map { evaluationQuery ->
            val rankedIds = ranker(evaluationQuery.query)
                .map { rankedMedia -> rankedMedia.media.id }

            QueryEvaluationResult(
                query = evaluationQuery.query,
                precisionAtK = precisionAtK(
                    rankedIds = rankedIds,
                    relevantIds = evaluationQuery.relevanceById.keys,
                    k = k
                ),
                ndcgAtK = ndcgAtK(
                    rankedIds = rankedIds,
                    relevanceById = evaluationQuery.relevanceById,
                    k = k
                ),
                rankedIds = rankedIds.take(k)
            )
        }

        val averagePrecision = perQueryResults
            .map(QueryEvaluationResult::precisionAtK)
            .average()
            .toFloat()

        val averageNdcg = perQueryResults
            .map(QueryEvaluationResult::ndcgAtK)
            .average()
            .toFloat()

        return EvaluationSummary(
            k = k,
            queryCount = perQueryResults.size,
            averagePrecisionAtK = averagePrecision,
            averageNdcgAtK = averageNdcg,
            perQueryResults = perQueryResults
        )
    }

    private fun discountedGain(relevance: Int, rank: Int): Double {
        if (relevance <= 0) return 0.0
        return ((1 shl relevance) - 1).toDouble() / log2(rank + 1.0)
    }

    private fun log2(value: Double): Double {
        return ln(value) / ln(2.0)
    }
}
