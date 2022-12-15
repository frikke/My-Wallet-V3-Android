package com.blockchain.chrome.tbr

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.FiatAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.fiatActions.fiatactions.FiatActions
import com.blockchain.fiatActions.fiatactions.models.FiatActionsResult
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FiatActionsViewModel(
    private val fiatActions: FiatActions
) : MviViewModel<
    FiatActionsIntents,
    FiatActionsViewState,
    FiatActionsModelState,
    FiatActionsNavEvent,
    ModelConfigArgs.NoArgs>(
    FiatActionsModelState()
) {

    init {
        viewModelScope.launch {
            fiatActions.result.collectLatest { result ->
                updateState {
                    it.copy(
                        fiatAccount = result.account,
                        action = result.action
                    )
                }

                when (result) {
                    is FiatActionsResult.BlockedDueToSanctions -> {
                        navigate(
                            FiatActionsNavEvent.BlockedDueToSanctions(
                                reason = result.reason
                            )
                        )
                    }
                    is FiatActionsResult.DepositQuestionnaire -> {
                        navigate(
                            FiatActionsNavEvent.DepositQuestionnaire(
                                questionnaire = result.questionnaire
                            )
                        )
                    }
                    is FiatActionsResult.LinkBankMethod -> {
                        navigate(
                            FiatActionsNavEvent.LinkBankMethod(
                                paymentMethodsForAction = result.paymentMethodsForAction
                            )
                        )
                    }
                    is FiatActionsResult.TransactionFlow -> {
                        navigate(
                            FiatActionsNavEvent.TransactionFlow(
                                sourceAccount = result.account,
                                target = result.target,
                                action = result.action
                            )
                        )
                    }
                    is FiatActionsResult.WireTransferAccountDetails -> {
                        navigate(
                            FiatActionsNavEvent.WireTransferAccountDetails(
                                account = result.account
                            )
                        )
                    }
                    is FiatActionsResult.BankLinkFlow -> {
                        navigate(
                            FiatActionsNavEvent.BankLinkFlow(
                                linkBankTransfer = result.linkBankTransfer,
                                fiatAccount = result.account,
                                assetAction = result.action
                            )
                        )
                    }
                }
            }
        }
    }

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: FiatActionsModelState) = FiatActionsViewState()

    override suspend fun handleIntent(
        modelState: FiatActionsModelState,
        intent: FiatActionsIntents
    ) {
        when (intent) {
            is FiatActionsIntents.Deposit -> {
                updateState {
                    it.copy(
                        fiatAccount = intent.account,
                        action = intent.action
                    )
                }

                fiatActions.deposit(
                    account = intent.account,
                    action = intent.action,
                    shouldLaunchBankLinkTransfer = intent.shouldLaunchBankLinkTransfer,
                    shouldSkipQuestionnaire = intent.shouldSkipQuestionnaire
                )
            }

            is FiatActionsIntents.RestartDeposit -> {
                check(modelState.fiatAccount != null)
                val action = intent.action ?: modelState.action ?: error("action undefined")
                fiatActions.deposit(
                    account = modelState.fiatAccount,
                    action = action,
                    shouldLaunchBankLinkTransfer = intent.shouldLaunchBankLinkTransfer,
                    shouldSkipQuestionnaire = intent.shouldSkipQuestionnaire
                )
            }
            FiatActionsIntents.WireTransferAccountDetails -> {
                check(modelState.fiatAccount != null)
                navigate(
                    FiatActionsNavEvent.WireTransferAccountDetails(
                        account = modelState.fiatAccount
                    )
                )
            }
        }
    }
}

class FiatActionsViewState : ViewState

data class FiatActionsModelState(
    val fiatAccount: FiatAccount? = null,
    val action: AssetAction? = null,
) : ModelState
