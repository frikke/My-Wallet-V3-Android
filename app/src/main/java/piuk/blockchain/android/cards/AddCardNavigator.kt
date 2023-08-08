package piuk.blockchain.android.cards

import com.blockchain.domain.paymentmethods.model.PaymentMethod

interface AddCardNavigator {
    fun restartFlow()
    fun navigateToCardDetails()
    fun navigateToBillingDetails()
    fun navigateToCardVerification()
    fun exitWithSuccess(card: PaymentMethod.Card)
    fun exitWithError()
}
