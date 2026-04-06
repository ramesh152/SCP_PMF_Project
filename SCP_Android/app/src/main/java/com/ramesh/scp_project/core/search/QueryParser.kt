package com.ramesh.scp_project.core.search

data class ParsedQuery(
    val text: String,
    val timeFilterDays: Int?,
    val merchant: String?
)

class QueryParser {

    fun parse(query: String): ParsedQuery {
        val normalizedQuery = query.trim()
        val lowercaseQuery = normalizedQuery.lowercase()

        val timeFilterDays = when {
            "today" in lowercaseQuery -> 1
            "last week" in lowercaseQuery -> 7
            "last month" in lowercaseQuery -> 30
            else -> null
        }

        val merchant = SUPPORTED_MERCHANTS.firstOrNull { merchantName ->
            merchantName in lowercaseQuery
        }

        val cleanedText = normalizedQuery
            .replace("today", "", ignoreCase = true)
            .replace("last week", "", ignoreCase = true)
            .replace("last month", "", ignoreCase = true)
            .replace(Regex("\\b(swiggy|amazon|zomato)\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        return ParsedQuery(
            text = cleanedText,
            timeFilterDays = timeFilterDays,
            merchant = merchant
        )
    }

    companion object {
        private val SUPPORTED_MERCHANTS = listOf(
            "swiggy",
            "amazon",
            "zomato"
        )
    }
}