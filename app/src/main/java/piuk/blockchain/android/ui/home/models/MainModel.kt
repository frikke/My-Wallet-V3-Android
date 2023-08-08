package piuk.blockchain.android.ui.home.models

import android.content.Intent
import android.net.Uri
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.api.NabuApiException
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.TransactionTarget
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.deeplinking.processor.BlockchainLinkState
import com.blockchain.deeplinking.processor.DeepLinkResult
import com.blockchain.deeplinking.processor.KycLinkState
import com.blockchain.deeplinking.processor.LinkState
import com.blockchain.deeplinking.processor.OpenBankingLinkType
import com.blockchain.domain.paymentmethods.model.BankAuthDeepLinkState
import com.blockchain.domain.paymentmethods.model.BankAuthFlowState
import com.blockchain.domain.paymentmethods.model.BankPaymentApproval
import com.blockchain.domain.paymentmethods.model.BankTransferDetails
import com.blockchain.domain.paymentmethods.model.BankTransferStatus
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.extensions.exhaustive
import com.blockchain.home.presentation.navigation.ScanResult
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.models.responses.nabu.CampaignData
import com.blockchain.network.PollResult
import com.blockchain.utils.emptySubscribe
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import com.blockchain.walletconnect.domain.WalletConnectSession
import com.blockchain.walletconnect.domain.WalletConnectSessionEvent
import com.blockchain.walletconnect.ui.networks.NetworkInfo
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.serialization.SerializationException
import piuk.blockchain.android.scan.QrScanError
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.ui.kyc.navhost.models.KycEntryPoint
import timber.log.Timber

class MainModel(
    initialState: MainState,
    mainScheduler: Scheduler,
    private val interactor: MainInteractor,
    private val walletConnectServiceAPI: WalletConnectServiceAPI,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger
) : MviModel<MainState, MainIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    remoteLogger
) {

    private val compositeDisposable = CompositeDisposable()

    fun clearDisposables() = compositeDisposable.clear()

    init {
        compositeDisposable += walletConnectServiceAPI.sessionEvents.subscribeBy { sessionEvent ->
            when (sessionEvent) {
                is WalletConnectSessionEvent.ReadyForApproval -> process(
                    MainIntent.GetNetworkInfoForWCSession(sessionEvent.session)
                )
                is WalletConnectSessionEvent.DidConnect -> process(
                    MainIntent.UpdateViewToLaunch(ViewToLaunch.LaunchWalletConnectSessionApproved(sessionEvent.session))
                )
                is WalletConnectSessionEvent.FailToConnect,
                is WalletConnectSessionEvent.DidReject -> process(
                    MainIntent.UpdateViewToLaunch(ViewToLaunch.LaunchWalletConnectSessionRejected(sessionEvent.session))
                )
                is WalletConnectSessionEvent.DidDisconnect -> {
                    Timber.i("Session ${sessionEvent.session.url} Disconnected")
                }
            }.exhaustive
        }
    }

    override fun performAction(previousState: MainState, intent: MainIntent): Disposable? =
        when (intent) {
            is MainIntent.PerformInitialChecks -> {
                interactor.checkForUserWalletErrors().subscribeBy(
                    onComplete = {
                        process(MainIntent.ProcessPendingDeeplinkIntent(intent.deeplinkIntent))
                    },
                    onError = { throwable ->
                        if (throwable is NabuApiException && throwable.isUserWalletLinkError()) {
                            process(
                                MainIntent.UpdateViewToLaunch(
                                    ViewToLaunch.CheckForAccountWalletLinkErrors(throwable.getWalletIdHint())
                                )
                            )
                        }
                    }
                )
            }
            is MainIntent.ProcessPendingDeeplinkIntent -> {
                intent.deeplinkIntent.data.takeIf { it != Uri.EMPTY }?.let { uri ->
                    interactor.processDeepLinkV2(uri).subscribeBy(
                        onSuccess = {
                            if (it is DeepLinkResult.DeepLinkResultUnknownLink) {
                                process(MainIntent.CheckForPendingLinks(intent.deeplinkIntent))
                            }
                        },
                        onError = {
                            // fail silently
                            Timber.e(it)
                        }
                    )
                }
            }
            is MainIntent.CheckReferralCode -> {
                interactor.checkReferral()
                    .onErrorReturn { ReferralState(ReferralInfo.NotAvailable) }
                    .subscribeBy { referralState ->
                        if (previousState.referral.referralDeeplink) {
                            process(MainIntent.UpdateViewToLaunch(ViewToLaunch.ShowReferralSheet))
                        }
                        process(MainIntent.ReferralCodeIntent(referralState))
                    }
            }
            is MainIntent.ReferralIconClicked -> {
                interactor.storeReferralClicked()
                null
            }
            is MainIntent.CheckForPendingLinks -> {
                interactor.checkForDeepLinks(intent.appIntent)
                    .subscribeBy(
                        onSuccess = { linkState ->
                            if ((
                                intent.appIntent.flags
                                    and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
                                ) == 0
                            ) {
                                dispatchDeepLink(linkState)
                            }
                        },
                        onComplete = {
                            // do nothing
                        },
                        onError = {
                            Timber.e(it)
                        }
                    )
            }
            is MainIntent.ClearDeepLinkResult -> interactor.clearDeepLink()
                .onErrorComplete()
                .subscribe()
            is MainIntent.UnpairWallet -> interactor.unpairWallet()
                .onErrorComplete()
                .subscribe()
            is MainIntent.ProcessScanResult -> interactor.processQrScanResult(intent.decodedData)
                .subscribeBy(
                    onSuccess = {
                        when (it) {
                            is ScanResult.HttpUri -> handlePossibleDeepLinkFromScan(it)
                            is ScanResult.TxTarget -> {
                                process(
                                    MainIntent.UpdateViewToLaunch(
                                        ViewToLaunch.LaunchTransactionFlowWithTargets(it.targets)
                                    )
                                )
                            }
                            is ScanResult.WalletConnectRequest -> walletConnectServiceAPI.attemptToConnect(it.data)
                                .emptySubscribe()
                            is ScanResult.WalletConnectV2Request,
                            is ScanResult.ImportedWallet -> {
                                // TODO: as part of Auth
                            }
                            is ScanResult.SecuredChannelLogin -> interactor.sendSecureChannelHandshake(it.handshake)
                        }
                    },
                    onError = {
                        when (it) {
                            is QrScanError -> process(
                                MainIntent.UpdateViewToLaunch(ViewToLaunch.ShowTargetScanError(it))
                            )
                            else -> {
                                Timber.d("Scan failed")
                            }
                        }
                    }
                )
            is MainIntent.ApproveWCSession -> walletConnectServiceAPI.acceptConnection(intent.session).emptySubscribe()
            is MainIntent.RejectWCSession -> walletConnectServiceAPI.denyConnection(intent.session).emptySubscribe()
            is MainIntent.StartWCSession -> walletConnectServiceAPI.attemptToConnect(intent.url).emptySubscribe()
            is MainIntent.GetNetworkInfoForWCSession -> getNetworkInfoForWCSession(intent.session)
            is MainIntent.LoadFeatureFlags ->
                interactor.isEarnOnNavBarEnabled().subscribeBy(
                    onSuccess = { earnEnabled ->
                        process(MainIntent.UpdateFlags(earnEnabled))
                    },
                    onError = {
                        process(MainIntent.UpdateFlags(isEarnEnabled = false))
                    }
                )
            is MainIntent.LaunchTransactionFlowFromDeepLink ->
                // the interest deposit flow requires that there are defined source and target accounts before launch
                if (intent.action == AssetAction.InterestDeposit) {
                    Singles.zip(
                        interactor.selectAccountForTxFlow(intent.networkTicker, intent.action),
                        interactor.selectRewardsAccountForAsset(intent.networkTicker)
                    ).map { (sourceAccount, targetAccount) ->
                        require(sourceAccount is LaunchFlowForAccount.SourceAccount)
                        require(targetAccount is LaunchFlowForAccount.SourceAccount)

                        LaunchFlowForAccount.SourceAndTargetAccount(
                            sourceAccount = sourceAccount.source,
                            targetAccount = targetAccount.source as TransactionTarget
                        )
                    }
                } else {
                    interactor.selectAccountForTxFlow(intent.networkTicker, intent.action)
                }
                    .subscribeBy(
                        onSuccess = { account ->
                            process(
                                MainIntent.UpdateViewToLaunch(
                                    ViewToLaunch.LaunchTxFlowWithAccountForAction(account, intent.action)
                                )
                            )
                        },
                        onError = {
                            Timber.e(
                                "Error getting default account for TxFlow ${intent.action} deeplink ${it.message}"
                            )

                            process(
                                MainIntent.UpdateViewToLaunch(
                                    ViewToLaunch.LaunchTxFlowWithAccountForAction(
                                        LaunchFlowForAccount.NoAccount,
                                        intent.action
                                    )
                                )
                            )
                        }
                    )
            is MainIntent.SelectRewardsAccountForAsset ->
                interactor.selectRewardsAccountForAsset(intent.cryptoTicker)
                    .subscribeBy(
                        onSuccess = { account ->
                            process(
                                MainIntent.UpdateViewToLaunch(
                                    ViewToLaunch.LaunchRewardsSummaryFromDeepLink(account)
                                )
                            )
                        },
                        onError = {
                            Timber.e(
                                "Error getting default account for Rewards Summary ${it.message}"
                            )

                            process(
                                MainIntent.UpdateViewToLaunch(
                                    ViewToLaunch.LaunchRewardsSummaryFromDeepLink(LaunchFlowForAccount.NoAccount)
                                )
                            )
                        }
                    )
            else -> null
        }

    private fun handlePossibleDeepLinkFromScan(scanResult: ScanResult.HttpUri) {
        compositeDisposable += interactor.checkForDeepLinks(scanResult)
            .subscribeBy(
                onSuccess = {
                    dispatchDeepLink(it)
                },
                onError = { Timber.e(it) }
            )
    }

    private fun dispatchDeepLink(linkState: LinkState) {
        when (linkState) {
            is LinkState.EmailVerifiedDeepLink -> {
                // no-op - keeping the event for email verification
            }
            is LinkState.KycDeepLink -> handleKycDeepLink(linkState)
            is LinkState.OpenBankingLink -> handleOpenBankingDeepLink(linkState)
            is LinkState.BlockchainLink -> handleBlockchainDeepLink(linkState)
            else -> {
                // do nothing
            }
        }
    }

    private fun handleBlockchainDeepLink(linkState: LinkState.BlockchainLink) {
        when (val link = linkState.link) {
            BlockchainLinkState.NoUri -> Timber.e("Invalid deep link")
            BlockchainLinkState.TwoFa -> process(MainIntent.UpdateViewToLaunch(ViewToLaunch.LaunchTwoFaSetup))
            BlockchainLinkState.VerifyEmail -> process(
                MainIntent.UpdateViewToLaunch(ViewToLaunch.LaunchVerifyEmail)
            )
            BlockchainLinkState.SetupFingerprint -> process(
                MainIntent.UpdateViewToLaunch(ViewToLaunch.LaunchSetupBiometricLogin)
            )
            BlockchainLinkState.Interest -> process(
                MainIntent.UpdateViewToLaunch(ViewToLaunch.LaunchInterestDashboard(LaunchOrigin.DEEPLINK))
            )
            is BlockchainLinkState.SimpleBuy -> process(
                MainIntent.UpdateViewToLaunch(
                    ViewToLaunch.LaunchSimpleBuy(
                        interactor.getAssetFromTicker(link.ticker) ?: throw IllegalStateException(
                            "Unknown asset ticker ${link.ticker}"
                        )
                    )
                )
            )
            is BlockchainLinkState.KycCampaign -> {
                val entryPoint = when {
                    link.campaignType.equals("SimpleBuy", ignoreCase = true) -> KycEntryPoint.Buy
                    else -> KycEntryPoint.Other
                }
                process(MainIntent.UpdateViewToLaunch(ViewToLaunch.LaunchKyc(entryPoint)))
            }
            else -> {
            }
        }
    }

    private fun handleKycDeepLink(linkState: LinkState.KycDeepLink) {
        when (linkState.link) {
            is KycLinkState.Resubmit -> process(
                MainIntent.UpdateViewToLaunch(ViewToLaunch.LaunchKyc(KycEntryPoint.Resubmission))
            )
            is KycLinkState.EmailVerified -> process(
                MainIntent.UpdateViewToLaunch(ViewToLaunch.LaunchKyc(KycEntryPoint.Other))
            )
            is KycLinkState.General -> {
                val data = (linkState.link as KycLinkState.General).campaignData
                if (data != null) {
                    registerForCampaign(data)
                } else {
                    process(
                        MainIntent.UpdateViewToLaunch(ViewToLaunch.LaunchKyc(KycEntryPoint.Other))
                    )
                }
            }
            else -> {
                // do nothing
            }
        }
    }

    private fun registerForCampaign(campaignData: CampaignData) {
        // Future campaigns register here
    }

    private fun handleOpenBankingDeepLink(state: LinkState.OpenBankingLink) =
        when (state.type) {
            OpenBankingLinkType.LINK_BANK -> handleBankLinking(state.consentToken)
            OpenBankingLinkType.PAYMENT_APPROVAL -> handleBankApproval(state.consentToken)
            OpenBankingLinkType.UNKNOWN -> process(MainIntent.UpdateViewToLaunch(ViewToLaunch.ShowOpenBankingError))
        }

    private fun handleBankLinking(consentToken: String?) {
        val bankLinkingState = interactor.getBankLinkingState()

        if (bankLinkingState.bankAuthFlow == BankAuthFlowState.BANK_LINK_COMPLETE) {
            interactor.resetLocalBankAuthState()
            return
        }

        consentToken?.let { token ->
            compositeDisposable += interactor.updateOpenBankingConsent(token)
                .subscribeBy(
                    onComplete = {
                        try {
                            interactor.updateBankLinkingState(
                                bankLinkingState.copy(bankAuthFlow = BankAuthFlowState.BANK_LINK_COMPLETE)
                            )

                            bankLinkingState.bankLinkingInfo?.let {
                                process(MainIntent.UpdateViewToLaunch(ViewToLaunch.LaunchOpenBankingLinking(it)))
                            }
                        } catch (e: SerializationException) {
                            process(MainIntent.UpdateViewToLaunch(ViewToLaunch.ShowOpenBankingError))
                        }
                    },
                    onError = {
                        Timber.e("Error updating consent token on new bank link: $it")
                        bankLinkingState.bankLinkingInfo?.let { linkingInfo ->
                            process(MainIntent.UpdateViewToLaunch(ViewToLaunch.LaunchOpenBankingLinking(linkingInfo)))
                        } ?: process(MainIntent.UpdateViewToLaunch(ViewToLaunch.ShowOpenBankingError))
                    }
                )
        } ?: run {
            Timber.e("Error updating consent token on new bank link: token is null.")
            bankLinkingState.bankLinkingInfo?.let { linkingInfo ->
                process(MainIntent.UpdateViewToLaunch(ViewToLaunch.LaunchOpenBankingLinking(linkingInfo)))
            } ?: process(MainIntent.UpdateViewToLaunch(ViewToLaunch.ShowOpenBankingError))
        }
    }

    private fun handleBankApproval(consentToken: String?) {
        val deepLinkState = interactor.getBankLinkingState()

        if (deepLinkState.bankAuthFlow == BankAuthFlowState.BANK_APPROVAL_COMPLETE) {
            interactor.resetLocalBankAuthState()
            return
        }

        consentToken?.let { token ->
            compositeDisposable += interactor.updateOpenBankingConsent(token)
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

                        interactor.resetLocalBankAuthState()

                        deepLinkState.bankPaymentData?.let { data ->
                            process(
                                MainIntent.UpdateViewToLaunch(
                                    ViewToLaunch.LaunchOpenBankingError(data.orderValue.currencyCode)
                                )
                            )
                        } ?: process(MainIntent.UpdateViewToLaunch(ViewToLaunch.LaunchOpenBankingBuyApprovalError))
                    }
                )
        } ?: run {
            deepLinkState.bankPaymentData?.let {
                compositeDisposable += interactor.cancelOrder(it.paymentId).subscribeBy(
                    onComplete = {
                        process(
                            MainIntent.UpdateViewToLaunch(
                                ViewToLaunch.LaunchOpenBankingError(it.orderValue.currencyCode)
                            )
                        )
                    }
                )
            } ?: run {
                process(MainIntent.UpdateViewToLaunch(ViewToLaunch.LaunchOpenBankingBuyApprovalError))
            }
        }
    }

    private fun handleDepositApproval(
        paymentData: BankPaymentApproval,
        deepLinkState: BankAuthDeepLinkState
    ) {
        compositeDisposable += interactor.pollForBankTransferCharge(paymentData)
            .doOnSubscribe {
                process(
                    MainIntent.UpdateViewToLaunch(
                        ViewToLaunch.LaunchOpenBankingApprovalDepositInProgress(paymentData.orderValue)
                    )
                )
            }.subscribeBy(
                onSuccess = {
                    when (it) {
                        is PollResult.FinalResult -> {
                            interactor.updateBankLinkingState(
                                deepLinkState.copy(
                                    bankAuthFlow = BankAuthFlowState.BANK_APPROVAL_COMPLETE,
                                    bankPaymentData = null,
                                    bankLinkingInfo = null
                                )
                            )

                            handleTransferStatus(it.value, paymentData)
                        }
                        is PollResult.TimeOut -> {
                            process(
                                MainIntent.UpdateViewToLaunch(
                                    ViewToLaunch.LaunchOpenBankingApprovalTimeout(paymentData.orderValue.currencyCode)
                                )
                            )
                            interactor.updateBankLinkingState(
                                deepLinkState.copy(
                                    bankAuthFlow = BankAuthFlowState.BANK_APPROVAL_COMPLETE,
                                    bankPaymentData = null,
                                    bankLinkingInfo = null
                                )
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
                onError = { error ->
                    interactor.resetLocalBankAuthState()
                    (error as? NabuApiException)?.getServerSideErrorInfo()?.let { info ->
                        process(
                            MainIntent.UpdateViewToLaunch(
                                ViewToLaunch.LaunchServerDrivenOpenBankingError(
                                    currencyCode = paymentData.orderValue.currencyCode,
                                    title = info.title,
                                    description = info.description
                                )
                            )
                        )
                    } ?: process(
                        MainIntent.UpdateViewToLaunch(
                            ViewToLaunch.LaunchOpenBankingError(paymentData.orderValue.currencyCode)
                        )
                    )
                }
            )
    }

    private fun handleTransferStatus(
        it: BankTransferDetails,
        paymentData: BankPaymentApproval
    ) {
        when (it.status) {
            BankTransferStatus.Complete -> {
                process(
                    MainIntent.UpdateViewToLaunch(
                        ViewToLaunch.LaunchOpenBankingApprovalDepositComplete(
                            it.amount,
                            interactor.getEstimatedDepositCompletionTime()
                        )
                    )
                )
            }
            BankTransferStatus.Pending -> {
                process(
                    MainIntent.UpdateViewToLaunch(
                        ViewToLaunch.LaunchOpenBankingApprovalTimeout(paymentData.orderValue.currencyCode)
                    )
                )
            }
            is BankTransferStatus.Error,
            BankTransferStatus.Unknown -> {
                process(
                    MainIntent.UpdateViewToLaunch(
                        ViewToLaunch.LaunchOpenBankingError(paymentData.orderValue.currencyCode)
                    )
                )
            }
        }
    }

    private fun handleSimpleBuyApproval() {
        interactor.getSimpleBuySyncLocalState()?.let {
            handleOrderState(it)
        } ?: kotlin.run {
            // try to sync with server once, otherwise fail
            compositeDisposable += interactor.performSimpleBuySync()
                .subscribeBy(
                    onComplete = {
                        interactor.getSimpleBuySyncLocalState()?.let {
                            handleOrderState(it)
                        } ?: process(MainIntent.UpdateViewToLaunch(ViewToLaunch.LaunchOpenBankingBuyApprovalError))
                    },
                    onError = {
                        Timber.e("Error doing SB sync for bank linking $it")
                        interactor.resetLocalBankAuthState()
                        process(MainIntent.UpdateViewToLaunch(ViewToLaunch.LaunchOpenBankingBuyApprovalError))
                    }
                )
        }
    }

    private fun getNetworkInfoForWCSession(session: WalletConnectSession) =
        interactor.getSupportedEvmNetworks().subscribeBy(
            onError = {
                Timber.e(it)
                process(
                    MainIntent.UpdateViewToLaunch(
                        ViewToLaunch.LaunchWalletConnectSessionApproval(
                            session
                        )
                    )
                )
            },
            onSuccess = { networks ->
                val network = networks.find { it.chainId == session.dAppInfo.chainId }
                network?.let {
                    val networkInfo = NetworkInfo(
                        networkTicker = network.networkTicker,
                        name = network.name,
                        chainId = network.chainId!!,
                        logo = interactor.getAssetFromTicker(network.networkTicker)?.logo
                    )
                    process(
                        MainIntent.UpdateViewToLaunch(
                            ViewToLaunch.LaunchWalletConnectSessionApprovalWithNetwork(
                                session,
                                networkInfo
                            )
                        )
                    )
                } ?: process(
                    MainIntent.UpdateViewToLaunch(
                        ViewToLaunch.LaunchWalletConnectSessionApproval(
                            session
                        )
                    )
                )
            }
        )

    private fun handleOrderState(state: SimpleBuyState) {
        if (state.orderState == OrderState.AWAITING_FUNDS) {
            process(MainIntent.UpdateViewToLaunch(ViewToLaunch.LaunchSimpleBuyFromDeepLinkApproval))
        } else {
            interactor.resetLocalBankAuthState()
            process(MainIntent.UpdateViewToLaunch(ViewToLaunch.LaunchPaymentForCancelledOrder(state)))
        }
    }

    private fun NabuApiException.getWalletIdHint(): String =
        getErrorDescription().split(NabuApiException.USER_WALLET_LINK_ERROR_PREFIX).last()
}
