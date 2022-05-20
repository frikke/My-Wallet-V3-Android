package piuk.blockchain.android.ui.settings.v2

import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.core.payments.model.LinkBankTransfer
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.nabu.Tier
import com.blockchain.nabu.datamanagers.PaymentMethod
import info.blockchain.balance.FiatCurrency
import piuk.blockchain.android.domain.usecases.AvailablePaymentMethodType

data class SettingsState(
    val basicProfileInfo: BasicProfileInfo? = null,
    val hasWalletUnpaired: Boolean = false,
    val viewToLaunch: ViewToLaunch = ViewToLaunch.None,
    val paymentMethodInfo: PaymentMethods? = null,
    val tier: Tier = Tier.BRONZE,
    val error: SettingsError = SettingsError.NONE,
    val referralInfo: ReferralInfo = ReferralInfo.NotAvailable
) : MviState

sealed class ViewToLaunch {
    object None : ViewToLaunch()
    object Profile : ViewToLaunch()
    class BankTransfer(val linkBankTransfer: LinkBankTransfer) : ViewToLaunch()
    class BankAccount(val currency: FiatCurrency) : ViewToLaunch()
}

enum class SettingsError {
    NONE,
    PAYMENT_METHODS_LOAD_FAIL,
    BANK_LINK_START_FAIL,
    UNPAIR_FAILED
}

data class PaymentMethods(
    val availablePaymentMethodTypes: List<AvailablePaymentMethodType>,
    val linkedCards: List<PaymentMethod.Card>,
    val linkedBanks: List<BankItem>
)

data class UserDetails(
    val userTier: Tier,
    val userInfo: BasicProfileInfo,
    val referralInfo: ReferralInfo
)
