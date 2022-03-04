package piuk.blockchain.android.ui.recover

import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames
import java.io.Serializable

sealed class AccountRecoveryAnalytics(
    override val event: String,
    override val params: Map<String, Serializable> = emptyMap()
) : AnalyticsEvent {

    class PasswordReset(isCustodialAccount: Boolean) : AccountRecoveryAnalytics(
        event = AnalyticsNames.RECOVERY_PASSWORD_RESET.eventName,
        params = createMap(isCustodialAccount).toMutableMap().apply {
            // This is ALWAYS true, as users can only reset their password after providing the recovery phrase
            put(WITH_RECOVERY_PHRASE, true)
        }
    )

    class RecoveryFailed(isCustodialAccount: Boolean) : AccountRecoveryAnalytics(
        event = AnalyticsNames.RECOVERY_FAILED.eventName,
        params = createMap(isCustodialAccount)
    )

    // does not currently trigger
    class CloudBackupScanned(isCustodialAccount: Boolean) : AccountRecoveryAnalytics(
        event = AnalyticsNames.RECOVERY_CLOUD_BACKUP_SCANNED.eventName,
        params = createMap(isCustodialAccount)
    )

    class NewPasswordSet(isCustodialAccount: Boolean) : AccountRecoveryAnalytics(
        event = AnalyticsNames.RECOVERY_NEW_PASSWORD.eventName,
        params = createMap(isCustodialAccount)
    )

    class RecoveryOptionSelected(isCustodialAccount: Boolean) : AccountRecoveryAnalytics(
        event = AnalyticsNames.RECOVERY_OPTION_SELECTED.eventName,
        params = createMap(isCustodialAccount)
    )

    class MnemonicEntered(isCustodialAccount: Boolean) : AccountRecoveryAnalytics(
        event = AnalyticsNames.RECOVERY_MNEMONIC_ENTERED.eventName,
        params = createMap(isCustodialAccount)
    )

    class ResetCancelled(isCustodialAccount: Boolean) : AccountRecoveryAnalytics(
        event = AnalyticsNames.RECOVERY_RESET_CANCELLED.eventName,
        params = createMap(isCustodialAccount)
    )

    class ResetClicked(isCustodialAccount: Boolean) : AccountRecoveryAnalytics(
        event = AnalyticsNames.RECOVERY_RESET_CLICKED.eventName,
        params = createMap(isCustodialAccount)
    )

    companion object {
        private const val PLATFORM_WALLET = "WALLET"
        private const val ACCOUNT_TYPE = "account_type"
        private const val SITE_REDIRECT = "site_redirect"
        private const val ACCOUNT_TYPE_PRIVATE_KEY = "USERKEY"
        private const val ACCOUNT_TYPE_CUSTODIAL = "CUSTODIAL"
        private const val WITH_RECOVERY_PHRASE = "with_recovery_phrase"

        private fun createMap(isCustodialAccount: Boolean): Map<String, Serializable> =
            mapOf(
                SITE_REDIRECT to PLATFORM_WALLET,
                ACCOUNT_TYPE to if (isCustodialAccount) ACCOUNT_TYPE_CUSTODIAL else ACCOUNT_TYPE_PRIVATE_KEY
            )
    }
}
