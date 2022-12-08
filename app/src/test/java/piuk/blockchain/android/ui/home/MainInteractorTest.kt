package piuk.blockchain.android.ui.home

import android.content.Intent
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.Asset
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.fiat.FiatCustodialAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.coincore.impl.CustodialInterestAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.core.referral.ReferralRepository
import com.blockchain.deeplinking.navigation.DeeplinkRedirector
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.outcome.Outcome
import com.blockchain.preferences.BankLinkingPrefs
import com.blockchain.preferences.ReferralPrefs
import com.blockchain.serializers.BigDecimalSerializer
import com.blockchain.walletmode.WalletModeService
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import exchange.ExchangeLinking
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import piuk.blockchain.android.deeplink.DeepLinkProcessor
import piuk.blockchain.android.domain.usecases.CancelOrderUseCase
import piuk.blockchain.android.scan.QrScanResultProcessor
import piuk.blockchain.android.scan.ScanResult
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.ui.auth.newlogin.domain.service.SecureChannelService
import piuk.blockchain.android.ui.home.models.LaunchFlowForAccount
import piuk.blockchain.android.ui.home.models.MainInteractor
import piuk.blockchain.android.ui.home.models.ReferralState
import piuk.blockchain.android.ui.launcher.DeepLinkPersistence
import piuk.blockchain.android.ui.linkbank.BankAuthDeepLinkState
import piuk.blockchain.android.ui.upsell.KycUpgradePromptManager

@OptIn(ExperimentalCoroutinesApi::class) class MainInteractorTest {

    private lateinit var interactor: MainInteractor
    private val deepLinkProcessor: DeepLinkProcessor = mock()
    private val deeplinkRedirector: DeeplinkRedirector = mock()
    private val deepLinkPersistence: DeepLinkPersistence = mock()
    private val exchangeLinking: ExchangeLinking = mock()
    private val assetCatalogue: AssetCatalogue = mock()
    private val bankLinkingPrefs: BankLinkingPrefs = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val simpleBuySync: SimpleBuySyncFactory = mock()
    private val userIdentity: UserIdentity = mock()
    private val upsellManager: KycUpgradePromptManager = mock()
    private val credentialsWiper: CredentialsWiper = mock()
    private val qrScanResultProcessor: QrScanResultProcessor = mock()
    private val secureChannelService: SecureChannelService = mock()
    private val cancelOrderUseCase: CancelOrderUseCase = mock()
    private val bankService: BankService = mock()
    private val referralPrefs: ReferralPrefs = mock()
    private val referralRepository: ReferralRepository = mock()
    private val ethDataManager: EthDataManager = mock()
    private val stakingFF: FeatureFlag = mock()
    private val membershipsFF: FeatureFlag = mock()
    private val earnEnabledFF: FeatureFlag = mock()
    private val coincore: Coincore = mock()
    private val walletModeService: WalletModeService = mock()

    private val jsonSerializers = module {
        single {
            Json {
                explicitNulls = false
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
                serializersModule = SerializersModule {
                    contextual(BigDecimalSerializer)
                }
            }
        }
    }

    @Before
    fun setup() {
        GlobalContext.startKoin {
            modules(
                jsonSerializers,
            )
        }

        interactor = MainInteractor(
            deepLinkProcessor = deepLinkProcessor,
            deeplinkRedirector = deeplinkRedirector,
            deepLinkPersistence = deepLinkPersistence,
            exchangeLinking = exchangeLinking,
            assetCatalogue = assetCatalogue,
            bankLinkingPrefs = bankLinkingPrefs,
            simpleBuySync = simpleBuySync,
            userIdentity = userIdentity,
            upsellManager = upsellManager,
            credentialsWiper = credentialsWiper,
            qrScanResultProcessor = qrScanResultProcessor,
            secureChannelService = secureChannelService,
            cancelOrderUseCase = cancelOrderUseCase,
            bankService = bankService,
            referralPrefs = referralPrefs,
            referralRepository = referralRepository,
            ethDataManager = ethDataManager,
            stakingAccountFlag = stakingFF,
            membershipFlag = membershipsFF,
            coincore = coincore,
            earnOnNavBarFlag = earnEnabledFF,
            walletModeService = walletModeService
        )
    }

    @After
    fun cleanup() {
        stopKoin()
    }

    @Test
    fun checkForDeepLink_Intent() {
        val intent: Intent = mock()

        interactor.checkForDeepLinks(intent)
        whenever(deepLinkProcessor.getLink(intent)).thenReturn(Single.just(mock()))
        verify(deepLinkProcessor).getLink(intent)
    }

    @Test
    @Ignore("Underlying Uri.parse is an android call")
    fun checkForDeepLink_ScanResult() {
        val url = "https://test.com?link=1234"
        val result = ScanResult.HttpUri(url, false)

        interactor.checkForDeepLinks(result)

        verify(deepLinkProcessor).getLink("1234")
    }

    @Test
    fun checkForWalletErrors() {
        interactor.checkForUserWalletErrors()
        whenever(userIdentity.checkForUserWalletLinkErrors()).thenReturn(Completable.complete())
        verify(userIdentity).checkForUserWalletLinkErrors()
    }

    @Test
    fun getExchangeLinkingState() {
        interactor.getExchangeLinkingState()
        verify(exchangeLinking).isExchangeLinked()
    }

    @Test
    fun getAssetFromTicker_Known_Ticker() {
        val assetInfo: AssetInfo = mock()
        val ticker = "BTC"
        whenever(assetCatalogue.assetInfoFromNetworkTicker(ticker)).thenReturn(assetInfo)

        val resultAsset = interactor.getAssetFromTicker(ticker)
        verify(assetCatalogue).assetInfoFromNetworkTicker(ticker)
        Assert.assertEquals(assetInfo, resultAsset)
    }

    @Test
    fun getAssetFromTicker_Unknown_Ticker() {
        val assetInfo: AssetInfo? = null
        val ticker = "BTC"
        whenever(assetCatalogue.assetInfoFromNetworkTicker(ticker)).thenReturn(null)

        val resultAsset = interactor.getAssetFromTicker(ticker)

        verify(assetCatalogue).assetInfoFromNetworkTicker(ticker)
        Assert.assertEquals(assetInfo, resultAsset)
    }

    @Test
    fun resetLocalBankState() {
        doNothing().whenever(bankLinkingPrefs).setBankLinkingState(any())
        interactor.resetLocalBankAuthState()
        verify(bankLinkingPrefs).setBankLinkingState(any())
    }

    @Test
    fun setLocalBankState() {
        doNothing().whenever(bankLinkingPrefs).setBankLinkingState(any())
        interactor.updateBankLinkingState(BankAuthDeepLinkState())
        verify(bankLinkingPrefs).setBankLinkingState(any())
    }

    @Test
    fun updateConsent_Success() {
        val consentToken = "1234"
        val tokenUrl = "token url"
        whenever(bankLinkingPrefs.getDynamicOneTimeTokenUrl()).thenReturn(tokenUrl)
        whenever(bankService.updateOpenBankingConsent(tokenUrl, consentToken)).thenReturn(
            Completable.complete()
        )

        interactor.updateOpenBankingConsent(consentToken)

        verify(bankLinkingPrefs).getDynamicOneTimeTokenUrl()
        verify(bankService).updateOpenBankingConsent(tokenUrl, consentToken)
        verifyNoMoreInteractions(bankLinkingPrefs)
        verifyNoMoreInteractions(custodialWalletManager)
    }

    @Test
    fun updateConsent_Error() {
        val consentToken = "1234"
        val tokenUrl = "token url"
        val exception = Exception("test")

        whenever(bankLinkingPrefs.getDynamicOneTimeTokenUrl()).thenReturn(tokenUrl)
        whenever(bankService.updateOpenBankingConsent(tokenUrl, consentToken)).thenReturn(
            Completable.error(exception)
        )
        doNothing().whenever(bankLinkingPrefs).setBankLinkingState(any())

        val observer = interactor.updateOpenBankingConsent(consentToken).test()
        observer
            .assertNotComplete()
            .assertError(exception)

        verify(bankLinkingPrefs).getDynamicOneTimeTokenUrl()
        verify(bankLinkingPrefs).setBankLinkingState(any())
        verify(bankService).updateOpenBankingConsent(tokenUrl, consentToken)
        verifyNoMoreInteractions(bankLinkingPrefs)
        verifyNoMoreInteractions(custodialWalletManager)
    }

    @Test
    fun unpairWallet() {
        doNothing().whenever(credentialsWiper).wipe()

        val observer = interactor.unpairWallet().test()
        observer.assertComplete()

        verify(credentialsWiper).wipe()
    }

    @Test
    fun simpleBuy_LocalState() {
        val sbState: SimpleBuyState = mock()
        whenever(simpleBuySync.currentState()).thenReturn(sbState)
        val result = interactor.getSimpleBuySyncLocalState()

        verify(simpleBuySync).currentState()
        Assert.assertEquals(sbState, result)
    }

    @Test
    fun simpleBuy_Sync() {
        whenever(simpleBuySync.performSync()).thenReturn(Completable.complete())
        val observer = interactor.performSimpleBuySync().test()
        observer.assertComplete()

        verify(simpleBuySync).performSync()
    }

    @Test
    fun cancelOrder() {
        // Arrange
        val orderId = "orderId"

        // Act
        interactor.cancelOrder(orderId)

        // Assert
        verify(cancelOrderUseCase).invoke(orderId)
    }

    @Test
    fun fetchReferralData() {
        runBlocking {
            val referralMock = mock<ReferralInfo.Data>()
            whenever(referralRepository.fetchReferralDataLegacy()).thenReturn(Outcome.Success(referralMock))
            whenever(referralPrefs.hasReferralIconBeenClicked).thenReturn(true)
            whenever(membershipsFF.coEnabled()).thenReturn(false)

            interactor.checkReferral()
                .test()
                .await()
                .assertComplete()
                .assertValue(ReferralState(referralMock, true, areMembershipsEnabled = false))
        }
    }

    @Test
    fun `given no wallets with same ticker when function called then NoAccount should be returned`() {
        val btcCurrency = mock<AssetInfo> {
            on { networkTicker }.thenReturn("BTC")
        }
        val ethCurrency = mock<AssetInfo> {
            on { networkTicker }.thenReturn("ETH")
        }
        val accountList = listOf<CustodialTradingAccount>(
            mock {
                on { currency }.thenReturn(btcCurrency)
            },
            mock {
                on { currency }.thenReturn(ethCurrency)
            }
        )

        whenever(coincore.walletsWithActions(setOf(AssetAction.Sell))).thenReturn(Single.just(accountList))

        val result = interactor.selectAccountForTxFlow("XLM", AssetAction.Sell).test()
        result.assertValue {
            it is LaunchFlowForAccount.NoAccount
        }
    }

    @Test
    fun `given a funded custodial account when called then it should be returned`() {
        val btcCurrency = mock<AssetInfo> {
            on { networkTicker }.thenReturn("BTC")
        }
        val ethCurrency = mock<AssetInfo> {
            on { networkTicker }.thenReturn("ETH")
        }
        val btcAccount = mock<CustodialTradingAccount> {
            on { currency }.thenReturn(btcCurrency)
            on { isFunded }.thenReturn(true)
        }
        val ethAccount = mock<CustodialTradingAccount> {
            on { currency }.thenReturn(ethCurrency)
            on { isFunded }.thenReturn(true)
        }
        val accountList = listOf(btcAccount, ethAccount)

        whenever(coincore.walletsWithActions(setOf(AssetAction.Sell))).thenReturn(Single.just(accountList))

        val result = interactor.selectAccountForTxFlow("BTC", AssetAction.Sell).test()
        result.assertValue {
            it is LaunchFlowForAccount.SourceAccount && it.source == btcAccount
        }
    }

    @Test
    fun `given no funded custodial account and a funded non-custodial account when called then it should be returned`() {
        val btcCurrency = mock<AssetInfo> {
            on { networkTicker }.thenReturn("BTC")
        }
        val ethCurrency = mock<AssetInfo> {
            on { networkTicker }.thenReturn("ETH")
        }
        val btcCustodialAccount = mock<CustodialTradingAccount> {
            on { currency }.thenReturn(btcCurrency)
            on { isFunded }.thenReturn(false)
        }
        val ethCustodialAccount = mock<CustodialTradingAccount> {
            on { currency }.thenReturn(ethCurrency)
            on { isFunded }.thenReturn(true)
        }
        val btcNonCustodialAccount = mock<CryptoNonCustodialAccount> {
            on { currency }.thenReturn(btcCurrency)
            on { isFunded }.thenReturn(true)
        }
        val ethNonCustodialAccount = mock<CryptoNonCustodialAccount> {
            on { currency }.thenReturn(ethCurrency)
            on { isFunded }.thenReturn(true)
        }
        val accountList = listOf(
            btcCustodialAccount,
            ethCustodialAccount,
            btcNonCustodialAccount,
            ethNonCustodialAccount
        )

        whenever(coincore.walletsWithActions(setOf(AssetAction.Sell))).thenReturn(Single.just(accountList))

        val result = interactor.selectAccountForTxFlow("BTC", AssetAction.Sell).test()
        result.assertValue {
            it is LaunchFlowForAccount.SourceAccount && it.source == btcNonCustodialAccount
        }
    }

    @Test
    fun `given no funded custodial or non-custodial accounts when called then NoAccount should be returned`() {
        val btcCurrency = mock<AssetInfo> {
            on { networkTicker }.thenReturn("BTC")
        }
        val ethCurrency = mock<AssetInfo> {
            on { networkTicker }.thenReturn("ETH")
        }
        val btcCustodialAccount = mock<CustodialTradingAccount> {
            on { currency }.thenReturn(btcCurrency)
            on { isFunded }.thenReturn(false)
        }
        val ethCustodialAccount = mock<CustodialTradingAccount> {
            on { currency }.thenReturn(ethCurrency)
            on { isFunded }.thenReturn(false)
        }
        val btcNonCustodialAccount = mock<CryptoNonCustodialAccount> {
            on { currency }.thenReturn(btcCurrency)
            on { isFunded }.thenReturn(false)
        }
        val ethNonCustodialAccount = mock<CryptoNonCustodialAccount> {
            on { currency }.thenReturn(ethCurrency)
            on { isFunded }.thenReturn(false)
        }
        val accountList = listOf(
            btcCustodialAccount,
            ethCustodialAccount,
            btcNonCustodialAccount,
            ethNonCustodialAccount
        )

        whenever(coincore.walletsWithActions(setOf(AssetAction.Sell))).thenReturn(Single.just(accountList))

        val result = interactor.selectAccountForTxFlow("BTC", AssetAction.Sell).test()
        result.assertValue {
            it is LaunchFlowForAccount.NoAccount
        }
    }

    @Test
    fun `given valid account when fiat deposit action then account should be returned`() {
        val usdCurrency = mock<FiatCurrency> {
            on { networkTicker }.thenReturn("USD")
        }
        val gbpCurrency = mock<FiatCurrency> {
            on { networkTicker }.thenReturn("GBP")
        }
        val usdFiatAccount = mock<FiatCustodialAccount> {
            on { currency }.thenReturn(usdCurrency)
            on { isFunded }.thenReturn(false)
        }
        val gbpFiatAccount = mock<FiatCustodialAccount> {
            on { currency }.thenReturn(gbpCurrency)
            on { isFunded }.thenReturn(false)
        }

        val accountList = listOf(
            usdFiatAccount,
            gbpFiatAccount
        )

        whenever(coincore.allFiats()).thenReturn(Single.just(accountList))

        val result = interactor.selectAccountForTxFlow("GBP", AssetAction.FiatDeposit).test()
        result.assertValue {
            it is LaunchFlowForAccount.TargetAccount && it.target == gbpFiatAccount
        }

        verify(coincore).allFiats()
        verifyNoMoreInteractions(coincore)
    }

    @Test
    fun `given no account when fiat deposit action then NoAccount should be returned`() {
        val usdCurrency = mock<FiatCurrency> {
            on { networkTicker }.thenReturn("USD")
        }
        val gbpCurrency = mock<FiatCurrency> {
            on { networkTicker }.thenReturn("GBP")
        }
        val usdFiatAccount = mock<FiatCustodialAccount> {
            on { currency }.thenReturn(usdCurrency)
            on { isFunded }.thenReturn(false)
        }
        val gbpFiatAccount = mock<FiatCustodialAccount> {
            on { currency }.thenReturn(gbpCurrency)
            on { isFunded }.thenReturn(false)
        }

        val accountList = listOf(
            usdFiatAccount,
            gbpFiatAccount
        )

        whenever(coincore.allFiats()).thenReturn(Single.just(accountList))

        val result = interactor.selectAccountForTxFlow("EUR", AssetAction.FiatDeposit).test()
        result.assertValue {
            it is LaunchFlowForAccount.NoAccount
        }

        verify(coincore).allFiats()
        verifyNoMoreInteractions(coincore)
    }

    @Test
    fun `given too many accounts when fiat deposit action then NoAccount should be returned`() {
        val usdCurrency = mock<FiatCurrency> {
            on { networkTicker }.thenReturn("USD")
        }
        val gbpCurrency = mock<FiatCurrency> {
            on { networkTicker }.thenReturn("GBP")
        }

        val usdFiatAccount = mock<FiatCustodialAccount> {
            on { currency }.thenReturn(usdCurrency)
            on { isFunded }.thenReturn(false)
        }
        val gbpFiatAccount = mock<FiatCustodialAccount> {
            on { currency }.thenReturn(gbpCurrency)
            on { isFunded }.thenReturn(false)
        }
        val gbpFiatAccount2 = mock<FiatCustodialAccount> {
            on { currency }.thenReturn(gbpCurrency)
            on { isFunded }.thenReturn(false)
        }

        val accountList = listOf(
            usdFiatAccount,
            gbpFiatAccount,
            gbpFiatAccount2
        )

        whenever(coincore.allFiats()).thenReturn(Single.just(accountList))

        val result = interactor.selectAccountForTxFlow("GBP", AssetAction.FiatDeposit).test()
        result.assertValue {
            it is LaunchFlowForAccount.NoAccount
        }

        verify(coincore).allFiats()
        verifyNoMoreInteractions(coincore)
    }

    @Test
    fun `given no rewards account for ticker when usecase called then NoAccount is returned`() {
        val assetTicker = "BTC"

        whenever(assetCatalogue.assetInfoFromNetworkTicker(assetTicker)).thenReturn(null)

        val result = interactor.selectRewardsAccountForAsset(assetTicker).test()

        result.assertValue {
            it is LaunchFlowForAccount.NoAccount
        }

        verifyNoMoreInteractions(coincore)
    }

    @Test
    fun `given rewards account for ticker when usecase called then valid Account is returned`() {
        val assetTicker = "BTC"
        val assetInfo: AssetInfo = mock()
        val interestAccount = mock<CustodialInterestAccount>()
        val accountGroup: AccountGroup = mock {
            on { accounts }.thenReturn(listOf(interestAccount))
        }
        val asset: Asset = mock {
            on { accountGroup(AssetFilter.Interest) }.thenReturn(Maybe.just(accountGroup))
        }

        whenever(assetCatalogue.assetInfoFromNetworkTicker(assetTicker)).thenReturn(assetInfo)
        whenever(coincore[assetInfo]).thenReturn(asset)

        val result = interactor.selectRewardsAccountForAsset(assetTicker).test()

        result.assertValue {
            it is LaunchFlowForAccount.SourceAccount && it.source == interestAccount
        }
    }
}
