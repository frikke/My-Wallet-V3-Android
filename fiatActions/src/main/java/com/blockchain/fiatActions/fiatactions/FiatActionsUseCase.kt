package com.blockchain.fiatActions.fiatactions

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.fiat.LinkedBanksFactory
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.domain.dataremediation.DataRemediationService
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.domain.dataremediation.model.QuestionnaireContext
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.fiatActions.fiatactions.models.FiatActionsResult
import com.blockchain.fiatActions.fiatactions.models.FiatTransactionRequestResult
import com.blockchain.fiatActions.fiatactions.models.LinkablePaymentMethods
import com.blockchain.fiatActions.fiatactions.models.LinkablePaymentMethodsForAction
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.utils.rxMaybeOutcome
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.Optional
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asCoroutineDispatcher

class FiatActionsUseCase(
    private val scope: CoroutineScope,
    private val dataRemediationService: DataRemediationService,
    private val userIdentity: UserIdentity,
    private val linkedBanksFactory: LinkedBanksFactory,
    private val bankService: BankService
) {
    private val _result = MutableSharedFlow<FiatActionsResult>()
    val result: SharedFlow<FiatActionsResult> get() = _result

    fun noEligibleAccount(currency: FiatCurrency) {
        scope.launch {
            _result.emit(FiatActionsResult.KycDepositCashBenefits(currency))
        }
    }

    fun deposit(
        account: FiatAccount,
        action: AssetAction,
        shouldLaunchBankLinkTransfer: Boolean,
        shouldSkipQuestionnaire: Boolean = false
    ) {
        handleDeposit(
            account = account,
            shouldLaunchBankLinkTransfer = shouldLaunchBankLinkTransfer,
            shouldSkipQuestionnaire = shouldSkipQuestionnaire,
            action = action
        )
    }

    fun withdraw(
        account: FiatAccount,
        action: AssetAction,
        shouldLaunchBankLinkTransfer: Boolean,
        shouldSkipQuestionnaire: Boolean
    ) {
        handleWithdraw(
            account = account,
            shouldLaunchBankLinkTransfer = shouldLaunchBankLinkTransfer,
            shouldSkipQuestionnaire = shouldSkipQuestionnaire,
            action = action
        )
    }

    private fun handleDeposit(
        account: FiatAccount,
        shouldLaunchBankLinkTransfer: Boolean,
        shouldSkipQuestionnaire: Boolean,
        action: AssetAction
    ) = Singles.zip(
        getQuestionnaireIfNeeded(shouldSkipQuestionnaire, QuestionnaireContext.FIAT_DEPOSIT),
        userIdentity.userAccessForFeature(
            Feature.DepositFiat,
            freshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
        ),
        linkedBanksFactory.eligibleBankPaymentMethods(account.currency).map { paymentMethods ->
            // Ignore any WireTransferMethods In case BankLinkTransfer should launch
            paymentMethods.filter { it == PaymentMethodType.BANK_TRANSFER || !shouldLaunchBankLinkTransfer }
        },
        linkedBanksFactory.getNonWireTransferBanks().map {
            it.filter { bank -> bank.currency == account.currency }
        }
    ) { questionnaireOpt, eligibility, paymentMethods, linkedBanks ->
        (questionnaireOpt to eligibility) to (paymentMethods to linkedBanks)
    }.flatMap { (questionnaireOptAndEligibility, paymentMethodsAndLinkedBanks) ->
        val (questionnaireOpt, eligibility) = questionnaireOptAndEligibility
        val (paymentMethods, linkedBanks) = paymentMethodsAndLinkedBanks

        val eligibleBanks = linkedBanks.filter { paymentMethods.contains(it.type) }

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
                        questionnaire = questionnaireOpt.get()
                    )
                )

            eligibleBanks.isEmpty() -> {
                handleNoLinkedBanks(
                    account,
                    action,
                    LinkablePaymentMethodsForAction.LinkablePaymentMethodsForDeposit(
                        linkablePaymentMethods = LinkablePaymentMethods(
                            account.currency,
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
                        targetAccount = account
                    )
                )
            }

            else -> {
                Single.just(
                    FiatTransactionRequestResult.LaunchDepositFlowWithMultipleAccounts(
                        action = action,
                        targetAccount = account
                    )
                )
            }
        }
    }.map {
        handlePaymentMethodsUpdate(it, account, action)
    }.subscribeBy(
        onSuccess = { result ->
            scope.launch {
                _result.emit(result)
            }
        },
        onError = { error ->
            scope.launch {
                _result.emit(
                    FiatActionsResult.Failure(
                        action, (error as? Exception) ?: Exception(error.localizedMessage.orEmpty())
                    )
                )
            }
        }
    )

    private fun handleWithdraw(
        account: FiatAccount,
        action: AssetAction,
        shouldLaunchBankLinkTransfer: Boolean,
        shouldSkipQuestionnaire: Boolean
    ): Disposable {
        return Singles.zip(
            getQuestionnaireIfNeeded(shouldSkipQuestionnaire, QuestionnaireContext.FIAT_WITHDRAW),
            userIdentity.userAccessForFeature(Feature.WithdrawFiat, FreshnessStrategy.Fresh),
            linkedBanksFactory.eligibleBankPaymentMethods(account.currency)
                .map { paymentMethods ->
                    // Ignore any WireTransferMethods In case BankLinkTransfer should launch
                    paymentMethods.filter { it == PaymentMethodType.BANK_TRANSFER || !shouldLaunchBankLinkTransfer }
                },
            linkedBanksFactory.getAllLinkedBanks().map {
                it.filter { bank -> bank.currency == account.currency }
            }
        ) { questionnaireOpt, eligibility, paymentMethods, linkedBanks ->
            (questionnaireOpt to eligibility) to (paymentMethods to linkedBanks)
        }.flatMap { (questionnaireOptAndEligibility, paymentMethodsAndLinkedBanks) ->
            val (questionnaireOpt, eligibility) = questionnaireOptAndEligibility
            val (paymentMethods, linkedBanks) = paymentMethodsAndLinkedBanks

            //            analytics.logEvent(WithdrawMethodOptionsViewed(paymentMethods.map { it.name }))
            when {
                eligibility is FeatureAccess.Blocked && eligibility.reason is BlockedReason.Sanctions ->
                    Single.just(
                        FiatTransactionRequestResult.BlockedDueToSanctions(
                            eligibility.reason as BlockedReason.Sanctions
                        )
                    )

                questionnaireOpt.isPresent -> Single.just(
                    FiatTransactionRequestResult.LaunchQuestionnaire(
                        questionnaire = questionnaireOpt.get()
                    )
                )

                linkedBanks.isEmpty() -> {
                    handleNoLinkedBanks(
                        account,
                        action,
                        LinkablePaymentMethodsForAction.LinkablePaymentMethodsForWithdraw(
                            LinkablePaymentMethods(
                                account.currency,
                                paymentMethods.sortedBy { it.ordinal }
                            )
                        )
                    )
                }

                linkedBanks.size == 1 -> {
                    Single.just(
                        FiatTransactionRequestResult.LaunchWithdrawalFlow(
                            preselectedBankAccount = linkedBanks.first(),
                            action = action,
                            sourceAccount = account
                        )
                    )
                }

                else -> {
                    Single.just(
                        FiatTransactionRequestResult.LaunchWithdrawalFlowWithMultipleAccounts(
                            action = action,
                            sourceAccount = account
                        )
                    )
                }
            }
        }.map {
            handlePaymentMethodsUpdate(it, account, action)
        }.subscribeBy(
            onSuccess = { result ->
                scope.launch {
                    _result.emit(result)
                }
            },
            onError = { error ->
                scope.launch {
                    _result.emit(FiatActionsResult.Failure(action, (error as? Exception) ?: Exception()))
                }
            }
        )
    }

    private fun getQuestionnaireIfNeeded(
        shouldSkipQuestionnaire: Boolean,
        questionnaireContext: QuestionnaireContext
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
        paymentMethodForAction: LinkablePaymentMethodsForAction
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
                        targetAccount.balanceRx().firstOrError().map {
                            FiatTransactionRequestResult.LaunchDepositDetailsSheet(targetAccount, it.total.isPositive)
                        }
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
        action: AssetAction
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
                FiatActionsResult.TransactionFlow(
                    account = fiatAccount,
                    action = action
                )
            }

            is FiatTransactionRequestResult.LaunchWithdrawalFlow -> {
                FiatActionsResult.TransactionFlow(
                    account = fiatAccount,
                    action = action,
                    target = fiatTxRequestResult.preselectedBankAccount
                )
            }

            is FiatTransactionRequestResult.LaunchBankLink -> {
                FiatActionsResult.BankLinkFlow(
                    account = fiatAccount,
                    action = action,
                    linkBankTransfer = fiatTxRequestResult.linkBankTransfer
                )
            }

            is FiatTransactionRequestResult.NotSupportedPartner -> {
                FiatActionsResult.KycDepositCashBenefits(fiatAccount.currency)
            }

            is FiatTransactionRequestResult.BlockedDueToSanctions -> {
                FiatActionsResult.BlockedDueToSanctions(
                    account = fiatAccount,
                    action = action,
                    reason = fiatTxRequestResult.reason
                )
            }

            is FiatTransactionRequestResult.LaunchQuestionnaire -> {
                FiatActionsResult.LaunchQuestionnaire(
                    account = fiatAccount,
                    action = action,
                    questionnaire = fiatTxRequestResult.questionnaire
                )
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
                    accountIsFunded = fiatTxRequestResult.accountIsFunded
                )
            }

            is FiatTransactionRequestResult.LaunchAliasWithdrawal -> {
                FiatActionsResult.LinkBankWithAlias(
                    account = fiatAccount,
                    action = action
                )
            }
        }
    }
}
