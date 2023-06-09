package com.blockchain.chrome

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blockchain.deeplinking.navigation.DeeplinkRedirector
import com.blockchain.deeplinking.processor.DeepLinkResult
import com.blockchain.deeplinking.processor.DeeplinkService
import com.blockchain.deeplinking.processor.LinkState
import com.blockchain.deeplinking.processor.OpenBankingLinkType
import com.blockchain.domain.buy.CancelOrderService
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.model.BankAuthDeepLinkState
import com.blockchain.domain.paymentmethods.model.BankAuthFlowState
import com.blockchain.domain.paymentmethods.model.BankBuyAuthStep
import com.blockchain.domain.paymentmethods.model.BankBuyNavigation
import com.blockchain.domain.paymentmethods.model.BankLinkingInfo
import com.blockchain.domain.paymentmethods.model.BankPaymentApproval
import com.blockchain.domain.paymentmethods.model.BankTransferDetails
import com.blockchain.domain.paymentmethods.model.BankTransferStatus
import com.blockchain.domain.paymentmethods.model.fromPreferencesValue
import com.blockchain.domain.paymentmethods.model.toPreferencesValue
import com.blockchain.nabu.UserIdentity
import com.blockchain.network.PollResult
import com.blockchain.network.PollService
import com.blockchain.preferences.BankLinkingPrefs
import com.blockchain.walletconnect.domain.WalletConnectV2Service
import com.blockchain.walletconnect.domain.WalletConnectV2UrlValidator
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.NoSuchElementException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.awaitSingle
import timber.log.Timber

class DeeplinkNavigationHandler(
    private val userIdentity: UserIdentity,
    private val deeplinkRedirector: DeeplinkRedirector,
    private val deeplinkService: DeeplinkService,
    private val cancelOrderUseCase: CancelOrderService,
    private val bankService: BankService,
    private val bankBuyNavigation: BankBuyNavigation,
    private val bankLinkingPrefs: BankLinkingPrefs,
    private val walletConnectV2Service: WalletConnectV2Service,
    private val walletConnectV2UrlValidator: WalletConnectV2UrlValidator
) : ViewModel() {
    private val _step: MutableSharedFlow<DeeplinkNavigationStep> = MutableSharedFlow()
    private val compositeDisposable = CompositeDisposable()

    val step: Flow<DeeplinkNavigationStep>
        get() = _step.distinctUntilChanged()

    suspend fun checkDeeplinkDestination(intent: Intent) {
        val walletLinkError = try {
            userIdentity.userLinkedError().awaitSingle()
        } catch (e: NoSuchElementException) {
            null
        } catch (d: Exception) {
            return
        }

        if (walletLinkError == null) {
            try {
                val linkState = processDeeplink(intent).awaitSingle()
                dispatchDeepLink(linkState)
            } catch (e: Exception) {
                return
            }
        } else {
            _step.emit(DeeplinkNavigationStep.AccountWalletLinkAlert(walletLinkError.linkError))
        }
    }

    private fun processDeeplink(intent: Intent): Maybe<LinkState> {
        return (
            intent.data.takeIf {
                it != Uri.EMPTY &&
                    (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0
            }?.let {

                // A WalletConnect session pair deeplink must be handled by the WalletConnect service directly
                if (walletConnectV2UrlValidator.validateURI(it.toString())) {
                    viewModelScope.launch {
                        walletConnectV2Service.pair(it.toString())
                    }
                    Maybe.empty()
                } else {
                    deeplinkRedirector.processDeeplinkURL(it).toMaybe()
                        .flatMap { result ->
                            if (result is DeepLinkResult.DeepLinkResultUnknownLink) {
                                deeplinkService.getLink(intent)
                            } else Maybe.empty()
                        }
                }
            } ?: Maybe.empty()
            )
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.dispose()
    }

    private fun dispatchDeepLink(linkState: LinkState) {
        when (linkState) {
            is LinkState.EmailVerifiedDeepLink -> {
                // no-op - keeping the event for email verification
            }
            is LinkState.KycDeepLink -> {}
            is LinkState.OpenBankingLink -> handleOpenBankingDeepLink(linkState)
            is LinkState.BlockchainLink -> {
            }
            else -> {
                // do nothing
            }
        }
    }

    private fun handleOpenBankingDeepLink(state: LinkState.OpenBankingLink) =
        when (state.type) {
            OpenBankingLinkType.LINK_BANK -> handleBankLinking(state.consentToken)
            OpenBankingLinkType.PAYMENT_APPROVAL -> handleBankApproval(state.consentToken)
            OpenBankingLinkType.UNKNOWN -> {
                emit(DeeplinkNavigationStep.OpenBankingError)
            }
        }

    private fun handleBankApproval(consentToken: String?) {
        val deepLinkState = bankLinkingPrefs.getBankLinkingState().fromPreferencesValue() ?: BankAuthDeepLinkState()

        if (deepLinkState.bankAuthFlow == BankAuthFlowState.BANK_APPROVAL_COMPLETE) {
            resetLocalBankAuthState()
            return
        }

        consentToken?.let { token ->
            compositeDisposable += updateOpenBankingConsent(token)
                .subscribeBy(
                    onComplete = {
                        if (deepLinkState.bankAuthFlow == BankAuthFlowState.BANK_APPROVAL_PENDING) {
                            deepLinkState.bankPaymentData?.let { paymentData ->
                                handleDepositApproval(paymentData, deepLinkState)
                            } ?: handleSimpleBuyApproval()
                        }
                    },
                    onError = {
                        Timber.e("Error updating consent token on approval: $it")
                        resetLocalBankAuthState()
                        deepLinkState.bankPaymentData?.let { data ->
                            emit(DeeplinkNavigationStep.OpenBankingErrorWithCurrency(data.orderValue.currency))
                        } ?: run {
                            emit(DeeplinkNavigationStep.OpenBankingBuyApprovalError)
                        }
                    }
                )
        } ?: run {
            deepLinkState.bankPaymentData?.let {
                compositeDisposable += cancelOrderUseCase.cancelOrder(it.paymentId).subscribeBy(
                    onComplete = {
                        emit(DeeplinkNavigationStep.OpenBankingErrorWithCurrency(it.orderValue.currency))
                    }
                )
            } ?: run {
                emit(DeeplinkNavigationStep.OpenBankingBuyApprovalError)
            }
        }
    }

    private fun handleSimpleBuyApproval() {
        compositeDisposable += bankBuyNavigation.step().subscribeBy {
            when (it) {
                is BankBuyAuthStep.BankAuthForCancelledOrder -> emit(
                    DeeplinkNavigationStep.PaymentForCancelledOrder(it.fiatCurrency)
                )
                BankBuyAuthStep.BuyWithBankApproved -> emit(
                    DeeplinkNavigationStep.SimpleBuyFromDeepLinkApproval
                )
                BankBuyAuthStep.BuyWithBankError -> emit(
                    DeeplinkNavigationStep.OpenBankingError
                )
            }
        }
    }

    private fun emit(navigationStep: DeeplinkNavigationStep) {
        viewModelScope.launch {
            _step.emit(navigationStep)
        }
    }

    private fun handleDepositApproval(paymentData: BankPaymentApproval, deepLinkState: BankAuthDeepLinkState) {
        compositeDisposable += PollService(
            bankService.getBankTransferCharge(paymentData.paymentId)
        ) { transferDetails ->
            transferDetails.status != BankTransferStatus.Pending
        }.start()
            .doOnSubscribe {
                emit(DeeplinkNavigationStep.OpenBankingApprovalDepositInProgress(paymentData.orderValue))
            }.subscribeBy(
                onSuccess = {
                    when (it) {
                        is PollResult.FinalResult -> {
                            bankLinkingPrefs.setBankLinkingState(
                                deepLinkState.copy(
                                    bankAuthFlow = BankAuthFlowState.BANK_APPROVAL_COMPLETE,
                                    bankPaymentData = null,
                                    bankLinkingInfo = null
                                ).toPreferencesValue()
                            )

                            handleTransferStatus(it.value, paymentData)
                        }
                        is PollResult.TimeOut -> {
                            viewModelScope.launch {
                                _step.emit(
                                    DeeplinkNavigationStep.OpenBankingApprovalTimeout(paymentData.orderValue.currency)
                                )
                            }

                            bankLinkingPrefs.setBankLinkingState(
                                deepLinkState.copy(
                                    bankAuthFlow = BankAuthFlowState.BANK_APPROVAL_COMPLETE,
                                    bankPaymentData = null,
                                    bankLinkingInfo = null
                                ).toPreferencesValue()
                            )
                        }
                        is PollResult.Cancel -> {
                            // do nothing
                        }
                        else -> {
                            // do nothing
                        }
                    }
                },
                onError = {
                    bankLinkingPrefs.setBankLinkingState(
                        BankAuthDeepLinkState(
                            bankAuthFlow = BankAuthFlowState.NONE,
                            bankPaymentData = null,
                            bankLinkingInfo = null
                        )
                            .toPreferencesValue()
                    )
                    viewModelScope.launch {
                        _step.emit(
                            DeeplinkNavigationStep.OpenBankingErrorWithCurrency(paymentData.orderValue.currency)
                        )
                    }
                }
            )
    }

    private fun handleTransferStatus(
        it: BankTransferDetails,
        paymentData: BankPaymentApproval
    ) {
        when (it.status) {
            BankTransferStatus.Complete -> {
                emit(
                    DeeplinkNavigationStep.OpenBankingApprovalDepositComplete(
                        it.amount,
                        depositCompleteTime()
                    )
                )
            }
            BankTransferStatus.Pending -> {
                emit(
                    DeeplinkNavigationStep.OpenBankingApprovalTimeout(
                        paymentData.orderValue.currency
                    )
                )
            }
            is BankTransferStatus.Error,
            BankTransferStatus.Unknown -> {
                emit(
                    DeeplinkNavigationStep.OpenBankingErrorWithCurrency(
                        paymentData.orderValue.currency
                    )
                )
            }
        }
    }

    private fun handleBankLinking(consentToken: String?) {
        val bankLinkingState = bankLinkingPrefs.getBankLinkingState().fromPreferencesValue() ?: BankAuthDeepLinkState()

        if (bankLinkingState.bankAuthFlow == BankAuthFlowState.BANK_LINK_COMPLETE) {
            resetLocalBankAuthState()
            return
        }

        consentToken?.let { token ->
            compositeDisposable += updateOpenBankingConsent(token)
                .subscribeBy(
                    onComplete = {
                        try {
                            updateBankLinkingState(
                                bankLinkingState.copy(bankAuthFlow = BankAuthFlowState.BANK_LINK_COMPLETE)
                            )

                            bankLinkingState.bankLinkingInfo?.let {
                                emit(DeeplinkNavigationStep.OpenBankingLinking(it))
                            }
                        } catch (e: Exception) {
                            emit(DeeplinkNavigationStep.OpenBankingError)
                        }
                    },
                    onError = {
                        Timber.e("Error updating consent token on new bank link: $it")
                        bankLinkingState.bankLinkingInfo?.let { linkingInfo ->
                            emit(DeeplinkNavigationStep.OpenBankingLinking(linkingInfo))
                        } ?: run {
                            emit(DeeplinkNavigationStep.OpenBankingError)
                        }
                    }
                )
        } ?: run {
            Timber.e("Error updating consent token on new bank link: token is null.")
            bankLinkingState.bankLinkingInfo?.let { linkingInfo ->
                emit(DeeplinkNavigationStep.OpenBankingLinking(linkingInfo))
            } ?: run {
                emit(DeeplinkNavigationStep.OpenBankingError)
            }
        }
    }

    private fun updateBankLinkingState(bankLinkingState: BankAuthDeepLinkState) {
        bankLinkingPrefs.setBankLinkingState(bankLinkingState.toPreferencesValue())
    }

    private fun updateOpenBankingConsent(token: String): Completable {
        return bankService.updateOpenBankingConsent(
            bankLinkingPrefs.getDynamicOneTimeTokenUrl(),
            token
        ).doOnError {
            resetLocalBankAuthState()
        }
    }

    private fun resetLocalBankAuthState() {
        bankLinkingPrefs.setBankLinkingState(
            BankAuthDeepLinkState(bankAuthFlow = BankAuthFlowState.NONE, bankPaymentData = null, bankLinkingInfo = null)
                .toPreferencesValue()
        )
    }

    private fun depositCompleteTime(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 3)
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(cal.time)
    }
}

sealed class DeeplinkNavigationStep {
    class AccountWalletLinkAlert(val walletIdHint: String) : DeeplinkNavigationStep()
    class OpenBankingErrorWithCurrency(val currency: FiatCurrency) : DeeplinkNavigationStep()
    object OpenBankingError : DeeplinkNavigationStep()
    object OpenBankingBuyApprovalError : DeeplinkNavigationStep()
    class OpenBankingApprovalDepositInProgress(val orderValue: Money) : DeeplinkNavigationStep()
    class OpenBankingApprovalTimeout(val currency: FiatCurrency) : DeeplinkNavigationStep()
    class OpenBankingApprovalDepositComplete(val amount: Money, val estimationTime: String) :
        DeeplinkNavigationStep()

    class OpenBankingLinking(val bankLinkingInfo: BankLinkingInfo) : DeeplinkNavigationStep()
    object SimpleBuyFromDeepLinkApproval : DeeplinkNavigationStep()
    class PaymentForCancelledOrder(val currency: FiatCurrency) : DeeplinkNavigationStep()
}
