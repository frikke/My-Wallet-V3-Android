package piuk.blockchain.android.simplebuy

import com.blockchain.android.testutils.rxInit
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.price.ExchangeRate
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.domain.fiatcurrencies.model.TradingCurrencies
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.outcome.Outcome
import com.blockchain.testutils.EUR
import com.blockchain.testutils.GBP
import com.blockchain.testutils.USD
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class BuyFlowNavigatorTest {

    private val userIdentity: UserIdentity = mock {
        on { userAccessForFeature(Feature.Buy) }.thenReturn(Single.just(FeatureAccess.Granted()))
    }
    private val kycService: KycService = mock()
    private val fiatCurrenciesService: FiatCurrenciesService = mock {
        onBlocking { getTradingCurrencies() }.thenReturn(Outcome.Success(tradingCurrencies))
        on { selectedTradingCurrency }.thenReturn(tradingCurrencies.selected)
    }
    private val custodialWalletManager: CustodialWalletManager = mock {
        on { availableFiatCurrenciesForTrading(any()) }.thenReturn(Single.just(tradingCurrencies.allAvailable))
    }
    private val simpleBuySyncFactory: SimpleBuySyncFactory = mock()
    private lateinit var subject: BuyFlowNavigator

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        computationTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = BuyFlowNavigator(
            simpleBuySyncFactory, userIdentity, kycService, fiatCurrenciesService, custodialWalletManager
        )
    }

    @Test
    fun `if user is not eligible to buy then it should navigate to Kyc Upgrade Now`() {
        val eligibility = FeatureAccess.Blocked(BlockedReason.InsufficientTier.Tier2Required)
        mockCurrencyIsSupported()
        whenever(simpleBuySyncFactory.currentState()).thenReturn(SimpleBuyState())
        whenever(userIdentity.userAccessForFeature(Feature.Buy)).thenReturn(Single.just(eligibility))

        val test = subject.navigateTo(
            startedFromKycResume = false,
            startedFromDashboard = true,
            startedFromApprovalDeepLink = false,
            preselectedCrypto = CryptoCurrency.BTC,
            failOnUnavailableCurrency = false
        ).test()

        test.assertValueAt(0, BuyNavigation.BlockBuy(BlockedReason.InsufficientTier.Tier2Required))
    }

    @Test
    fun `if currency is not  supported  and startedFromDashboard then screen should be currency selector`() {
        runBlocking {
            val tradingCurrencies = TradingCurrencies(
                selected = USD,
                allRecommended = listOf(USD, GBP),
                allAvailable = listOf(USD, GBP)
            )
            whenever(simpleBuySyncFactory.currentState()).thenReturn(SimpleBuyState())
            whenever(custodialWalletManager.availableFiatCurrenciesForTrading(CryptoCurrency.BTC))
                .thenReturn(Single.just(listOf(GBP, EUR)))
            whenever(fiatCurrenciesService.getTradingCurrencies()).thenReturn(Outcome.Success(tradingCurrencies))
            whenever(fiatCurrenciesService.selectedTradingCurrency).thenReturn(tradingCurrencies.selected)

            val test =
                subject.navigateTo(
                    startedFromKycResume = false,
                    startedFromDashboard = true,
                    startedFromApprovalDeepLink = false,
                    preselectedCrypto = CryptoCurrency.BTC,
                    failOnUnavailableCurrency = false
                )
                    .test()
            test.assertValueAt(0, BuyNavigation.CurrencySelection(listOf(GBP), USD))
        }
    }

    @Test
    fun `if currency is not  supported  and should fail on check then it should faile`() {
        runBlocking {
            val tradingCurrencies = TradingCurrencies(
                selected = USD,
                allRecommended = listOf(USD, GBP),
                allAvailable = listOf(USD, GBP)
            )
            whenever(simpleBuySyncFactory.currentState()).thenReturn(SimpleBuyState())
            whenever(custodialWalletManager.availableFiatCurrenciesForTrading(CryptoCurrency.BTC))
                .thenReturn(Single.just(listOf(GBP, EUR)))
            whenever(fiatCurrenciesService.getTradingCurrencies()).thenReturn(Outcome.Success(tradingCurrencies))
            whenever(fiatCurrenciesService.selectedTradingCurrency).thenReturn(tradingCurrencies.selected)

            val test =
                subject.navigateTo(
                    startedFromKycResume = false,
                    startedFromDashboard = true,
                    startedFromApprovalDeepLink = false,
                    preselectedCrypto = CryptoCurrency.BTC,
                    failOnUnavailableCurrency = true
                )
                    .test()
            test.assertValueAt(0, BuyNavigation.CurrencyNotAvailable)
        }
    }

    @Test
    fun `if currency is  supported and state is clear and startedFromDashboard then screen should be enter amount`() {
        mockCurrencyIsSupported()
        whenever(simpleBuySyncFactory.currentState()).thenReturn(SimpleBuyState())

        val test =
            subject.navigateTo(
                startedFromKycResume = false,
                startedFromDashboard = true,
                startedFromApprovalDeepLink = false,
                preselectedCrypto = CryptoCurrency.BTC,
                failOnUnavailableCurrency = false
            ).test()
        test.assertValueAt(0, BuyNavigation.FlowScreenWithCurrency(FlowScreen.ENTER_AMOUNT, CryptoCurrency.BTC))
    }

    @Test
    fun `if currency is supported and state is clear and startedFromApprovalDeepLink then screen should be payment`() {
        mockCurrencyIsSupported()

        whenever(simpleBuySyncFactory.currentState()).thenReturn(SimpleBuyState())

        val test =
            subject.navigateTo(
                startedFromKycResume = false,
                startedFromDashboard = false,
                startedFromApprovalDeepLink = true,
                preselectedCrypto = CryptoCurrency.BTC
            ).test()
        test.assertValueAt(0, BuyNavigation.OrderInProgressScreen)
    }

    // KYC tests
    @Test
    fun `if  current is screen is KYC and tier 2 approved then screen should be enter amount`() {
        mockCurrencyIsSupported()
        whenever(simpleBuySyncFactory.currentState())
            .thenReturn(
                SimpleBuyState().copy(currentScreen = FlowScreen.KYC)
            )

        whenever(userIdentity.isVerifiedFor(Feature.TierLevel(KycTier.GOLD))).thenReturn(Single.just(true))
        whenever(kycService.isInProgress()).thenReturn(Single.just(false))
        whenever(kycService.isRejectedFor(KycTier.GOLD)).thenReturn(Single.just(false))

        val test =
            subject.navigateTo(
                startedFromKycResume = false,
                startedFromDashboard = true,
                startedFromApprovalDeepLink = false,
                preselectedCrypto = CryptoCurrency.BTC
            ).test()
        test.assertValueAt(0, BuyNavigation.FlowScreenWithCurrency(FlowScreen.ENTER_AMOUNT, CryptoCurrency.BTC))
    }

    @Test
    fun `if  current is screen is KYC and tier 2 is pending then screen should be kyc verification`() {
        mockCurrencyIsSupported()

        whenever(simpleBuySyncFactory.currentState())
            .thenReturn(
                SimpleBuyState().copy(currentScreen = FlowScreen.KYC)
            )
        whenever(userIdentity.isVerifiedFor(Feature.TierLevel(KycTier.GOLD))).thenReturn(Single.just(false))
        whenever(kycService.isInProgress()).thenReturn(Single.just(true))
        whenever(kycService.isRejectedFor(KycTier.GOLD)).thenReturn(Single.just(false))

        val test =
            subject.navigateTo(
                startedFromKycResume = false,
                startedFromDashboard = true,
                startedFromApprovalDeepLink = false,
                preselectedCrypto = CryptoCurrency.BTC
            ).test()
        test.assertValueAt(
            0,
            BuyNavigation.FlowScreenWithCurrency(FlowScreen.KYC_VERIFICATION, CryptoCurrency.BTC)
        )
    }

    @Test
    fun `if  current is screen is KYC and tier 2 is none then screen should be kyc`() {
        mockCurrencyIsSupported()
        whenever(simpleBuySyncFactory.currentState())
            .thenReturn(
                SimpleBuyState().copy(currentScreen = FlowScreen.KYC)
            )
        whenever(userIdentity.isVerifiedFor(Feature.TierLevel(KycTier.GOLD))).thenReturn(Single.just(false))
        whenever(kycService.isInProgress()).thenReturn(Single.just(false))
        whenever(kycService.isRejectedFor(KycTier.GOLD)).thenReturn(Single.just(false))

        val test =
            subject.navigateTo(
                startedFromKycResume = false,
                startedFromDashboard = true,
                startedFromApprovalDeepLink = false,
                preselectedCrypto = CryptoCurrency.BTC
            ).test()
        test.assertValueAt(
            0,
            BuyNavigation.FlowScreenWithCurrency(FlowScreen.KYC, CryptoCurrency.BTC)
        )
    }

    private fun mockCurrencyIsSupported() = runBlocking {
        val tradingCurrencies = TradingCurrencies(
            selected = EUR,
            allRecommended = listOf(GBP, EUR),
            allAvailable = listOf(GBP, EUR)
        )
        whenever(custodialWalletManager.availableFiatCurrenciesForTrading(CryptoCurrency.BTC))
            .thenReturn(Single.just(listOf(GBP, EUR)))
        whenever(fiatCurrenciesService.getTradingCurrencies()).thenReturn(Outcome.Success(tradingCurrencies))
        whenever(fiatCurrenciesService.selectedTradingCurrency).thenReturn(tradingCurrencies.selected)
    }

    companion object {
        private val btcExchangeRate = ExchangeRate(from = GBP, to = CryptoCurrency.BTC, rate = null)

        private val tradingCurrencies = TradingCurrencies(
            selected = USD,
            allRecommended = listOf(USD, GBP),
            allAvailable = listOf(USD, GBP)
        )
    }
}
