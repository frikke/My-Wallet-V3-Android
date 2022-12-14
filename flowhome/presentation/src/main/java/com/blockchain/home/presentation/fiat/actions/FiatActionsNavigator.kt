package com.blockchain.home.presentation.fiat.actions

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.FiatAccount
import com.blockchain.fiatActions.fiatactions.FiatActionsUseCase
import com.blockchain.fiatActions.fiatactions.models.FiatActionsResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FiatActionsNavigator(
    private val scope: CoroutineScope,
    private val fiatActions: FiatActionsUseCase
) {
    private var account: FiatAccount? = null
    private var action: AssetAction? = null

    private val _navigator = MutableSharedFlow<FiatActionsNavEvent>()
    val navigator: Flow<FiatActionsNavEvent> get() = _navigator

    init {
        scope.launch {
            fiatActions.result.collectLatest { result ->
                account = result.account
                action = result.action

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

    fun performAction(
        request: FiatActionRequest
    ) {
        when (request) {
            is FiatActionRequest.Deposit -> {
                account = request.account
                action = request.action

                fiatActions.deposit(
                    account = request.account,
                    action = request.action,
                    shouldLaunchBankLinkTransfer = request.shouldLaunchBankLinkTransfer,
                    shouldSkipQuestionnaire = request.shouldSkipQuestionnaire
                )
            }

            is FiatActionRequest.RestartDeposit -> {
                check(account != null) { "account undefined" }
                val action = request.action ?: action ?: error("action undefined")

                fiatActions.deposit(
                    account = account!!,
                    action = action,
                    shouldLaunchBankLinkTransfer = request.shouldLaunchBankLinkTransfer,
                    shouldSkipQuestionnaire = request.shouldSkipQuestionnaire
                )
            }

            FiatActionRequest.WireTransferAccountDetails -> {
                check(account != null) { "account undefined" }

                navigate(
                    FiatActionsNavEvent.WireTransferAccountDetails(
                        account = account!!
                    )
                )
            }
        }
    }

    private fun navigate(event: FiatActionsNavEvent) {
        scope.launch {
            _navigator.emit(event)
        }
    }
}
