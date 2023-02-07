package com.blockchain.home.presentation.fiat.actions

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.FiatAccount
import com.blockchain.fiatActions.fiatactions.FiatActionsUseCase
import com.blockchain.fiatActions.fiatactions.models.FiatActionsResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.take
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
                    is FiatActionsResult.LaunchQuestionnaire -> {
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
                                account = result.account,
                                target = result.target,
                                action = result.action
                            )
                        )
                    }
                    is FiatActionsResult.WireTransferAccountDetails -> {
                        navigate(
                            FiatActionsNavEvent.WireTransferAccountDetails(
                                account = result.account,
                                accountIsFunded = result.accountIsFunded
                            )
                        )
                    }
                    is FiatActionsResult.BankLinkFlow -> {
                        navigate(
                            FiatActionsNavEvent.BankLinkFlow(
                                linkBankTransfer = result.linkBankTransfer,
                                account = result.account,
                                action = result.action
                            )
                        )
                    }
                    is FiatActionsResult.LinkBankWithAlias -> {
                        navigate(
                            FiatActionsNavEvent.LinkBankWithAlias(
                                account = result.account,
                                action = result.action
                            )
                        )
                    }
                    is FiatActionsResult.KycDepositCashBenefits -> navigate(
                        FiatActionsNavEvent.KycCashBenefits(
                            currency = result.currency
                        )
                    )
                }
            }
        }
    }

    fun performAction(
        request: FiatActionRequest
    ) {
        when (request) {
            is FiatActionRequest.Restart -> {
                check(account != null) { "account undefined" }
                val action = request.action ?: action ?: error("action undefined")

                when (action) {
                    AssetAction.FiatDeposit -> fiatActions.deposit(
                        account = account!!,
                        action = action,
                        shouldLaunchBankLinkTransfer = request.shouldLaunchBankLinkTransfer,
                        shouldSkipQuestionnaire = request.shouldSkipQuestionnaire
                    )
                    AssetAction.FiatWithdraw -> fiatActions.withdraw(
                        account = account!!,
                        action = action,
                        shouldLaunchBankLinkTransfer = request.shouldLaunchBankLinkTransfer,
                        shouldSkipQuestionnaire = request.shouldSkipQuestionnaire
                    )
                    else -> error("unsupported")
                }
            }

            FiatActionRequest.WireTransferAccountDetails -> {
                check(account != null) { "account undefined" }
                scope.launch {
                    account!!.balance().take(1).collectLatest {
                        navigate(
                            FiatActionsNavEvent.WireTransferAccountDetails(
                                account = account!!,
                                accountIsFunded = it.total.isPositive
                            )
                        )
                    }
                }
            }
        }
    }

    private fun navigate(event: FiatActionsNavEvent) {
        scope.launch {
            _navigator.emit(event)
        }
    }
}
