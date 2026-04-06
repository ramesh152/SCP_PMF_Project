package com.ramesh.scp_project.core.analytics

data class SearchEvent(
    val query: String,
    val results: Int,
    val timestamp: Long,
    val variant: String
)

data class ClickEvent(
    val query: String,
    val clickedId: String,
    val position: Int,
    val timestamp: Long
)

class AnalyticsLogger {
    private val searchEvents = mutableListOf<SearchEvent>()
    private val clickEvents = mutableListOf<ClickEvent>()

    fun logSearch(event: SearchEvent) {
        searchEvents += event
    }

    fun logClick(event: ClickEvent) {
        clickEvents += event
    }

    fun getSearchEvents(): List<SearchEvent> = searchEvents.toList()

    fun getClickEvents(): List<ClickEvent> = clickEvents.toList()

    fun clear() {
        searchEvents.clear()
        clickEvents.clear()
    }
}
