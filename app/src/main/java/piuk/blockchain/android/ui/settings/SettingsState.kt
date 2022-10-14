package piuk.blockchain.android.ui.settings

import com.blockchain.api.NabuApiException
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.nabu.BasicProfileInfo
import piuk.blockchain.android.domain.usecases.AvailablePaymentMethodType

data class SettingsState(
    val basicProfileInfo: BasicProfileInfo? = null,
    val hasWalletUnpaired: Boolean = false,
    val viewToLaunch: ViewToLaunch = ViewToLaunch.None,
    val paymentMethodInfo: PaymentMethods? = null,
    val tier: KycTier = KycTier.BRONZE,
    val error: SettingsError = SettingsError.None,
    val referralInfo: ReferralInfo = ReferralInfo.NotAvailable,
    val canPayWithBind: Boolean = false,
    val featureFlagsSet: FeatureFlagsSet = FeatureFlagsSet()
) : MviState

sealed class ViewToLaunch {
    object None : ViewToLaunch()
    object Profile : ViewToLaunch()
    class BankTransfer(val linkBankTransfer: LinkBankTransfer) : ViewToLaunch()
}

sealed class SettingsError {
    object None : SettingsError()
    object PaymentMethodsLoadFail : SettingsError()
    object BankLinkStartFail : SettingsError()
    data class BankLinkMaxAccountsReached(val error: NabuApiException) : SettingsError()
    data class BankLinkMaxAttemptsReached(val error: NabuApiException) : SettingsError()
    object UnpairFailed : SettingsError()
}

data class PaymentMethods(
    val availablePaymentMethodTypes: List<AvailablePaymentMethodType>,
    val linkedCards: List<PaymentMethod.Card>,
    val linkedBanks: List<BankItem>
)

data class UserDetails(
    val kycTier: KycTier,
    val userInfo: BasicProfileInfo,
    val referralInfo: ReferralInfo
)

@kotlinx.serialization.Serializable
data class FeatureFlagsSet(
    val cardRejectionFF: Boolean = false,
    val dustBalancesFF: Boolean = false
)
