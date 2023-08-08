package com.blockchain.nabu.datamanagers

import com.blockchain.analytics.UserAnalytics
import com.blockchain.analytics.UserProperty
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.preferences.SessionPrefs

interface NabuUserReporter {
    fun reportUserId(userId: String)
    fun reportUser(nabuUser: NabuUser)
}

class AnalyticsNabuUserReporterImpl(
    private val userAnalytics: UserAnalytics
) : NabuUserReporter {
    override fun reportUserId(userId: String) {
        userAnalytics.logUserId(userId)
        userAnalytics.logUserProperty(UserProperty(UserAnalytics.NABU_USER_ID, userId))
        userAnalytics.logUserProperty(UserProperty("sometestfield", "a1"))
    }

    override fun reportUser(nabuUser: NabuUser) {
        userAnalytics.logUserProperty(
            UserProperty(
                UserAnalytics.KYC_LEVEL,
                nabuUser.tierInProgressOrCurrentTier.toString()
            )
        )
        nabuUser.updatedAt?.let {
            userAnalytics.logUserProperty(UserProperty(UserAnalytics.KYC_UPDATED_DATE, it))
        }
        nabuUser.insertedAt?.let {
            userAnalytics.logUserProperty(UserProperty(UserAnalytics.KYC_CREATION_DATE, it))
        }

        userAnalytics.logUserProperty(UserProperty(UserAnalytics.COWBOYS_USER, nabuUser.isCowboysUser.toString()))
    }
}

class UniqueAnalyticsNabuUserReporter(
    private val nabuUserReporter: NabuUserReporter,
    private val prefs: SessionPrefs
) : NabuUserReporter by nabuUserReporter {
    override fun reportUserId(userId: String) {
        val reportedKey = prefs.analyticsReportedNabuUser
        if (reportedKey != userId) {
            nabuUserReporter.reportUserId(userId)
            prefs.analyticsReportedNabuUser = userId
        }
    }
}
