package com.blockchain.home.presentation.fiat.actions

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.fiat.LinkedBanksFactory
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.domain.dataremediation.DataRemediationService
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.domain.dataremediation.model.QuestionnaireContext
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.extensions.exhaustive
import com.blockchain.home.presentation.fiat.actions.models.FiatTransactionRequestResult
import com.blockchain.home.presentation.fiat.actions.models.LinkablePaymentMethods
import com.blockchain.home.presentation.fiat.actions.models.LinkablePaymentMethodsForAction
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.utils.rxMaybeOutcome
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.Optional
import kotlinx.coroutines.rx3.asCoroutineDispatcher

class FiatActionsViewModel(
    private val dataRemediationService: DataRemediationService,
    private val userIdentity: UserIdentity,
    private val linkedBanksFactory: LinkedBanksFactory,
    private val bankService: BankService,
) : MviViewModel<
    FiatActionsIntent, FiatActionsViewState, FiatActionsModelState, FiatActionsNavEvent, ModelConfigArgs.NoArgs>(
    FiatActionsModelState
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: FiatActionsModelState) = FiatActionsViewState

    override suspend fun handleIntent(modelState: FiatActionsModelState, intent: FiatActionsIntent) {
        when (intent) {
            is FiatActionsIntent.FiatDeposit -> {
                require(intent.account is FiatAccount) { "account is not FiatAccount" }
                handleFiatDeposit(
                    targetAccount = intent.account,
                    shouldLaunchBankLinkTransfer = intent.shouldLaunchBankLinkTransfer,
                    shouldSkipQuestionnaire = intent.shouldSkipQuestionnaire,
                    action = intent.action
                )
            }
        }
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
                //                Single.just(
                //                    FiatTransactionRequestResult.BlockedDueToSanctions(
                //                        eligibility.reason as BlockedReason.Sanctions
                //                    )
                //                )
                TODO()
            questionnaireOpt.isPresent ->
                //                Single.just(
                //                FiatTransactionRequestResult.LaunchQuestionnaire(
                //                    questionnaire = questionnaireOpt.get(),
                //                    callbackIntent = DashboardIntent.LaunchBankTransferFlow(
                //                        targetAccount,
                //                        action,
                //                        shouldLaunchBankLinkTransfer,
                //                        shouldSkipQuestionnaire = true
                //                    )
                //                )
                //            )
                TODO()
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
                //                Single.just(
                //                    FiatTransactionRequestResult.LaunchDepositFlow(
                //                        preselectedBankAccount = linkedBanks.first(),
                //                        action = action,
                //                        targetAccount = targetAccount
                //                    )
                //                )
                TODO()
            }
            else -> {
                //                Single.just(
                //                    FiatTransactionRequestResult.LaunchDepositFlowWithMultipleAccounts(
                //                        action = action,
                //                        targetAccount = targetAccount
                //                    )
                //                )
                TODO()
            }
        }
    }.subscribeBy(
        onSuccess = {
            handlePaymentMethodsUpdate(it, targetAccount, action)
        },
        onError = {
            //            Timber.e("Error loading bank transfer info $it")
        }
    )

    private fun handlePaymentMethodsUpdate(
        fiatTxRequestResult: FiatTransactionRequestResult,
        fiatAccount: FiatAccount,
        action: AssetAction,
    ) {
        when (fiatTxRequestResult) {
            is FiatTransactionRequestResult.LaunchDepositFlowWithMultipleAccounts -> {
                //                DashboardIntent.UpdateNavigationAction(
                //                    DashboardNavigationAction.TransactionFlow(
                //                        target = fiatAccount,
                //                        action = action
                //                    )
                //                )

                TODO()
            }
            is FiatTransactionRequestResult.LaunchDepositFlow -> {
                //                DashboardIntent.UpdateNavigationAction(
                //                    DashboardNavigationAction.TransactionFlow(
                //                        target = fiatAccount,
                //                        sourceAccount = fiatTxRequestResult.preselectedBankAccount,
                //                        action = action
                //                    )
                //                )
                TODO()
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
                //                DashboardIntent.LaunchBankLinkFlow(
                //                    fiatTxRequestResult.linkBankTransfer,
                //                    fiatAccount,
                //                    action
                //                )
                TODO()
            }
            is FiatTransactionRequestResult.NotSupportedPartner -> {
                // TODO Show an error
            }
            is FiatTransactionRequestResult.BlockedDueToSanctions -> {
                //                DashboardIntent.UpdateNavigationAction(
                //                    DashboardNavigationAction.FiatDepositOrWithdrawalBlockedDueToSanctions(
                //                        fiatTxRequestResult.reason
                //                    )
                //                )
                TODO()
            }
            is FiatTransactionRequestResult.LaunchQuestionnaire -> {
                //                DashboardIntent.UpdateNavigationAction(
                //                    DashboardNavigationAction.DepositQuestionnaire(
                //                        questionnaire = fiatTxRequestResult.questionnaire,
                //                        callbackIntent = fiatTxRequestResult.callbackIntent
                //                    )
                //                )
                TODO()
            }
            is FiatTransactionRequestResult.LaunchPaymentMethodChooser -> {
                //                DashboardIntent.ShowLinkablePaymentMethodsSheet(
                //                    fiatAccount = fiatAccount,
                //                    paymentMethodsForAction = fiatTxRequestResult.paymentMethodForAction
                //                )
                TODO()
            }
            is FiatTransactionRequestResult.LaunchDepositDetailsSheet -> {
                navigate(FiatActionsNavEvent.WireTransferAccountDetails(account = fiatAccount))
            }
            is FiatTransactionRequestResult.LaunchAliasWithdrawal -> {
                //                DashboardIntent.ShowBankLinkingWithAlias(fiatTxRequestResult.targetAccount)
                TODO()
            }
            null -> {
            }
        }.exhaustive
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
}
