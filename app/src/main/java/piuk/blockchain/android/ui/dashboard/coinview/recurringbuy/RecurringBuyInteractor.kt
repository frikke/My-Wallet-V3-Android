package piuk.blockchain.android.ui.dashboard.coinview.recurringbuy

import com.blockchain.core.payments.PaymentsDataManager
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.FundsAccount
import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.nabu.models.data.RecurringBuyPaymentDetails
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.domain.repositories.TradeDataManager

class RecurringBuyInteractor(
    private val tradeDataManager: TradeDataManager,
    private val paymentsDataManager: PaymentsDataManager
) {

    fun loadPaymentDetails(
        paymentMethodType: PaymentMethodType,
        paymentMethodId: String,
        originCurrency: String
    ): Single<RecurringBuyPaymentDetails> {
        return when (paymentMethodType) {
            PaymentMethodType.PAYMENT_CARD -> paymentsDataManager.getCardDetails(paymentMethodId)
                .map { it }
            PaymentMethodType.BANK_TRANSFER -> paymentsDataManager.getLinkedBank(paymentMethodId)
                .map { it.toPaymentMethod() }
            PaymentMethodType.FUNDS -> Single.just(FundsAccount(currency = originCurrency))

            else -> Single.just(object : RecurringBuyPaymentDetails {
                override val paymentDetails: PaymentMethodType
                    get() = paymentMethodType
            })
        }
    }

    fun deleteRecurringBuy(id: String) = tradeDataManager.cancelRecurringBuy(id)

    fun getRecurringBuyById(recurringBuyId: String): Single<RecurringBuy> =
        tradeDataManager.getRecurringBuyForId(recurringBuyId)
}
