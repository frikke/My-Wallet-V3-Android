package piuk.blockchain.android.ui.settings.v2

import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.nabu.Tier
import com.blockchain.nabu.datamanagers.Bank
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.LinkBankTransfer
import info.blockchain.balance.FiatCurrency
import piuk.blockchain.android.ui.base.mvi.MviState

data class SettingsState(
    val basicProfileInfo: BasicProfileInfo? = null,
    val hasWalletUnpaired: Boolean = false,
    val viewToLaunch: ViewToLaunch = ViewToLaunch.None,
    val paymentMethodInfo: PaymentMethods? = null,
    val tier: Tier = Tier.BRONZE,
    val error: SettingsError = SettingsError.NONE
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
    val eligiblePaymentMethods: Map<PaymentMethodType, Boolean>,
    val linkedCards: List<PaymentMethod.Card>,
    val linkedBanks: Set<Bank>
) {
    fun canLinkPaymentMethods(): Boolean =
        eligiblePaymentMethods.values.any()
}

data class UserDetails(
    val userTier: Tier,
    val userInfo: BasicProfileInfo
)
