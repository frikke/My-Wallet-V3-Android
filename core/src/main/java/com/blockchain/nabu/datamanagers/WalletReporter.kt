package com.blockchain.nabu.datamanagers

import com.blockchain.analytics.UserAnalytics
import com.blockchain.analytics.UserProperty
import com.blockchain.preferences.SessionPrefs
import info.blockchain.wallet.api.data.Settings
import java.security.MessageDigest
import org.spongycastle.util.encoders.Hex

interface WalletReporter {
    fun reportWalletGuid(walletGuid: String)
    fun reportUserSettings(settings: Settings)
}

class AnalyticsWalletReporter(private val userAnalytics: UserAnalytics) : WalletReporter {
    override fun reportWalletGuid(walletGuid: String) {
        val walletId = String(
            Hex.encode(
                MessageDigest.getInstance("SHA-256")
                    .digest(
                        walletGuid.toByteArray(charset("UTF-8"))
                    )
            )
        ).take(UserProperty.MAX_VALUE_LEN)
        userAnalytics.logUserProperty(UserProperty(UserAnalytics.WALLET_ID, walletId))
    }

    override fun reportUserSettings(settings: Settings) {
        userAnalytics.logUserProperty(
            UserProperty(
                UserAnalytics.EMAIL_VERIFIED,
                settings.isEmailVerified.toString()
            )
        )

        userAnalytics.logUserProperty(
            UserProperty(
                UserAnalytics.TWOFA_ENABLED,
                (settings.authType != Settings.AUTH_TYPE_OFF).toString()
            )
        )
    }
}

class UniqueAnalyticsWalletReporter(
    private val walletReporter: WalletReporter,
    private val prefs: SessionPrefs
) : WalletReporter by walletReporter {
    override fun reportWalletGuid(walletGuid: String) {
        val reportedKey = prefs.analyticsReportedWalletKey.take(UserProperty.MAX_VALUE_LEN)
        if (reportedKey != walletGuid) {
            walletReporter.reportWalletGuid(walletGuid)
            prefs.analyticsReportedWalletKey = walletGuid
        }
    }

    override fun reportUserSettings(settings: Settings) {
        walletReporter.reportUserSettings(settings)
    }
}
