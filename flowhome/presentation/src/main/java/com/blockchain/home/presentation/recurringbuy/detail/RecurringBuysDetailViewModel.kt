package com.blockchain.home.presentation.recurringbuy.detail

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.core.recurringbuy.domain.RecurringBuyService
import com.blockchain.core.recurringbuy.domain.model.FundsAccount
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.map
import com.blockchain.data.updateDataWith
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.CardService
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.paymentmethods.model.RecurringBuyPaymentDetails
import com.blockchain.home.presentation.recurringbuy.list.toHumanReadableRecurringBuy
import com.blockchain.home.presentation.recurringbuy.list.toHumanReadableRecurringDate
import com.blockchain.store.flatMapData
import com.blockchain.store.mapData
import com.blockchain.utils.toFormattedDateWithoutYear
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class RecurringBuysDetailViewModel(
    private val recurringBuyId: String,
    private val recurringBuyService: RecurringBuyService,
    private val bankService: BankService,
    private val cardService: CardService
) : MviViewModel<
    RecurringBuysDetailIntent,
    RecurringBuyDetailViewState,
    RecurringBuysDetailModelState,
    RecurringBuysDetailNavEvent,
    ModelConfigArgs.NoArgs
    >(initialState = RecurringBuysDetailModelState()) {
    private var recurringBuyJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(
        state: RecurringBuysDetailModelState
    ): RecurringBuyDetailViewState = state.run {
        RecurringBuyDetailViewState(
            detail = recurringBuy.map { recurringBuy ->
                RecurringBuyDetail(
                    iconUrl = recurringBuy.asset.logo,
                    amount = recurringBuy.amount.toStringWithSymbol(),
                    assetName = recurringBuy.asset.name,
                    assetTicker = recurringBuy.asset.networkTicker,
                    paymentMethod = when (recurringBuy.paymentMethodType) {
                        PaymentMethodType.FUNDS -> {
                            recurringBuy.amount.currency.name
                        }
                        PaymentMethodType.PAYMENT_CARD -> {
                            (recurringBuy.paymentDetails as PaymentMethod.Card).let {
                                "${it.uiLabel()} ••••${it.endDigits}"
                            }
                        }
                        else -> {
                            (recurringBuy.paymentDetails as PaymentMethod.Bank).let {
                                "${it.bankName} ••••${it.accountEnding}"
                            }
                        }
                    },
                    frequency = TextValue.Combined(
                        listOf(
                            TextValue.IntResValue(
                                recurringBuy.recurringBuyFrequency.toHumanReadableRecurringBuy()
                            ),
                            recurringBuy.recurringBuyFrequency.toHumanReadableRecurringDate(
                                ZonedDateTime.ofInstant(
                                    recurringBuy.nextPaymentDate.toInstant(),
                                    ZoneId.systemDefault()
                                )
                            )
                        )
                    ),
                    nextBuy = recurringBuy.nextPaymentDate.toFormattedDateWithoutYear()
                )
            },
            cancelationInProgress = cancelationInProgress
        )
    }

    override suspend fun handleIntent(
        modelState: RecurringBuysDetailModelState,
        intent: RecurringBuysDetailIntent
    ) {
        when (intent) {
            is RecurringBuysDetailIntent.LoadRecurringBuy -> {
                loadRecurringBuy(includeInactive = intent.includeInactive)
            }

            RecurringBuysDetailIntent.CancelRecurringBuy -> {
                check(modelState.recurringBuy is DataResource.Data)

                updateState {
                    it.copy(cancelationInProgress = true)
                }

                recurringBuyService.cancelRecurringBuy(
                    modelState.recurringBuy.data
                )

                navigate(RecurringBuysDetailNavEvent.Close)
            }
        }
    }

    private fun loadRecurringBuy(
        includeInactive: Boolean = false
    ) {
        recurringBuyJob?.cancel()
        recurringBuyJob = viewModelScope.launch {
            recurringBuyService.recurringBuy(
                id = recurringBuyId,
                includeInactive = includeInactive,
                FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
            )
                .flatMapData { recurringBuy ->
                    loadPaymentDetails(
                        paymentMethodType = recurringBuy.paymentMethodType,
                        paymentMethodId = recurringBuy.paymentMethodId.orEmpty(),
                        originCurrency = recurringBuy.amount.currencyCode
                    ).mapData {
                        recurringBuy.copy(paymentDetails = it)
                    }
                }
                .collectLatest { recurringBuy ->
                    updateState {
                        it.copy(
                            recurringBuy = it.recurringBuy.updateDataWith(recurringBuy)
                        )
                    }
                }
        }
    }

    private fun loadPaymentDetails(
        paymentMethodType: PaymentMethodType,
        paymentMethodId: String,
        originCurrency: String
    ): Flow<DataResource<RecurringBuyPaymentDetails>> {
        return when (paymentMethodType) {
            PaymentMethodType.PAYMENT_CARD -> cardService.getCardDetails(
                paymentMethodId,
                FreshnessStrategy.Cached(
                    RefreshStrategy.RefreshIfStale
                )
            )

            PaymentMethodType.BANK_TRANSFER -> bankService.getLinkedBank(paymentMethodId)
                .mapData { it.toPaymentMethod() }

            PaymentMethodType.FUNDS -> flowOf(DataResource.Data(FundsAccount(currency = originCurrency)))

            else -> flowOf(
                DataResource.Data(object : RecurringBuyPaymentDetails {
                    override val paymentDetails: PaymentMethodType
                        get() = paymentMethodType
                })
            )
        }
    }
}
