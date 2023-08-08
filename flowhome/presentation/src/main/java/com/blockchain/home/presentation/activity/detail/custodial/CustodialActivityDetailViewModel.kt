package com.blockchain.home.presentation.activity.detail.custodial

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CustodialActiveRewardsActivitySummaryItem
import com.blockchain.coincore.CustodialInterestActivitySummaryItem
import com.blockchain.coincore.CustodialStakingActivitySummaryItem
import com.blockchain.coincore.CustodialTradingActivitySummaryItem
import com.blockchain.coincore.CustodialTransferActivitySummaryItem
import com.blockchain.coincore.FiatActivitySummaryItem
import com.blockchain.coincore.RecurringBuyActivitySummaryItem
import com.blockchain.coincore.TradeActivitySummaryItem
import com.blockchain.coincore.selectFirstAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.recurringbuy.domain.RecurringBuyService
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.combineDataResources
import com.blockchain.data.filterNotLoading
import com.blockchain.data.map
import com.blockchain.data.mapData
import com.blockchain.data.onErrorReturn
import com.blockchain.data.updateDataWith
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.CardService
import com.blockchain.domain.paymentmethods.PaymentMethodService
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.home.activity.CustodialActivityService
import com.blockchain.home.presentation.activity.detail.ActivityDetailIntent
import com.blockchain.home.presentation.activity.detail.ActivityDetailModelState
import com.blockchain.home.presentation.activity.detail.ActivityDetailViewState
import com.blockchain.home.presentation.activity.detail.custodial.mappers.buildActivityDetail
import com.blockchain.home.presentation.activity.detail.custodial.mappers.buildSellActivityDetail
import com.blockchain.home.presentation.activity.detail.custodial.mappers.buildSwapActivityDetail
import com.blockchain.home.presentation.activity.detail.custodial.mappers.toActivityDetail
import com.blockchain.home.presentation.activity.list.custodial.mappers.isSellingPair
import com.blockchain.home.presentation.activity.list.custodial.mappers.isSwapPair
import com.blockchain.home.presentation.dashboard.HomeNavEvent
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.Money
import info.blockchain.balance.asAssetInfoOrThrow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await

class CustodialActivityDetailViewModel(
    private val activityTxId: String,
    private val custodialActivityService: CustodialActivityService,
    private val paymentMethodService: PaymentMethodService,
    private val cardService: CardService,
    private val bankService: BankService,
    private val coincore: Coincore,
    private val defaultLabels: DefaultLabels,
    private val recurringBuyService: RecurringBuyService
) : MviViewModel<
    ActivityDetailIntent<CustodialActivityDetail>,
    ActivityDetailViewState,
    ActivityDetailModelState<CustodialActivityDetail>,
    HomeNavEvent,
    ModelConfigArgs.NoArgs
    >(ActivityDetailModelState()) {

    private var activityDetailJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun ActivityDetailModelState<CustodialActivityDetail>.reduce() = ActivityDetailViewState(
        activityDetail = activityDetail.map { it.toActivityDetail() }
    )

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

    private fun loadActivityDetail() {
        activityDetailJob?.cancel()
        activityDetailJob = viewModelScope.launch {
            custodialActivityService
                .getActivity(
                    id = activityTxId
                )
                .filterNotLoading()
                .flatMapConcat { summaryDataResource ->
                    when (summaryDataResource) {
                        is DataResource.Data -> {
                            with(summaryDataResource.data) {
                                when (this) {
                                    is CustodialTradingActivitySummaryItem -> tradingDetail()
                                    is CustodialTransferActivitySummaryItem -> interestDetail()
                                    is CustodialInterestActivitySummaryItem -> interestDetail()
                                    is CustodialStakingActivitySummaryItem -> stackingDetail()
                                    is CustodialActiveRewardsActivitySummaryItem -> activeRewardsDetail()
                                    is RecurringBuyActivitySummaryItem -> recurringBuyDetail()
                                    is TradeActivitySummaryItem -> when {
                                        isSellingPair() -> sellDetail()
                                        isSwapPair() -> swapDetail()
                                        else -> error("unsupported")
                                    }
                                    is FiatActivitySummaryItem -> fiatDetail()
                                    // todo rest of types
                                    else -> flowOf(DataResource.Loading)
                                }
                            }
                        }
                        is DataResource.Error -> flowOf(DataResource.Error(summaryDataResource.error))
                        DataResource.Loading -> flowOf(DataResource.Loading)
                    }
                }
                .collect { dataResource ->
                    updateState {
                        copy(activityDetail = activityDetail.updateDataWith(dataResource))
                    }
                }
        }
    }

    /**
     * fetch more detail when it's PAYMENT_CARD and BANK_TRANSFER
     * otherwise default data
     */
    private fun CustodialTradingActivitySummaryItem.tradingDetail(): Flow<DataResource<CustodialActivityDetail>> {
        return paymentMethodType.paymentMethod(
            paymentMethodId = paymentMethodId,
            fundedFiat = fundedFiat
        ).mapData { paymentDetails ->
            buildActivityDetail(paymentDetails)
        }
    }

    private fun CustodialTransferActivitySummaryItem.interestDetail(): Flow<DataResource<CustodialActivityDetail>> {
        return flowOf(DataResource.Data(buildActivityDetail()))
    }

    private fun CustodialInterestActivitySummaryItem.interestDetail(): Flow<DataResource<CustodialActivityDetail>> {
        return flowOf(DataResource.Data(buildActivityDetail()))
    }

    private fun CustodialStakingActivitySummaryItem.stackingDetail(): Flow<DataResource<CustodialActivityDetail>> {
        return flowOf(DataResource.Data(buildActivityDetail()))
    }

    private fun CustodialActiveRewardsActivitySummaryItem.activeRewardsDetail():
        Flow<DataResource<CustodialActivityDetail>> {
        return flowOf(DataResource.Data(buildActivityDetail()))
    }

    private fun RecurringBuyActivitySummaryItem.recurringBuyDetail(): Flow<DataResource<CustodialActivityDetail>> {
        return recurringBuyId?.let {
            val recurringBuyFlow = recurringBuyService.recurringBuy(id = it, includeInactive = true)
                .onErrorReturn { null }

            val paymentDetailFlow = paymentMethodType.paymentMethod(
                paymentMethodId = paymentMethodId,
                fundedFiat = fundedFiat
            )

            combine(recurringBuyFlow, paymentDetailFlow) { recurringBuy, paymentDetail ->
                combineDataResources(recurringBuy, paymentDetail) { recurringBuyData, paymentDetailData ->
                    buildActivityDetail(recurringBuy = recurringBuyData, paymentDetails = paymentDetailData)
                }
            }
        } ?: flowOf(DataResource.Error(Exception("recurringBuyId not found")))
    }

    private suspend fun TradeActivitySummaryItem.sellDetail(): Flow<DataResource<CustodialActivityDetail>> {
        return flowOf(
            depositNetworkFee
                .map { fee ->
                    @Suppress("USELESS_CAST")
                    DataResource.Data(buildSellActivityDetail(fee = fee)) as DataResource<CustodialActivityDetail>
                }
                .onErrorReturn {
                    DataResource.Error(Exception(it))
                }
                .await()
        )
    }

    private suspend fun TradeActivitySummaryItem.swapDetail(): Flow<DataResource<CustodialActivityDetail>> {
        val depositNetworkFeeFlow = flowOf(
            depositNetworkFee
                .map { fee ->
                    @Suppress("USELESS_CAST")
                    DataResource.Data(fee) as DataResource<Money>
                }
                .onErrorReturn { DataResource.Error(Exception(it)) }
                .await()
        )

        val defaultToLabelFlow = flowOf(
            when (direction) {
                TransferDirection.ON_CHAIN -> {
                    coincore.findAccountByAddress(
                        currencyPair.destination.asAssetInfoOrThrow(),
                        receivingAddress!!
                    ).toSingle().map {
                        val defaultLabel = defaultLabels.getDefaultNonCustodialWalletLabel()

                        if (it.label.isEmpty() || it.label == defaultLabel) {
                            "${currencyPair.destination.displayTicker} $defaultLabel"
                        } else {
                            it.label
                        }
                    }
                }
                TransferDirection.INTERNAL,
                TransferDirection.FROM_USERKEY -> {
                    coincore[currencyPair.destination.asAssetInfoOrThrow()]
                        .accountGroup(AssetFilter.Trading)
                        .toSingle()
                        .map {
                            "${currencyPair.destination.displayTicker} ${it.selectFirstAccount().label}"
                        }
                }
                TransferDirection.TO_USERKEY -> {
                    error("TO_USERKEY swap direction not supported")
                }
            }.onErrorReturn { "" }
                .map {
                    @Suppress("USELESS_CAST")
                    DataResource.Data(it) as DataResource<String>
                }
                .onErrorReturn {
                    DataResource.Error(Exception(it))
                }
                .await()
        )

        return combine(depositNetworkFeeFlow, defaultToLabelFlow) { fee, toLabel ->
            combineDataResources(fee, toLabel) { feeData, toLabelData ->
                buildSwapActivityDetail(fee = feeData, toLabel = toLabelData)
            }
        }
    }

    private fun FiatActivitySummaryItem.fiatDetail(): Flow<DataResource<CustodialActivityDetail>> {
        return paymentMethodService.getPaymentMethodDetailsForId(
            paymentId = paymentMethodId.orEmpty(),
            freshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
        )
            .mapData { paymentMethodDetails ->
                buildActivityDetail(paymentMethodDetails)
            }
    }

    private fun PaymentMethodType.paymentMethod(
        paymentMethodId: String,
        fundedFiat: Money
    ): Flow<DataResource<PaymentDetails>> {
        return when (this) {
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
