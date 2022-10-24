package com.blockchain.home.presentation.activity.list

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.data.map
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.dashboard.HomeNavEvent

class ActivityViewModel(
    // todo
) : MviViewModel<
    ActivityIntent,
    ActivityViewState,
    ActivityModelState,
    HomeNavEvent,
    ModelConfigArgs.NoArgs>(ActivityModelState()) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun reduce(state: ActivityModelState): ActivityViewState = state.run {
        ActivityViewState(
            activity = activity.map {
                when (val sectionSize = state.sectionSize) {
                    SectionSize.All -> {
                        it
                    }
                    is SectionSize.Limited -> {
                        // todo do we need to filter out pending?
                        // todo make sure it's date sorted
                        mapOf(
                            TransactionGroup.Combined to it.values.flatten().take(sectionSize.size)
                        )
                    }
                }
            }
        )
    }

    override suspend fun handleIntent(modelState: ActivityModelState, intent: ActivityIntent) {
        when (intent) {
            is ActivityIntent.LoadActivity -> {
                updateState { it.copy(sectionSize = intent.sectionSize) }

                updateState {
                    it.copy(
                        activity = DataResource.Data(
                            mapOf(
                                TransactionGroup.Group("Pending") to listOf(
                                    TransactionState(
                                        transactionTypeIcon = "transactionTypeIcon",
                                        transactionCoinIcon = "transactionCoinIcon",
                                        TransactionStatus.Pending(),
                                        valueTopStart = "Sent Bitcoin",
                                        valueTopEnd = "-10.00",
                                        valueBottomStart = "85% confirmed",
                                        valueBottomEnd = "-0.00893208 ETH"
                                    ),
                                    TransactionState(
                                        transactionTypeIcon = "Cashed Out USD",
                                        transactionCoinIcon = "transactionCoinIcon",
                                        TransactionStatus.Pending(isRbfTransaction = true),
                                        valueTopStart = "Sent Bitcoin",
                                        valueTopEnd = "-25.00",
                                        valueBottomStart = "RBF transaction",
                                        valueBottomEnd = "valueBottomEnd"
                                    )
                                ),
                                TransactionGroup.Group("June") to listOf(
                                    TransactionState(
                                        transactionTypeIcon = "transactionTypeIcon",
                                        transactionCoinIcon = "transactionCoinIcon",
                                        TransactionStatus.Confirmed,
                                        valueTopStart = "Sent Bitcoin",
                                        valueTopEnd = "-10.00",
                                        valueBottomStart = "June 14",
                                        valueBottomEnd = "-0.00893208 ETH"
                                    ),
                                    TransactionState(
                                        transactionTypeIcon = "Cashed Out USD",
                                        transactionCoinIcon = "transactionCoinIcon",
                                        TransactionStatus.Canceled,
                                        valueTopStart = "Sent Bitcoin",
                                        valueTopEnd = "-25.00",
                                        valueBottomStart = "Canceled",
                                        valueBottomEnd = "valueBottomEnd"
                                    ),
                                    TransactionState(
                                        transactionTypeIcon = "transactionTypeIcon",
                                        transactionCoinIcon = "transactionCoinIcon",
                                        TransactionStatus.Canceled,
                                        valueTopStart = "Sent Bitcoin",
                                        valueTopEnd = "100.00",
                                        valueBottomStart = "Canceled",
                                        valueBottomEnd = "0.00025 BTC"
                                    )
                                ),
                                TransactionGroup.Group("July") to listOf(
                                    TransactionState(
                                        transactionTypeIcon = "transactionTypeIcon",
                                        transactionCoinIcon = "transactionCoinIcon",
                                        TransactionStatus.Declined,
                                        valueTopStart = "Added USD",
                                        valueTopEnd = "-25.00",
                                        valueBottomStart = "Declined",
                                        valueBottomEnd = "valueBottomEnd"
                                    ),
                                    TransactionState(
                                        transactionTypeIcon = "transactionTypeIcon",
                                        transactionCoinIcon = "transactionCoinIcon",
                                        TransactionStatus.Failed,
                                        valueTopStart = "Added USD",
                                        valueTopEnd = "-25.00",
                                        valueBottomStart = "Failed",
                                        valueBottomEnd = "valueBottomEnd"
                                    )
                                )
                            )
                        )
                    )
                }
            }
        }
    }
}
