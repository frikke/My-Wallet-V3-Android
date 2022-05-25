package piuk.blockchain.android.ui.dashboard.coinview.recurringbuy

import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.CardService
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.paymentmethods.model.RecurringBuyPaymentDetails
import com.blockchain.nabu.models.data.FundsAccount
import com.blockchain.nabu.models.data.RecurringBuy
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.domain.repositories.TradeDataService

class RecurringBuyInteractor(
    private val tradeDataService: TradeDataService,
    private val bankService: BankService,
    private val cardService: CardService
) {

    fun loadPaymentDetails(
        paymentMethodType: PaymentMethodType,
        paymentMethodId: String,
        originCurrency: String
    ): Single<RecurringBuyPaymentDetails> {
        return when (paymentMethodType) {
            PaymentMethodType.PAYMENT_CARD -> cardService.getCardDetails(paymentMethodId)
                .map { it }
            PaymentMethodType.BANK_TRANSFER -> bankService.getLinkedBank(paymentMethodId)
                .map { it.toPaymentMethod() }
            PaymentMethodType.FUNDS -> Single.just(FundsAccount(currency = originCurrency))

            else -> Single.just(object : RecurringBuyPaymentDetails {
                override val paymentDetails: PaymentMethodType
                    get() = paymentMethodType
            })
        }
    }

    fun deleteRecurringBuy(id: String) = tradeDataService.cancelRecurringBuy(id)

    fun getRecurringBuyById(recurringBuyId: String): Single<RecurringBuy> =
        tradeDataService.getRecurringBuyForId(recurringBuyId)
}
