package piuk.blockchain.android.ui.home.v2

import android.content.Intent
import com.blockchain.banking.BankPaymentApproval
import com.blockchain.coincore.AssetAction
import com.blockchain.extensions.valueOf
import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.models.data.BankTransferDetails
import com.blockchain.nabu.models.data.BankTransferStatus
import com.blockchain.nabu.models.responses.nabu.CampaignData
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.nabu.models.responses.nabu.NabuApiException
import com.blockchain.network.PollResult
import com.blockchain.notifications.analytics.LaunchOrigin
import com.blockchain.utils.capitalizeFirstChar
import com.google.gson.JsonSyntaxException
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.deeplink.BlockchainLinkState
import piuk.blockchain.android.deeplink.EmailVerifiedLinkState
import piuk.blockchain.android.deeplink.LinkState
import piuk.blockchain.android.deeplink.OpenBankingLinkType
import piuk.blockchain.android.kyc.KycLinkState
import piuk.blockchain.android.scan.QrScanError
import piuk.blockchain.android.scan.ScanResult
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.sunriver.CampaignLinkState
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.linkbank.BankAuthDeepLinkState
import piuk.blockchain.android.ui.linkbank.BankAuthFlowState
import piuk.blockchain.android.ui.sell.BuySellFragment
import piuk.blockchain.android.ui.upsell.KycUpgradePromptManager
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import timber.log.Timber

class RedesignModel(
    initialState: RedesignState,
    mainScheduler: Scheduler,
    private val interactor: RedesignInteractor,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<RedesignState, RedesignIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    crashLogger
) {

    private val compositeDisposable = CompositeDisposable()

    fun clearDisposables() = compositeDisposable.clear()

    override fun performAction(previousState: RedesignState, intent: RedesignIntent): Disposable? =
        when (intent) {
            is RedesignIntent.PerformInitialChecks -> {
                interactor.checkForUserWalletErrors()
                    .subscribeBy(
                        onComplete = {
                            // Nothing to do here
                        },
                        onError = { throwable ->
                            if (throwable is NabuApiException && throwable.isUserWalletLinkError()) {
                                process(
                                    RedesignIntent.UpdateViewToLaunch(
                                        ViewToLaunch.CheckForAccountWalletLinkErrors(throwable.getWalletIdHint())
                                    )
                                )
                            }
                        }
                    )
            }
            is RedesignIntent.CheckForPendingLinks -> {
                interactor.checkForDeepLinks(intent.appIntent)
                    .subscribeBy(
                        onSuccess = { linkState ->
                            if ((intent.appIntent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
                                dispatchDeepLink(linkState)
                            }
                        },
                        onError = { Timber.e(it) }
                    )
            }
            is RedesignIntent.ValidateAccountAction ->
                interactor.checkIfShouldUpsell(intent.action, intent.account)
                    .subscribeBy(
                        onSuccess = { upsell ->
                            if (upsell != KycUpgradePromptManager.Type.NONE) {
                                process(
                                    RedesignIntent.UpdateViewToLaunch(
                                        ViewToLaunch.LaunchUpsellAssetAction(upsell)
                                    )
                                )
                            } else {
                                process(
                                    RedesignIntent.UpdateViewToLaunch(
                                        ViewToLaunch.LaunchAssetAction(intent.action, intent.account)
                                    )
                                )
                            }
                        },
                        onError = {
                            Timber.e("Upsell manager failure")
                        }
                    )
            is RedesignIntent.UnpairWallet -> interactor.unpairWallet()
                .onErrorComplete()
                .subscribe()
            is RedesignIntent.CancelAnyPendingConfirmationBuy -> interactor.cancelAnyPendingConfirmationBuy()
                .subscribe()
            is RedesignIntent.ProcessScanResult -> interactor.processQrScanResult(intent.decodedData)
                .subscribeBy(
                    onSuccess = {
                        when (it) {
                            is ScanResult.HttpUri -> handlePossibleDeepLinkFromScan(it)
                            is ScanResult.TxTarget -> {
                                process(
                                    RedesignIntent.UpdateViewToLaunch(
                                        ViewToLaunch.LaunchTransactionFlowWithTargets(it.targets)
                                    )
                                )
                            }
                            is ScanResult.ImportedWallet -> {
                                // TODO: as part of Auth
                            }
                            is ScanResult.SecuredChannelLogin -> interactor.sendSecureChannelHandshake(it.handshake)
                        }
                    },
                    onError = {
                        when (it) {
                            is QrScanError -> process(
                                RedesignIntent.UpdateViewToLaunch(ViewToLaunch.ShowTargetScanError(it))
                            )
                            else -> {
                                Timber.d("Scan failed")
                            }
                        }
                    }
                )
            is RedesignIntent.LaunchExchange -> handleExchangeLaunchingFromLinkingState()
            else -> null
        }

    private fun handlePossibleDeepLinkFromScan(scanResult: ScanResult.HttpUri) {
        compositeDisposable += interactor.checkForDeepLinks(scanResult)
            .subscribeBy(
                onSuccess = { dispatchDeepLink(it) },
                onError = { Timber.e(it) }
            )
    }

    private fun dispatchDeepLink(linkState: LinkState) {
        when (linkState) {
            is LinkState.SunriverDeepLink -> handleSunriverDeepLink(linkState)
            is LinkState.EmailVerifiedDeepLink -> handleEmailVerifiedForExchangeLinking(linkState)
            is LinkState.KycDeepLink -> handleKycDeepLink(linkState)
            is LinkState.ThePitDeepLink ->
                process(RedesignIntent.UpdateViewToLaunch(ViewToLaunch.LaunchExchange(linkState.linkId)))
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
            BlockchainLinkState.Swap -> process(RedesignIntent.UpdateViewToLaunch(ViewToLaunch.LaunchSwap))
            BlockchainLinkState.TwoFa -> process(RedesignIntent.UpdateViewToLaunch(ViewToLaunch.LaunchTwoFaSetup))
            BlockchainLinkState.VerifyEmail -> process(
                RedesignIntent.UpdateViewToLaunch(ViewToLaunch.LaunchVerifyEmail)
            )
            BlockchainLinkState.SetupFingerprint -> process(
                RedesignIntent.UpdateViewToLaunch(ViewToLaunch.LaunchSetupBiometricLogin)
            )
            BlockchainLinkState.Interest -> process(
                RedesignIntent.UpdateViewToLaunch(ViewToLaunch.LaunchInterestDashboard(LaunchOrigin.DEEPLINK))
            )
            BlockchainLinkState.Receive -> process(RedesignIntent.UpdateViewToLaunch(ViewToLaunch.LaunchReceive))
            BlockchainLinkState.Send -> process(RedesignIntent.UpdateViewToLaunch(ViewToLaunch.LaunchSend))
            is BlockchainLinkState.Sell -> process(
                RedesignIntent.UpdateViewToLaunch(
                    ViewToLaunch.LaunchBuySell(
                        BuySellFragment.BuySellViewType.TYPE_SELL,
                        interactor.getAssetFromTicker(link.ticker)
                    )
                )
            )
            is BlockchainLinkState.Activities -> process(
                RedesignIntent.UpdateViewToLaunch(ViewToLaunch.LaunchAssetAction(AssetAction.ViewActivity, null))
            )
            is BlockchainLinkState.Buy -> process(
                RedesignIntent.UpdateViewToLaunch(
                    ViewToLaunch.LaunchBuySell(
                        BuySellFragment.BuySellViewType.TYPE_BUY,
                        interactor.getAssetFromTicker(link.ticker)
                    )
                )
            )
            is BlockchainLinkState.SimpleBuy -> process(
                RedesignIntent.UpdateViewToLaunch(
                    ViewToLaunch.LaunchSimpleBuy(
                        interactor.getAssetFromTicker(link.ticker) ?: throw IllegalStateException(
                            "Unknown asset ticker ${link.ticker}"
                        )
                    )
                )
            )
            is BlockchainLinkState.KycCampaign ->
                process(
                    RedesignIntent.UpdateViewToLaunch(
                        ViewToLaunch.LaunchKyc(
                            valueOf<CampaignType>(
                                link.campaignType.capitalizeFirstChar()
                            ) ?: CampaignType.None
                        )
                    )
                )
        }
    }

    private fun handleEmailVerifiedForExchangeLinking(linkState: LinkState.EmailVerifiedDeepLink) {
        if (linkState.link === EmailVerifiedLinkState.FromPitLinking) {
            compositeDisposable += handleExchangeLaunchingFromLinkingState()
        }
    }

    private fun handleExchangeLaunchingFromLinkingState() =
        interactor.getExchangeLinkingState()
            .subscribeBy(
                onSuccess = { isLinked ->
                    if (isLinked) {
                        process(RedesignIntent.UpdateViewToLaunch(ViewToLaunch.LaunchExchange()))
                    } else {
                        process(
                            RedesignIntent.UpdateViewToLaunch(
                                ViewToLaunch.LaunchExchange(interactor.getExchangeToWalletLinkId())
                            )
                        )
                    }
                },
                onError = {
                    Timber.e(it)
                }
            )

    private fun handleSunriverDeepLink(linkState: LinkState.SunriverDeepLink) {
        when (linkState.link) {
            is CampaignLinkState.WrongUri -> process(
                RedesignIntent.UpdateViewToLaunch(
                    ViewToLaunch.DisplayAlertDialog(
                        R.string.sunriver_invalid_url_title,
                        R.string.sunriver_invalid_url_message
                    )
                )
            )
            is CampaignLinkState.Data -> registerForCampaign(linkState.link.campaignData)
            else -> {
                // do nothing
            }
        }
    }

    private fun handleKycDeepLink(linkState: LinkState.KycDeepLink) {
        when (linkState.link) {
            is KycLinkState.Resubmit -> process(
                RedesignIntent.UpdateViewToLaunch(ViewToLaunch.LaunchKyc(CampaignType.Resubmission))
            )
            is KycLinkState.EmailVerified -> process(
                RedesignIntent.UpdateViewToLaunch(ViewToLaunch.LaunchKyc(CampaignType.None))
            )
            is KycLinkState.General -> {
                val data = linkState.link.campaignData
                if (data != null) {
                    registerForCampaign(data)
                } else {
                    process(
                        RedesignIntent.UpdateViewToLaunch(ViewToLaunch.LaunchKyc(CampaignType.None))
                    )
                }
            }
            else -> {
                // do nothing
            }
        }
    }

    private fun registerForCampaign(campaignData: CampaignData) {
        // keep the call to prompt users to KYC if they somehow come into the app wanting to register
        compositeDisposable += interactor.registerForCampaign(campaignData)
            .subscribeBy(
                onSuccess = { status ->
                    if (status != KycState.Verified) {
                        RedesignIntent.UpdateViewToLaunch(
                            ViewToLaunch.LaunchKyc(
                                CampaignType.Sunriver
                            )
                        )
                    }
                },
                onError = { throwable ->
                    Timber.e(throwable)

                    process(
                        RedesignIntent.UpdateViewToLaunch(
                            ViewToLaunch.DisplayAlertDialog(
                                R.string.sunriver_invalid_url_title,
                                R.string.sunriver_campaign_expired
                            )
                        )
                    )
                }
            )
    }

    private fun handleOpenBankingDeepLink(state: LinkState.OpenBankingLink) =
        when (state.type) {
            OpenBankingLinkType.LINK_BANK -> handleBankLinking(state.consentToken)
            OpenBankingLinkType.PAYMENT_APPROVAL -> handleBankApproval(state.consentToken)
            OpenBankingLinkType.UNKNOWN -> process(RedesignIntent.UpdateViewToLaunch(ViewToLaunch.ShowOpenBankingError))
        }

    private fun handleBankLinking(consentToken: String) {
        val bankLinkingState = interactor.getBankLinkingState()

        if (bankLinkingState.bankAuthFlow == BankAuthFlowState.BANK_LINK_COMPLETE) {
            interactor.resetLocalBankAuthState()
            return
        }

        compositeDisposable += interactor.updateOpenBankingConsent(consentToken)
            .subscribeBy(
                onComplete = {
                    try {
                        interactor.updateBankLinkingState(
                            bankLinkingState.copy(bankAuthFlow = BankAuthFlowState.BANK_LINK_COMPLETE)
                        )

                        bankLinkingState.bankLinkingInfo?.let {
                            process(RedesignIntent.UpdateViewToLaunch(ViewToLaunch.LaunchOpenBankingLinking(it)))
                        }
                    } catch (e: JsonSyntaxException) {
                        process(RedesignIntent.UpdateViewToLaunch(ViewToLaunch.ShowOpenBankingError))
                    }
                },
                onError = {
                    Timber.e("Error updating consent token on new bank link: $it")
                    bankLinkingState.bankLinkingInfo?.let {
                        process(RedesignIntent.UpdateViewToLaunch(ViewToLaunch.LaunchOpenBankingLinking(it)))
                    } ?: process(RedesignIntent.UpdateViewToLaunch(ViewToLaunch.ShowOpenBankingError))
                }
            )
    }

    private fun handleBankApproval(consentToken: String) {
        val deepLinkState = interactor.getBankLinkingState()

        if (deepLinkState.bankAuthFlow == BankAuthFlowState.BANK_APPROVAL_COMPLETE) {
            interactor.resetLocalBankAuthState()
            return
        }

        compositeDisposable += interactor.updateOpenBankingConsent(consentToken)
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
                            RedesignIntent.UpdateViewToLaunch(
                                ViewToLaunch.LaunchOpenBankingError(data.orderValue.currencyCode)
                            )
                        )
                    } ?: process(RedesignIntent.UpdateViewToLaunch(ViewToLaunch.LaunchOpenBankingBuyApprovalError))
                }
            )
    }

    private fun handleDepositApproval(
        paymentData: BankPaymentApproval,
        deepLinkState: BankAuthDeepLinkState
    ) {
        compositeDisposable += interactor.pollForBankTransferCharge(paymentData)
            .doOnSubscribe {
                process(
                    RedesignIntent.UpdateViewToLaunch(
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
                                RedesignIntent.UpdateViewToLaunch(
                                    ViewToLaunch.LaunchOpenBankingApprovalTimeout(paymentData.orderValue.currencyCode)
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
                onError = {
                    interactor.resetLocalBankAuthState()
                    process(
                        RedesignIntent.UpdateViewToLaunch(
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
            BankTransferStatus.COMPLETE -> {
                process(
                    RedesignIntent.UpdateViewToLaunch(
                        ViewToLaunch.LaunchOpenBankingApprovalDepositComplete(
                            it.amount, interactor.getEstimatedDepositCompletionTime()
                        )
                    )
                )
            }
            BankTransferStatus.PENDING -> {
                process(
                    RedesignIntent.UpdateViewToLaunch(
                        ViewToLaunch.LaunchOpenBankingApprovalTimeout(paymentData.orderValue.currencyCode)
                    )
                )
            }
            BankTransferStatus.ERROR,
            BankTransferStatus.UNKNOWN -> {
                process(
                    RedesignIntent.UpdateViewToLaunch(
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
                        } ?: process(RedesignIntent.UpdateViewToLaunch(ViewToLaunch.LaunchOpenBankingBuyApprovalError))
                    }, onError = {
                    Timber.e("Error doing SB sync for bank linking $it")
                    interactor.resetLocalBankAuthState()
                    process(RedesignIntent.UpdateViewToLaunch(ViewToLaunch.LaunchOpenBankingBuyApprovalError))
                }
                )
        }
    }

    private fun handleOrderState(state: SimpleBuyState) {
        if (state.orderState == OrderState.AWAITING_FUNDS) {
            process(RedesignIntent.UpdateViewToLaunch(ViewToLaunch.LaunchSimpleBuyFromDeepLinkApproval))
        } else {
            interactor.resetLocalBankAuthState()
            process(RedesignIntent.UpdateViewToLaunch(ViewToLaunch.LaunchPaymentForCancelledOrder(state)))
        }
    }

    private fun NabuApiException.getWalletIdHint(): String =
        getErrorDescription().split(NabuApiException.USER_WALLET_LINK_ERROR_PREFIX).last()
}
