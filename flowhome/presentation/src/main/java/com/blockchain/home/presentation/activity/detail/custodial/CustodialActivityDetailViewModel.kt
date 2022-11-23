package com.blockchain.home.presentation.activity.detail.custodial

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.CustodialTradingActivitySummaryItem
import com.blockchain.coincore.CustodialTransferActivitySummaryItem
import com.blockchain.coincore.FiatActivitySummaryItem
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.data.map
import com.blockchain.data.onErrorReturn
import com.blockchain.data.updateDataWith
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.CardService
import com.blockchain.domain.paymentmethods.PaymentMethodService
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.home.activity.CustodialActivityService
import com.blockchain.home.presentation.activity.detail.ActivityDetailIntent
import com.blockchain.home.presentation.activity.detail.ActivityDetailModelState
import com.blockchain.home.presentation.activity.detail.ActivityDetailViewState
import com.blockchain.home.presentation.activity.detail.custodial.mappers.buildActivityDetail
import com.blockchain.home.presentation.activity.detail.custodial.mappers.toActivityDetail
import com.blockchain.home.presentation.dashboard.HomeNavEvent
import com.blockchain.store.mapData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class CustodialActivityDetailViewModel(
    private val activityTxId: String,
    private val custodialActivityService: CustodialActivityService,
    private val paymentMethodService: PaymentMethodService,
    private val cardService: CardService,
    private val bankService: BankService
) : MviViewModel<
    ActivityDetailIntent<CustodialActivityDetail>,
    ActivityDetailViewState,
    ActivityDetailModelState<CustodialActivityDetail>,
    HomeNavEvent,
    ModelConfigArgs.NoArgs>(ActivityDetailModelState()) {

    private var activityDetailJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(
        state: ActivityDetailModelState<CustodialActivityDetail>
    ): ActivityDetailViewState = state.run {
        ActivityDetailViewState(
            activityDetail = activityDetail.map { it.toActivityDetail() }
        )
    }

    override suspend fun handleIntent(
        modelState: ActivityDetailModelState<CustodialActivityDetail>,
        intent: ActivityDetailIntent<CustodialActivityDetail>
    ) {
        when (intent) {
            is ActivityDetailIntent.LoadActivityDetail -> {
                loadActivityDetail()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadActivityDetail() {
        activityDetailJob?.cancel()
        activityDetailJob = viewModelScope.launch {
            custodialActivityService
                .getActivity(txId = activityTxId)
                .flatMapLatest { summaryDataResource ->
                    when (summaryDataResource) {
                        is DataResource.Data -> {
                            with(summaryDataResource.data) {
                                when (this) {
                                    is CustodialTradingActivitySummaryItem -> {
                                        tradingDetail()
                                    }
                                    is CustodialTransferActivitySummaryItem -> {
                                        transferDetail()
                                    }
                                    is FiatActivitySummaryItem -> {
                                        fiatDetail()
                                    }
                                    // todo rest of types
                                    else -> flowOf(DataResource.Loading)
                                }
                            }
                        }
                        is DataResource.Error -> flowOf(DataResource.Error(summaryDataResource.error))
                        DataResource.Loading -> flowOf(DataResource.Loading)
                    }
                }
                .onEach { dataResource ->
                    updateState {
                        it.copy(activityDetail = it.activityDetail.updateDataWith(dataResource))
                    }
                }
                .collect()
        }
    }

    /**
     * fetch more detail when it's PAYMENT_CARD and BANK_TRANSFER
     * otherwise default data
     */
    private fun CustodialTradingActivitySummaryItem.tradingDetail(): Flow<DataResource<CustodialActivityDetail>> {
        return when (paymentMethodType) {
            PaymentMethodType.PAYMENT_CARD -> {
                cardService.getCardDetails(cardId = paymentMethodId)
                    .mapData { card -> card.toPaymentDetail() }
            }
            PaymentMethodType.BANK_TRANSFER -> {
                bankService.getLinkedBank(id = paymentMethodId)
                    .mapData { bank -> bank.toPaymentMethod().toPaymentDetail() }
            }
            else -> {
                flowOf(
                    DataResource.Data(
                        PaymentDetails(
                            paymentMethodId = PaymentMethod.FUNDS_PAYMENT_ID,
                            label = fundedFiat.currencyCode
                        )
                    )
                )
            }
        }.catch {
            emit(
                DataResource.Data(
                    PaymentDetails(paymentMethodId = paymentMethodId, label = fundedFiat.currencyCode)
                )
            )
        }.onErrorReturn {
            PaymentDetails(paymentMethodId = paymentMethodId, label = fundedFiat.currencyCode)
        }.mapData { paymentDetails ->
            buildActivityDetail(paymentDetails)
        }
    }

    private fun CustodialTransferActivitySummaryItem.transferDetail(): Flow<DataResource<CustodialActivityDetail>> {
        return flowOf(DataResource.Data(buildActivityDetail()))
    }

    private fun FiatActivitySummaryItem.fiatDetail(): Flow<DataResource<CustodialActivityDetail>> {
        return paymentMethodService.getPaymentMethodDetailsForId(paymentMethodId.orEmpty())
            .mapData { paymentMethodDetails ->
                buildActivityDetail(paymentMethodDetails)
            }
    }
}

private fun PaymentMethod.label(): String? = when (this) {
    is PaymentMethod.Bank -> bankName
    is PaymentMethod.Card -> uiLabel()
    else -> null
}

private fun PaymentMethod.endDigits(): String? = when (this) {
    is PaymentMethod.Bank -> accountEnding
    is PaymentMethod.Card -> endDigits
    else -> null
}

private fun PaymentMethod.accountType(): String? = when (this) {
    is PaymentMethod.Bank -> uiAccountType
    else -> null
}

private fun PaymentMethod.toPaymentDetail(): PaymentDetails = PaymentDetails(
    paymentMethodId = id,
    label = label(),
    endDigits = endDigits(),
    accountType = accountType(),
    paymentMethodType = type,
    mobilePaymentType = (this as? PaymentMethod.Card)?.mobilePaymentType
)
