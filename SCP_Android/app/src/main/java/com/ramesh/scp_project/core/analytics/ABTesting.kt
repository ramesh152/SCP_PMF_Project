package com.ramesh.scp_project.core.analytics

import android.content.Context
import kotlin.math.absoluteValue

//Test
enum class RankingVariant {
    A_BASELINE,
    B_LTR
}

class ABTesting(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun getAssignment(userId: String): RankingVariant {
        val persisted = preferences.getString(KEY_ASSIGNMENT, null)
        if (persisted != null) {
            return RankingVariant.valueOf(persisted)
        }

        val assignedVariant = assignVariant(userId)
        preferences.edit()
            .putString(KEY_ASSIGNMENT, assignedVariant.name)
            .apply()

        return assignedVariant
    }

    private fun assignVariant(userId: String): RankingVariant {
        val bucket = userId.hashCode().absoluteValue % 2
        return if (bucket == 0) RankingVariant.A_BASELINE else RankingVariant.B_LTR
    }

    companion object {
        private const val PREFERENCES_NAME = "ab_testing"
        private const val KEY_ASSIGNMENT = "ranking_variant"
    }
}
