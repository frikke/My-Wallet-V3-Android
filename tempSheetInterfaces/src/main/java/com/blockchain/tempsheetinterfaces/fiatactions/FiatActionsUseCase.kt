package com.blockchain.tempsheetinterfaces.fiatactions

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.fiat.LinkedBanksFactory
import com.blockchain.domain.dataremediation.DataRemediationService
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.domain.dataremediation.model.QuestionnaireContext
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.tempsheetinterfaces.fiatactions.models.FiatActionsResult
import com.blockchain.tempsheetinterfaces.fiatactions.models.FiatTransactionRequestResult
import com.blockchain.tempsheetinterfaces.fiatactions.models.LinkablePaymentMethods
import com.blockchain.tempsheetinterfaces.fiatactions.models.LinkablePaymentMethodsForAction
import com.blockchain.utils.rxMaybeOutcome
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asCoroutineDispatcher
import java.util.Optional

class FiatActionsUseCase(
    private val scope: CoroutineScope,
    private val dataRemediationService: DataRemediationService,
    private val userIdentity: UserIdentity,
    private val linkedBanksFactory: LinkedBanksFactory,
    private val bankService: BankService,
) {

    private val _result = MutableSharedFlow<FiatActionsResult>()
    val result: SharedFlow<FiatActionsResult> get() = _result

    fun deposit(
        account: SingleAccount,
        action: AssetAction,
        shouldLaunchBankLinkTransfer: Boolean,
        shouldSkipQuestionnaire: Boolean = false
    ) {
        require(account is FiatAccount) { "account is not FiatAccount" }

        handleFiatDeposit(
            targetAccount = account,
            shouldLaunchBankLinkTransfer = shouldLaunchBankLinkTransfer,
            shouldSkipQuestionnaire = shouldSkipQuestionnaire,
            action = action
        )
    }

    private fun handleFiatDeposit(
        targetAccount: FiatAccount,
        shouldLaunchBankLinkTransfer: Boolean,
        shouldSkipQuestionnaire: Boolean,
        action: AssetAction,
    ) = Singles.zip(
        getQuestionnaireIfNeeded(shouldSkipQuestionnaire, QuestionnaireContext.FIAT_DEPOSIT),
        userIdentity.userAccessForFeature(Feature.DepositFiat),
        linkedBanksFactory.eligibleBankPaymentMethods(targetAccount.currency).map { paymentMethods ->
            // Ignore any WireTransferMethods In case BankLinkTransfer should launch
            paymentMethods.filter { it == PaymentMethodType.BANK_TRANSFER || !shouldLaunchBankLinkTransfer }
        },
        linkedBanksFactory.getNonWireTransferBanks().map {
            it.filter { bank -> bank.currency == targetAccount.currency }
        }
    ) { questionnaireOpt, eligibility, paymentMethods, linkedBanks ->
        (questionnaireOpt to eligibility) to (paymentMethods to linkedBanks)
    }.flatMap { (questionnaireOptAndEligibility, paymentMethodsAndLinkedBanks) ->
        val (questionnaireOpt, eligibility) = questionnaireOptAndEligibility
        val (paymentMethods, linkedBanks) = paymentMethodsAndLinkedBanks

        val eligibleBanks = linkedBanks.filter { paymentMethods.contains(it.type) }

        //        analytics.logEvent(DepositMethodOptionsViewed(paymentMethods.map { it.name }))

        when {
            eligibility is FeatureAccess.Blocked && eligibility.reason is BlockedReason.Sanctions ->
                Single.just(
                    FiatTransactionRequestResult.BlockedDueToSanctions(
                        eligibility.reason as BlockedReason.Sanctions
                    )
                )
            questionnaireOpt.isPresent ->
                Single.just(
                    FiatTransactionRequestResult.LaunchQuestionnaire(
                        // todo othman find an account with this
                        questionnaire = questionnaireOpt.get(),
                        //                        callbackIntent = DashboardIntent.LaunchBankTransferFlow(
                        //                            targetAccount,
                        //                            action,
                        //                            shouldLaunchBankLinkTransfer,
                        //                            shouldSkipQuestionnaire = true
                        //                        )
                    )
                )
            eligibleBanks.isEmpty() -> {
                handleNoLinkedBanks(
                    targetAccount,
                    action,
                    LinkablePaymentMethodsForAction.LinkablePaymentMethodsForDeposit(
                        linkablePaymentMethods = LinkablePaymentMethods(
                            targetAccount.currency,
                            paymentMethods.sortedBy { it.ordinal }
                        )
                    )
                )
            }
            eligibleBanks.size == 1 -> {
                Single.just(
                    FiatTransactionRequestResult.LaunchDepositFlow(
                        preselectedBankAccount = linkedBanks.first(),
                        action = action,
                        targetAccount = targetAccount
                    )
                )
            }
            else -> {
                Single.just(
                    FiatTransactionRequestResult.LaunchDepositFlowWithMultipleAccounts(
                        action = action,
                        targetAccount = targetAccount
                    )
                )
            }
        }
    }.map {
        handlePaymentMethodsUpdate(it, targetAccount, action)
    }.subscribe { result ->
        scope.launch {
            _result.emit(result)
        }
    }

    private fun getQuestionnaireIfNeeded(
        shouldSkipQuestionnaire: Boolean,
        questionnaireContext: QuestionnaireContext,
    ): Single<Optional<Questionnaire>> =
        if (shouldSkipQuestionnaire) {
            Single.just(Optional.empty())
        } else {
            rxMaybeOutcome(Schedulers.io().asCoroutineDispatcher()) {
                dataRemediationService.getQuestionnaire(questionnaireContext)
            }.map { Optional.of(it) }
                .defaultIfEmpty(Optional.empty())
        }

    private fun handleNoLinkedBanks(
        targetAccount: FiatAccount,
        action: AssetAction,
        paymentMethodForAction: LinkablePaymentMethodsForAction,
    ): Single<FiatTransactionRequestResult> {
        return when {
            paymentMethodForAction.linkablePaymentMethods.linkMethods.containsAll(
                listOf(PaymentMethodType.BANK_TRANSFER, PaymentMethodType.BANK_ACCOUNT)
            ) -> {
                Single.just(
                    FiatTransactionRequestResult.LaunchPaymentMethodChooser(
                        paymentMethodForAction
                    )
                )
            }
            paymentMethodForAction.linkablePaymentMethods.linkMethods.contains(PaymentMethodType.BANK_TRANSFER) -> {
                linkBankTransfer(targetAccount.currency).map {
                    FiatTransactionRequestResult.LaunchBankLink(
                        linkBankTransfer = it,
                        action = action
                    ) as FiatTransactionRequestResult
                }.onErrorReturn {
                    FiatTransactionRequestResult.NotSupportedPartner
                }
            }
            paymentMethodForAction.linkablePaymentMethods.linkMethods.contains(PaymentMethodType.BANK_ACCOUNT) -> {
                userIdentity.isArgentinian().flatMap { isArgentinian ->
                    if (isArgentinian && action == AssetAction.FiatWithdraw) {
                        Single.just(FiatTransactionRequestResult.LaunchAliasWithdrawal(targetAccount))
                    } else {
                        Single.just(FiatTransactionRequestResult.LaunchDepositDetailsSheet(targetAccount))
                    }
                }
            }
            else -> {
                Single.just(FiatTransactionRequestResult.NotSupportedPartner)
            }
        }
    }

    fun linkBankTransfer(currency: FiatCurrency): Single<LinkBankTransfer> =
        bankService.linkBank(currency)

    private fun handlePaymentMethodsUpdate(
        fiatTxRequestResult: FiatTransactionRequestResult,
        fiatAccount: FiatAccount,
        action: AssetAction,
    ): FiatActionsResult {
        return when (fiatTxRequestResult) {
            is FiatTransactionRequestResult.LaunchDepositFlowWithMultipleAccounts -> {
                FiatActionsResult.TransactionFlow(
                    target = fiatAccount,
                    action = action
                )
            }
            is FiatTransactionRequestResult.LaunchDepositFlow -> {
                FiatActionsResult.TransactionFlow(
                    account = fiatTxRequestResult.preselectedBankAccount,
                    action = action,
                    target = fiatAccount
                )
            }
            is FiatTransactionRequestResult.LaunchWithdrawalFlowWithMultipleAccounts -> {
                //                DashboardIntent.UpdateNavigationAction(
                //                    DashboardNavigationAction.TransactionFlow(
                //                        sourceAccount = fiatAccount,
                //                        action = action
                //                    )
                //                )
                TODO()
            }
            is FiatTransactionRequestResult.LaunchWithdrawalFlow -> {
                //                DashboardIntent.UpdateNavigationAction(
                //                    DashboardNavigationAction.TransactionFlow(
                //                        sourceAccount = fiatAccount,
                //                        target = fiatTxRequestResult.preselectedBankAccount,
                //                        action = action
                //                    )
                //                )
                TODO()
            }
            is FiatTransactionRequestResult.LaunchBankLink -> {
                FiatActionsResult.BankLinkFlow(
                    account = fiatAccount,
                    action = action,
                    linkBankTransfer = fiatTxRequestResult.linkBankTransfer
                )
            }
            is FiatTransactionRequestResult.NotSupportedPartner -> {
                // TODO Show an error
                TODO()
            }
            is FiatTransactionRequestResult.BlockedDueToSanctions -> {
                FiatActionsResult.BlockedDueToSanctions(
                    account = fiatAccount,
                    action = action,
                    reason = fiatTxRequestResult.reason
                )
            }
            is FiatTransactionRequestResult.LaunchQuestionnaire -> {
                FiatActionsResult.DepositQuestionnaire(
                    account = fiatAccount,
                    action = action,
                    questionnaire = fiatTxRequestResult.questionnaire
                )
                //                DashboardIntent.UpdateNavigationAction(
                //                    DashboardNavigationAction.DepositQuestionnaire(
                //                        questionnaire = fiatTxRequestResult.questionnaire,
                //                        callbackIntent = fiatTxRequestResult.callbackIntent
                //                    )
                //                )
            }
            is FiatTransactionRequestResult.LaunchPaymentMethodChooser -> {
                FiatActionsResult.LinkBankMethod(
                    account = fiatAccount,
                    action = action,
                    paymentMethodsForAction = fiatTxRequestResult.paymentMethodForAction
                )
            }
            is FiatTransactionRequestResult.LaunchDepositDetailsSheet -> {
                FiatActionsResult.WireTransferAccountDetails(
                    account = fiatAccount,
                    action = action,
                )
            }
            is FiatTransactionRequestResult.LaunchAliasWithdrawal -> {
                //                DashboardIntent.ShowBankLinkingWithAlias(fiatTxRequestResult.targetAccount)
                TODO()
            }
        }
    }
}
