package com.blockchain.home.presentation.activity.detail.custodial

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.FiatActivitySummaryItem
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.data.DataResource
import com.blockchain.data.map
import com.blockchain.data.updateDataWith
import com.blockchain.domain.paymentmethods.PaymentMethodService
import com.blockchain.domain.paymentmethods.model.MobilePaymentType
import com.blockchain.home.activity.CustodialActivityService
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.activity.detail.ActivityDetailViewState
import com.blockchain.home.presentation.activity.detail.custodial.mappers.toActivityDetail
import com.blockchain.home.presentation.dashboard.HomeNavEvent
import com.blockchain.store.mapData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class CustodialActivityDetailViewModel(
    private val activityTxId: String,
    private val custodialActivityService: CustodialActivityService,
    private val paymentMethodService: PaymentMethodService
) : MviViewModel<
    CustodialActivityDetailIntent,
    ActivityDetailViewState,
    CustodialActivityDetailModelState,
    HomeNavEvent,
    ModelConfigArgs.NoArgs>(CustodialActivityDetailModelState()) {

    private var activityDetailJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: CustodialActivityDetailModelState): ActivityDetailViewState = state.run {
        ActivityDetailViewState(
            activityDetail = activityDetail.map { it.toActivityDetail() }
        )
    }

    override suspend fun handleIntent(
        modelState: CustodialActivityDetailModelState,
        intent: CustodialActivityDetailIntent
    ) {
        when (intent) {
            is CustodialActivityDetailIntent.LoadActivityDetail -> {
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
                                    is FiatActivitySummaryItem -> {
                                        fiatDetail()
                                    }
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

    private suspend fun FiatActivitySummaryItem.fiatDetail(): Flow<DataResource<CustodialActivityDetail>> {
        return paymentMethodService.getPaymentMethodDetailsForId(paymentMethodId.orEmpty())
            .mapData { paymentMethodDetails ->
                CustodialActivityDetail(
                    activity = this,
                    extras = listOf(
                        CustodialActivityDetailExtra(
                            title = TextValue.IntResValue(R.string.activity_details_buy_payment_method),
                            value = with(paymentMethodDetails) {
                                when {
                                    mobilePaymentType == MobilePaymentType.GOOGLE_PAY -> TextValue.IntResValue(
                                        R.string.google_pay
                                    )
                                    mobilePaymentType == MobilePaymentType.APPLE_PAY -> TextValue.IntResValue(
                                        R.string.apple_pay
                                    )
                                    paymentMethodDetails.label.isNullOrBlank() -> TextValue.StringValue(
                                        account.currency.name
                                    )
                                    else -> TextValue.StringValue(
                                        "${paymentMethodDetails.label} ${paymentMethodDetails.endDigits}"
                                    )
                                }
                            }
                        )
                    )
                )
            }
    }
}
