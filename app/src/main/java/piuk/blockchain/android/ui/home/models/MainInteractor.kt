package piuk.blockchain.android.ui.home.models

import android.content.Intent
import android.net.Uri
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.impl.CustodialInterestAccount
import com.blockchain.coincore.impl.CustodialStakingAccount
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.core.referral.ReferralRepository
import com.blockchain.deeplinking.navigation.DeeplinkRedirector
import com.blockchain.deeplinking.processor.DeepLinkResult
import com.blockchain.deeplinking.processor.LinkState
import com.blockchain.domain.auth.SecureChannelService
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.model.BankAuthDeepLinkState
import com.blockchain.domain.paymentmethods.model.BankAuthFlowState
import com.blockchain.domain.paymentmethods.model.BankPaymentApproval
import com.blockchain.domain.paymentmethods.model.BankTransferDetails
import com.blockchain.domain.paymentmethods.model.BankTransferStatus
import com.blockchain.domain.paymentmethods.model.fromPreferencesValue
import com.blockchain.domain.paymentmethods.model.toPreferencesValue
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.home.presentation.navigation.ScanResult
import com.blockchain.nabu.UserIdentity
import com.blockchain.network.PollResult
import com.blockchain.network.PollService
import com.blockchain.preferences.BankLinkingPrefs
import com.blockchain.preferences.ReferralPrefs
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import exchange.ExchangeLinking
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.rx3.asObservable
import kotlinx.coroutines.rx3.rxCompletable
import piuk.blockchain.android.deeplink.DeepLinkProcessor
import piuk.blockchain.android.domain.usecases.CancelOrderUseCase
import piuk.blockchain.android.scan.QrScanResultProcessor
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.ui.home.CredentialsWiper
import piuk.blockchain.android.ui.launcher.DeepLinkPersistence

class MainInteractor internal constructor(
    private val deepLinkProcessor: DeepLinkProcessor,
    private val deeplinkRedirector: DeeplinkRedirector,
    private val deepLinkPersistence: DeepLinkPersistence,
    private val exchangeLinking: ExchangeLinking,
    private val assetCatalogue: AssetCatalogue,
    private val bankLinkingPrefs: BankLinkingPrefs,
    private val bankService: BankService,
    private val simpleBuySync: SimpleBuySyncFactory,
    private val userIdentity: UserIdentity,
    private val credentialsWiper: CredentialsWiper,
    private val qrScanResultProcessor: QrScanResultProcessor,
    private val secureChannelService: SecureChannelService,
    private val cancelOrderUseCase: CancelOrderUseCase,
    private val referralPrefs: ReferralPrefs,
    private val referralRepository: ReferralRepository,
    private val ethDataManager: EthDataManager,
    private val membershipFlag: FeatureFlag,
    private val coincore: Coincore,
    private val walletModeService: WalletModeService,
    private val earnOnNavBarFlag: FeatureFlag
) {

    fun checkForDeepLinks(intent: Intent): Maybe<LinkState> =
        deepLinkProcessor.getLink(intent)

    fun checkForDeepLinks(scanResult: ScanResult.HttpUri): Single<LinkState> =
        Single.fromCallable {
            Uri.parse(scanResult.uri).getQueryParameter("link") ?: throw IllegalStateException()
        }.flatMap {
            deepLinkProcessor.getLink(it)
        }

    fun checkForUserWalletErrors(): Completable =
        userIdentity.checkForUserWalletLinkErrors()

    fun getExchangeLinkingState(): Single<Boolean> =
        exchangeLinking.isExchangeLinked()

    fun getAssetFromTicker(ticker: String?): AssetInfo? =
        ticker?.let {
            assetCatalogue.assetInfoFromNetworkTicker(ticker)
        }

    fun resetLocalBankAuthState() =
        bankLinkingPrefs.setBankLinkingState(
            BankAuthDeepLinkState(bankAuthFlow = BankAuthFlowState.NONE, bankPaymentData = null, bankLinkingInfo = null)
                .toPreferencesValue()
        )

    fun getBankLinkingState(): BankAuthDeepLinkState =
        bankLinkingPrefs.getBankLinkingState().fromPreferencesValue() ?: BankAuthDeepLinkState()

    fun updateBankLinkingState(bankLinkingState: BankAuthDeepLinkState) =
        bankLinkingPrefs.setBankLinkingState(bankLinkingState.toPreferencesValue())

    fun updateOpenBankingConsent(consentToken: String): Completable =
        bankService.updateOpenBankingConsent(
            bankLinkingPrefs.getDynamicOneTimeTokenUrl(),
            consentToken
        ).doOnError {
            resetLocalBankAuthState()
        }

    fun pollForBankTransferCharge(paymentData: BankPaymentApproval): Single<PollResult<BankTransferDetails>> =
        PollService(
            bankService.getBankTransferCharge(paymentData.paymentId)
        ) { transferDetails ->
            transferDetails.status != BankTransferStatus.Pending
        }.start()

    fun getEstimatedDepositCompletionTime(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 3)
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(cal.time)
    }

    fun getSimpleBuySyncLocalState(): SimpleBuyState? = simpleBuySync.currentState()

    fun performSimpleBuySync(): Completable = simpleBuySync.performSync()

    fun unpairWallet(): Completable =
        Completable.fromAction {
            credentialsWiper.wipe()
        }

    fun processQrScanResult(decodedData: String): Single<out ScanResult> =
        qrScanResultProcessor.processScan(decodedData)

    fun sendSecureChannelHandshake(handshake: String) =
        secureChannelService.sendHandshake(handshake)

    fun cancelOrder(orderId: String): Completable =
        cancelOrderUseCase.invoke(orderId)

    fun processDeepLinkV2(url: Uri): Single<DeepLinkResult> =
        deeplinkRedirector.processDeeplinkURL(url)

    fun clearDeepLink(): Completable =
        Completable.fromAction {
            deepLinkPersistence.popDataFromSharedPrefs()
        }

    fun checkReferral(): Single<ReferralState> = Single.just(ReferralState(ReferralInfo.NotAvailable))

    fun storeReferralClicked() {
        referralPrefs.hasReferralIconBeenClicked = true
    }

    fun getSupportedEvmNetworks() = ethDataManager.supportedNetworks

    fun selectAccountForTxFlow(networkTicker: String, action: AssetAction): Single<LaunchFlowForAccount> =
        if (action == AssetAction.FiatDeposit) {
            coincore.allFiats().map { fiatAccount ->
                fiatAccount.filter { account ->
                    account.currency.networkTicker == networkTicker
                }
            }.map { accounts ->
                return@map if (accounts.isEmpty() || accounts.size > 1) {
                    LaunchFlowForAccount.NoAccount
                } else {
                    LaunchFlowForAccount.TargetAccount(accounts[0] as TransactionTarget)
                }
            }
        } else {
            Single.just(LaunchFlowForAccount.NoAccount)
            /*  coincore.walletsWithAction(action = action).map { accountsForAction ->
                  accountsForAction.filter { account ->
                      account.currency.networkTicker == networkTicker
                  }
              }.map { sameAssetAccounts ->

                  val eligibleTradingAccount = sameAssetAccounts.filterIsInstance<CustodialTradingAccount>()
                      .firstOrNull { tradingAccount ->
                          tradingAccount.isFunded
                      }

                  when {
                      eligibleTradingAccount != null -> {
                          return@map LaunchFlowForAccount.SourceAccount(eligibleTradingAccount)
                      }
                      else -> {
                          val eligibleNonCustodialAccount =
                              sameAssetAccounts.filterIsInstance<CryptoNonCustodialAccount>()
                                  .firstOrNull { ncAccount ->
                                      ncAccount.isFunded
                                  }
                          when {
                              eligibleNonCustodialAccount != null -> {
                                  return@map LaunchFlowForAccount.SourceAccount(eligibleNonCustodialAccount)
                              }
                              else -> {
                                  return@map LaunchFlowForAccount.NoAccount
                              }
                          }
                      }
                  }
              }*/
        }

    fun selectRewardsAccountForAsset(cryptoTicker: String): Single<LaunchFlowForAccount> =
        assetCatalogue.assetInfoFromNetworkTicker(cryptoTicker)?.let { cryptoCurrency ->
            coincore[cryptoCurrency].accountGroup(AssetFilter.Interest).toSingle()
                .map {
                    val interestAccount = it.accounts.first() as CustodialInterestAccount
                    LaunchFlowForAccount.SourceAccount(interestAccount)
                }
        } ?: Single.just(LaunchFlowForAccount.NoAccount)

    fun selectStakingAccountForCurrency(currency: Currency): Single<BlockchainAccount> =
        coincore[currency].accountGroup(AssetFilter.Staking).toSingle()
            .map {
                it.accounts.first() as CustodialStakingAccount
            }

    fun getEnabledWalletMode(): Observable<WalletMode> =
        walletModeService.walletMode.asObservable()

    fun updateWalletMode(mode: WalletMode): Completable =
        Completable.fromAction {
            rxCompletable { walletModeService.updateEnabledWalletMode(mode) }
        }

    fun isEarnOnNavBarEnabled(): Single<Boolean> = earnOnNavBarFlag.enabled
}
