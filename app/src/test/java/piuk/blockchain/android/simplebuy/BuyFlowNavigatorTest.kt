package piuk.blockchain.android.simplebuy

import com.blockchain.core.price.ExchangeRate
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.testutils.EUR
import com.blockchain.testutils.GBP
import com.blockchain.testutils.PLN
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test

class BuyFlowNavigatorTest {

    private val userIdentity: UserIdentity = mock {
        on { userAccessForFeature(Feature.SimpleBuy) }.thenReturn(Single.just(FeatureAccess.Granted()))
        on { userAccessForFeature(Feature.Buy) }.thenReturn(Single.just(FeatureAccess.Granted()))
    }
    private val currencyPrefs: CurrencyPrefs = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val simpleBuySyncFactory: SimpleBuySyncFactory = mock()
    private lateinit var subject: BuyFlowNavigator

    @Before
    fun setUp() {
        subject = BuyFlowNavigator(
            simpleBuySyncFactory, userIdentity, currencyPrefs, custodialWalletManager
        )
    }

    @Test
    fun `if user is not eligible to buy then it should navigate to Kyc Upgrade Now`() {
        val eligibility = FeatureAccess.Blocked(BlockedReason.InsufficientTier)
        mockCurrencyIsSupported(true)
        whenever(simpleBuySyncFactory.currentState()).thenReturn(SimpleBuyState())
        whenever(userIdentity.userAccessForFeature(Feature.Buy)).thenReturn(Single.just(eligibility))

        val test = subject.navigateTo(
            startedFromKycResume = false,
            startedFromDashboard = true,
            startedFromApprovalDeepLink = false,
            preselectedCrypto = CryptoCurrency.BTC,
            failOnUnavailableCurrency = false
        ).test()

        test.assertValueAt(0, BuyNavigation.TransactionsLimitReached)
    }

    @Test
    fun `if currency is not  supported  and startedFromDashboard then screen should be currency selector`() {
        mockCurrencyIsSupported(false)
        whenever(simpleBuySyncFactory.currentState()).thenReturn(SimpleBuyState())
        whenever(custodialWalletManager.getSupportedFiatCurrencies()).thenReturn(Single.just(listOf(GBP, EUR)))
        whenever(currencyPrefs.selectedFiatCurrency).thenReturn(PLN)
        whenever(currencyPrefs.tradingCurrency).thenReturn(PLN)
        whenever(custodialWalletManager.isCurrencySupportedForSimpleBuy(PLN))
            .thenReturn(Single.just(false))

        val test =
            subject.navigateTo(
                startedFromKycResume = false,
                startedFromDashboard = true,
                startedFromApprovalDeepLink = false,
                preselectedCrypto = CryptoCurrency.BTC,
                failOnUnavailableCurrency = false
            )
                .test()
        test.assertValueAt(0, BuyNavigation.CurrencySelection(listOf(GBP, EUR), PLN))
    }

    @Test
    fun `if currency is not  supported  and should fail on check then it should faile`() {
        mockCurrencyIsSupported(false)
        whenever(simpleBuySyncFactory.currentState()).thenReturn(SimpleBuyState())
        whenever(custodialWalletManager.getSupportedFiatCurrencies()).thenReturn(Single.just(listOf(GBP, EUR)))
        whenever(currencyPrefs.tradingCurrency).thenReturn(PLN)
        whenever(custodialWalletManager.isCurrencySupportedForSimpleBuy(PLN))
            .thenReturn(Single.just(false))

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

    @Test
    fun `if currency is  supported and state is clear and startedFromDashboard then screen should be enter amount`() {
        mockCurrencyIsSupported(true)
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
        mockCurrencyIsSupported(true)

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
        mockCurrencyIsSupported(true)
        whenever(simpleBuySyncFactory.currentState())
            .thenReturn(
                SimpleBuyState().copy(currentScreen = FlowScreen.KYC)
            )

        whenever(userIdentity.isVerifiedFor(Feature.TierLevel(Tier.GOLD))).thenReturn(Single.just(true))
        whenever(userIdentity.isKycInProgress()).thenReturn(Single.just(false))
        whenever(userIdentity.isRejectedForTier(Feature.TierLevel(Tier.GOLD))).thenReturn(Single.just(false))

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
        mockCurrencyIsSupported(true)

        whenever(simpleBuySyncFactory.currentState())
            .thenReturn(
                SimpleBuyState().copy(currentScreen = FlowScreen.KYC)
            )
        whenever(userIdentity.isVerifiedFor(Feature.TierLevel(Tier.GOLD))).thenReturn(Single.just(false))
        whenever(userIdentity.isKycInProgress()).thenReturn(Single.just(true))
        whenever(userIdentity.isRejectedForTier(Feature.TierLevel(Tier.GOLD))).thenReturn(Single.just(false))

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
        mockCurrencyIsSupported(true)
        whenever(simpleBuySyncFactory.currentState())
            .thenReturn(
                SimpleBuyState().copy(currentScreen = FlowScreen.KYC)
            )
        whenever(userIdentity.isVerifiedFor(Feature.TierLevel(Tier.GOLD))).thenReturn(Single.just(false))
        whenever(userIdentity.isKycInProgress()).thenReturn(Single.just(false))
        whenever(userIdentity.isRejectedForTier(Feature.TierLevel(Tier.GOLD))).thenReturn(Single.just(false))

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

    private fun mockCurrencyIsSupported(supported: Boolean) {
        whenever(
            custodialWalletManager
                .isCurrencySupportedForSimpleBuy(GBP)
        ).thenReturn(Single.just(supported))
        whenever(currencyPrefs.tradingCurrency).thenReturn((GBP))
    }

    companion object {
        private val btcExchangeRate = ExchangeRate(from = GBP, to = CryptoCurrency.BTC, rate = null)
    }
}
