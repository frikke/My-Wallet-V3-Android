package piuk.blockchain.android.ui.dashboard.coinview.recurringbuy

import com.blockchain.core.recurringbuy.domain.RecurringBuyService
import com.blockchain.core.recurringbuy.domain.model.FundsAccount
import com.blockchain.core.recurringbuy.domain.model.RecurringBuy
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.CardService
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.paymentmethods.model.RecurringBuyPaymentDetails
import com.blockchain.store.asSingle
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.rxCompletable

class RecurringBuyInteractor(
    private val recurringBuyService: RecurringBuyService,
    private val bankService: BankService,
    private val cardService: CardService
) {

    fun loadPaymentDetails(
        paymentMethodType: PaymentMethodType,
        paymentMethodId: String,
        originCurrency: String
    ): Single<RecurringBuyPaymentDetails> {
        return when (paymentMethodType) {
            PaymentMethodType.PAYMENT_CARD -> cardService.getCardDetailsLegacy(paymentMethodId)
                .map { it }
            PaymentMethodType.BANK_TRANSFER -> bankService.getLinkedBankLegacy(paymentMethodId)
                .map { it.toPaymentMethod() }
            PaymentMethodType.FUNDS -> Single.just(FundsAccount(currency = originCurrency))

            else -> Single.just(object : RecurringBuyPaymentDetails {
                override val paymentDetails: PaymentMethodType
                    get() = paymentMethodType
            })
        }
    }

    fun deleteRecurringBuy(recurringBuy: RecurringBuy) = rxCompletable {
        recurringBuyService.cancelRecurringBuy(recurringBuy)
    }

    fun getRecurringBuyById(recurringBuyId: String): Single<RecurringBuy> =
        recurringBuyService.recurringBuy(recurringBuyId).asSingle()
}
