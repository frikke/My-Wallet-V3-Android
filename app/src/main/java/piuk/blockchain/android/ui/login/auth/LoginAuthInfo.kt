package piuk.blockchain.android.ui.login.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed class LoginAuthInfo {
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
        @SerialName("user_type")
        val userType: String = ""
    ) : LoginAuthInfo()
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
    val mobileDeviceType: Int,
    @SerialName("last_mnemonic_backup")
    val lastMnemonicBackup: Long = 0L,
    @SerialName("has_cloud_backup")
    val hasCloudBackup: Boolean = false,
    @SerialName("two_fa_type")
    val twoFaType: Int? = null,
    @SerialName("nabu")
    val nabuAccountInfo: NabuAccountInfo = NabuAccountInfo(),
    @SerialName("exchange")
    val accountExchange: AccountExchangeInfo = AccountExchangeInfo()
)

@Serializable
data class NabuAccountInfo(
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("recovery_token")
    val recoveryToken: String = "",
    @SerialName("recovery_eligible")
    val isRecoveryEligible: String = ""
)

@Serializable
data class AccountExchangeInfo(
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("two_fa_mode")
    val twoFaMode: Boolean = false
)