package piuk.blockchain.android.ui.login

import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames
import com.blockchain.notifications.analytics.LaunchOrigin
import piuk.blockchain.android.ui.login.auth.LoginAuthInfo
import java.io.Serializable

sealed class LoginAnalytics(
    override val event: String,
    override val params: Map<String, Serializable> = emptyMap()
) : AnalyticsEvent {

    class DeviceVerified(authInfo: LoginAuthInfo?) : LoginAnalytics(
        event = AnalyticsNames.LOGIN_DEVICE_VERIFIED.eventName,
        params = authInfo.constructDataMap()
    )

    class LoginClicked(
        override val origin: LaunchOrigin = LaunchOrigin.LAUNCH_SCREEN
    ) : LoginAnalytics(
        event = AnalyticsNames.LOGIN_CTA_CLICKED.eventName
    )

    class LoginHelpClicked(authInfo: LoginAuthInfo?) : LoginAnalytics(
        event = AnalyticsNames.LOGIN_HELP_CLICKED.eventName,
        params = authInfo.constructDataMap()
    )

    object LoginIdentifierEntered : LoginAnalytics(
        event = AnalyticsNames.LOGIN_ID_ENTERED.eventName
    )

    class LoginLearnMoreClicked(authInfo: LoginAuthInfo?) : LoginAnalytics(
        event = AnalyticsNames.LOGIN_LEARN_MORE_CLICKED.eventName,
        params = authInfo.constructDataMap()
    )

    object LoginWithGoogleMethodSelected : LoginAnalytics(
        event = AnalyticsNames.LOGIN_METHOD_SELECTED.eventName,
        params = mapOf(
            LOGIN_METHOD to LOGIN_WITH_GOOGLE
        )
    )

    class LoginPasswordDenied(authInfo: LoginAuthInfo?) : LoginAnalytics(
        event = AnalyticsNames.LOGIN_PASSWORD_DENIED.eventName,
        params = authInfo.constructDataMap()
    )

    class LoginPasswordEntered(authInfo: LoginAuthInfo?) : LoginAnalytics(
        event = AnalyticsNames.LOGIN_PASSWORD_ENTERED.eventName,
        params = authInfo.constructDataMap()
    )

    class LoginRequestApproved(authInfo: LoginAuthInfo?) : LoginAnalytics(
        event = AnalyticsNames.LOGIN_REQUEST_APPROVED.eventName,
        params = authInfo.constructDataMap()
    )

    class LoginRequestDenied(authInfo: LoginAuthInfo?) : LoginAnalytics(
        event = AnalyticsNames.LOGIN_REQUEST_DENIED.eventName,
        params = authInfo.constructDataMap()
    )

    class LoginTwoFaDenied(authInfo: LoginAuthInfo?) : LoginAnalytics(
        event = AnalyticsNames.LOGIN_2FA_DENIED.eventName,
        params = authInfo.constructDataMap()
    )

    class LoginTwoFaEntered(authInfo: LoginAuthInfo?) : LoginAnalytics(
        event = AnalyticsNames.LOGIN_2FA_ENTERED.eventName,
        params = authInfo.constructDataMap()
    )

    object LoginViewed : LoginAnalytics(
        event = AnalyticsNames.LOGIN_VIEWED.eventName
    )

    companion object {
        private const val LOGIN_WITH_GOOGLE = "Google"
        private const val PLATFORM_WALLET = "WALLET"
        private const val EXCHANGE = "exchange"
        private const val TWO_FA_MODE = "two_fa_mode"
        private const val USER_ID = "user_id"
        private const val MERGEABLE = "mergeable"
        private const val SITE_REDIRECT = "site_redirect"
        private const val UPGRADEABLE = "upgradeable"
        private const val WALLET = "wallet"
        private const val AUTH_TYPE = "auth_type"
        private const val GUID_FIRST_FOUR = "guid_first_four"
        private const val HAS_CLOUD_BACKUP = "has_cloud_backup"
        private const val IS_MOBILE_SETUP = "is_mobile_setup"
        private const val LAST_MNEMONIC_BACKUP = "last_mnemonic_backup"
        private const val MOBILE_DEVICE_TYPE = "mobile_device_type"
        private const val NABU = "nabu"
        private const val RECOVERY_ELIGIBLE = "recovery_eligible"
        private const val LOGIN_METHOD = "method"

        private fun LoginAuthInfo?.constructDataMap(): Map<String, Serializable> =
            when (this) {
                is LoginAuthInfo.SimpleAccountInfo ->
                    // we don't have enough data to send required params here
                    emptyMap()
                is LoginAuthInfo.ExtendedAccountInfo ->
                    mapOf(
                        MERGEABLE to isMergeable,
                        SITE_REDIRECT to PLATFORM_WALLET,
                        UPGRADEABLE to isUpgradeable,
                        WALLET to NestedData(
                            mapOf(
                                AUTH_TYPE to (accountWallet.twoFaType ?: ""),
                                GUID_FIRST_FOUR to accountWallet.guid.take(4),
                                HAS_CLOUD_BACKUP to accountWallet.hasCloudBackup,
                                IS_MOBILE_SETUP to accountWallet.isMobileSetUp,
                                LAST_MNEMONIC_BACKUP to accountWallet.lastMnemonicBackup,
                                MOBILE_DEVICE_TYPE to accountWallet.mobileDeviceType
                            )
                        ),
                        EXCHANGE to NestedData(
                            mapOf(
                                TWO_FA_MODE to accountWallet.accountExchange.twoFaMode,
                                USER_ID to accountWallet.accountExchange.userId
                            )
                        ),
                        NABU to NestedData(
                            mapOf(
                                RECOVERY_ELIGIBLE to accountWallet.nabuAccountInfo.isRecoveryEligible,
                                USER_ID to accountWallet.nabuAccountInfo.userId
                            )
                        )
                    )
                else -> emptyMap()
            }

        private data class NestedData(val map: Map<String, Serializable>) : Serializable
    }
}