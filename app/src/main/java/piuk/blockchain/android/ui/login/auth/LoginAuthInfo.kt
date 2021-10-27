package piuk.blockchain.android.ui.login.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed class LoginAuthInfo : java.io.Serializable {
    @Serializable
    data class PollingDeniedAccountInfo(
        @SerialName("request_denied")
        val denied: Boolean
    )

    @Serializable
    data class SimpleAccountInfo(
        @SerialName("guid")
        val guid: String,
        @SerialName("email")
        val email: String,
        @SerialName("email_code")
        val authToken: String
    ) : LoginAuthInfo()

    @Serializable
    data class ExtendedAccountInfo(
        @SerialName("wallet")
        val accountWallet: AccountWalletInfo,
        @SerialName("upgradeable")
        val isUpgradeable: Boolean = false,
        @SerialName("mergeable")
        val isMergeable: Boolean = false,
        @SerialName("unified")
        val isUnified: Boolean = false,
        @SerialName("user_type")
        val userType: String = ""
    ) : LoginAuthInfo() {

        fun mapUserType(userType: String): BlockchainAccountType =
            when (userType) {
                WALLET -> BlockchainAccountType.WALLET
                EXCHANGE -> BlockchainAccountType.EXCHANGE
                WALLET_EXCHANGE_LINKED -> BlockchainAccountType.WALLET_EXCHANGE_LINKED
                WALLET_EXCHANGE_NOT_LINKED -> BlockchainAccountType.WALLET_EXCHANGE_NOT_LINKED
                WALLET_EXCHANGE_BOTH -> BlockchainAccountType.WALLET_EXCHANGE_BOTH
                else -> BlockchainAccountType.UNKNOWN
            }

        companion object {
            private const val WALLET = "WALLET"
            private const val EXCHANGE = "EXCHANGE"
            private const val WALLET_EXCHANGE_LINKED = "WALLET_EXCHANGE_LINKED"
            private const val WALLET_EXCHANGE_NOT_LINKED = "WALLET_EXCHANGE_NOT_LINKED"
            private const val WALLET_EXCHANGE_BOTH = "WALLET_EXCHANGE_BOTH"
        }
    }
}

@Serializable
data class AccountWalletInfo(
    @SerialName("guid")
    val guid: String,
    @SerialName("email")
    val email: String,
    @SerialName("email_code")
    val authToken: String,
    @SerialName("is_mobile_setup")
    val isMobileSetUp: Boolean,
    @SerialName("mobile_device_type")
    val mobileDeviceType: Int = 2, // BE doesn't always return this, so default: 2 is Android - 1 is iOS
    @SerialName("last_mnemonic_backup")
    val lastMnemonicBackup: Long = 0L,
    @SerialName("has_cloud_backup")
    val hasCloudBackup: Boolean = false,
    @SerialName("two_fa_type")
    val twoFaType: Int? = null,
    @SerialName("nabu")
    val nabuAccountInfo: NabuAccountInfo = NabuAccountInfo(),
    @SerialName("exchange")
    val accountExchange: AccountExchangeInfo = AccountExchangeInfo(),
    @SerialName("session_id")
    val sessionId: String
) : java.io.Serializable

@Serializable
data class NabuAccountInfo(
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("recovery_token")
    val recoveryToken: String = "",
    @SerialName("recovery_eligible")
    val isRecoveryEligible: String = ""
) : java.io.Serializable

@Serializable
data class AccountExchangeInfo(
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("two_fa_mode")
    val twoFaMode: Boolean = false
) : java.io.Serializable