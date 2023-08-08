package piuk.blockchain.android.sell

import app.cash.turbine.test
import com.blockchain.core.buy.domain.SimpleBuyService
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.kyc.domain.model.KycTiers
import com.blockchain.core.sell.domain.SellEligibility
import com.blockchain.core.sell.domain.SellUserEligibility
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.FiatCurrency
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.core.Single
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.brokerage.sell.SellRepository

@OptIn(ExperimentalCoroutinesApi::class)
class SellRepositoryTest {

    private lateinit var subject: SellRepository
    private val userFeaturePermissionService: UserFeaturePermissionService = mockk()
    private val kycService: KycService = mockk()
    private val simpleBuyService: SimpleBuyService = mockk()
    private val custodialWalletManager: CustodialWalletManager = mockk()
    private val currencyPrefs: CurrencyPrefs = mockk()

    private val TEST_ASSET = object : CryptoCurrency(
        displayTicker = "NOPE",
        networkTicker = "NOPE",
        name = "Not a real thing",
        categories = setOf(AssetCategory.TRADING),
        precisionDp = 8,
        requiredConfirmations = 3,
        colour = "000000"
    ) {}

    @Before
    fun setup() {
        subject = SellRepository(
            userFeaturePermissionService = userFeaturePermissionService,
            kycService = kycService,
            coincore = mockk(),
            accountsSorting = mockk(),
            simpleBuyService = simpleBuyService,
            custodialWalletManager = custodialWalletManager,
            currencyPrefs = currencyPrefs
        )
    }

    @Test
    fun `given sell checks when user has insufficient kyc tier then not eligible is returned`() = runTest {
        every {
            userFeaturePermissionService.getAccessForFeature(
                Feature.Sell,
                FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
            )
        } returns flowOf(DataResource.Data(FeatureAccess.Blocked(BlockedReason.InsufficientTier.Tier2Required)))

        subject.sellEligibility(FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)).test {
            expectMostRecentItem().run {
                assertTrue(this is DataResource.Data<SellEligibility>)
                this.data shouldBeEqualTo SellEligibility.NotEligible(BlockedReason.InsufficientTier.Tier2Required)
            }
        }

        verify(exactly = 1) {
            userFeaturePermissionService.getAccessForFeature(
                Feature.Sell,
                FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
            )
        }
        verify(exactly = 0) { kycService.getTiers(FreshnessStrategy.Fresh) }
        verify(exactly = 0) { simpleBuyService.isEligible(FreshnessStrategy.Fresh) }
        verify(exactly = 0) { custodialWalletManager.getSupportedFundsFiats() }
        verify(exactly = 0) { currencyPrefs.selectedFiatCurrency }
        verify(exactly = 0) { custodialWalletManager.getSupportedBuySellCryptoCurrencies() }
    }

    @Test
    fun `given sell checks when user is not eligible then not eligible is returned`() = runTest {
        every {
            userFeaturePermissionService.getAccessForFeature(
                Feature.Sell,
                FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
            )
        } returns flowOf(DataResource.Data(FeatureAccess.Blocked(BlockedReason.NotEligible("test"))))

        subject.sellEligibility(FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)).test {
            expectMostRecentItem().run {
                assertTrue(this is DataResource.Data<SellEligibility>)
                this.data shouldBeEqualTo SellEligibility.NotEligible(BlockedReason.NotEligible("test"))
            }
        }

        verify(exactly = 1) {
            userFeaturePermissionService.getAccessForFeature(
                Feature.Sell,
                FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
            )
        }
        verify(exactly = 0) { kycService.getTiers(FreshnessStrategy.Fresh) }
        verify(exactly = 0) { simpleBuyService.isEligible(FreshnessStrategy.Fresh) }
        verify(exactly = 0) { custodialWalletManager.getSupportedFundsFiats() }
        verify(exactly = 0) { currencyPrefs.selectedFiatCurrency }
        verify(exactly = 0) { custodialWalletManager.getSupportedBuySellCryptoCurrencies() }
    }

    @Test
    fun `given sell checks when user is sanctioned then not eligible is returned`() = runTest {
        every {
            userFeaturePermissionService.getAccessForFeature(
                Feature.Sell,
                FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
            )
        } returns flowOf(DataResource.Data(FeatureAccess.Blocked(BlockedReason.Sanctions.RussiaEU5("reason"))))

        subject.sellEligibility(FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)).test {
            expectMostRecentItem().run {
                assertTrue(this is DataResource.Data<SellEligibility>)
                this.data shouldBeEqualTo SellEligibility.NotEligible(BlockedReason.Sanctions.RussiaEU5("reason"))
            }
        }

        verify(exactly = 1) {
            userFeaturePermissionService.getAccessForFeature(
                Feature.Sell,
                FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
            )
        }
        verify(exactly = 0) { kycService.getTiers(FreshnessStrategy.Fresh) }
        verify(exactly = 0) { simpleBuyService.isEligible(FreshnessStrategy.Fresh) }
        verify(exactly = 0) { custodialWalletManager.getSupportedFundsFiats() }
        verify(exactly = 0) { currencyPrefs.selectedFiatCurrency }
        verify(exactly = 0) { custodialWalletManager.getSupportedBuySellCryptoCurrencies() }
    }

    @Test
    fun `given sell checks when user state valid, rejected for gold then rejected state is returned`() = runTest {
        every {
            userFeaturePermissionService.getAccessForFeature(
                Feature.Sell,
                FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
            )
        } returns flowOf(DataResource.Data(FeatureAccess.Granted()))

        val kycTiers: KycTiers = mockk {
            every { isApprovedFor(KycTier.GOLD) } returns false
            every { isRejectedFor(KycTier.GOLD) } returns true
        }
        every { kycService.getTiers(any()) } returns flowOf(DataResource.Data(kycTiers))
        every { simpleBuyService.isEligible(any()) } returns flowOf(DataResource.Data(true))
        subject.sellEligibility(FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)).test {
            expectMostRecentItem().run {
                assertTrue(this is DataResource.Data<SellEligibility>)
                this.data shouldBeEqualTo SellEligibility.KycBlocked(SellUserEligibility.KycRejectedUser)
            }
        }

        verify(exactly = 1) {
            userFeaturePermissionService.getAccessForFeature(
                Feature.Sell,
                FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
            )
        }
        verify(exactly = 0) { custodialWalletManager.getSupportedFundsFiats() }
        verify(exactly = 0) { currencyPrefs.selectedFiatCurrency }
        verify(exactly = 0) { custodialWalletManager.getSupportedBuySellCryptoCurrencies() }
    }

    @Test
    fun `given sell checks when user state valid, gold but not eligible then rejected state is returned`() = runTest {
        every {
            userFeaturePermissionService.getAccessForFeature(
                Feature.Sell,
                FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
            )
        } returns flowOf(DataResource.Data(FeatureAccess.Granted()))

        val kycTiers: KycTiers = mockk {
            every { isApprovedFor(KycTier.GOLD) } returns true
            every { isRejectedFor(KycTier.GOLD) } returns false
        }
        every { kycService.getTiers(any()) } returns flowOf(DataResource.Data(kycTiers))
        every { simpleBuyService.isEligible(any()) } returns flowOf(DataResource.Data(false))

        subject.sellEligibility(FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)).test {
            expectMostRecentItem().run {
                assertTrue(this is DataResource.Data<SellEligibility>)
                this.data shouldBeEqualTo SellEligibility.KycBlocked(SellUserEligibility.KycRejectedUser)
            }
        }

        verify(exactly = 1) {
            userFeaturePermissionService.getAccessForFeature(
                Feature.Sell,
                FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
            )
        }
        verify(exactly = 0) { custodialWalletManager.getSupportedFundsFiats() }
        verify(exactly = 0) { currencyPrefs.selectedFiatCurrency }
        verify(exactly = 0) { custodialWalletManager.getSupportedBuySellCryptoCurrencies() }
    }

    @Test
    fun `given sell checks when user state valid, silver then  non kyc state is returned`() = runTest {
        every {
            userFeaturePermissionService.getAccessForFeature(
                Feature.Sell,
                FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
            )
        } returns flowOf(DataResource.Data(FeatureAccess.Granted()))

        val kycTiers: KycTiers = mockk {
            every { isApprovedFor(KycTier.GOLD) } returns false
            every { isRejectedFor(KycTier.GOLD) } returns false
        }
        every { kycService.getTiers(any()) } returns flowOf(DataResource.Data(kycTiers))
        every { simpleBuyService.isEligible(any()) } returns flowOf(DataResource.Data(true))

        subject.sellEligibility(FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)).test {
            expectMostRecentItem().run {
                assertTrue(this is DataResource.Data<SellEligibility>)
                this.data shouldBeEqualTo SellEligibility.KycBlocked(SellUserEligibility.NonKycdUser)
            }
        }

        verify(exactly = 1) {
            userFeaturePermissionService.getAccessForFeature(
                Feature.Sell,
                FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
            )
        }
        verify(exactly = 0) { custodialWalletManager.getSupportedFundsFiats() }
        verify(exactly = 0) { currencyPrefs.selectedFiatCurrency }
        verify(exactly = 0) { custodialWalletManager.getSupportedBuySellCryptoCurrencies() }
    }

    @Test
    fun `given sell checks when user state valid, gold & eligible then sell list returned`() = runTest {
        every {
            userFeaturePermissionService.getAccessForFeature(
                Feature.Sell,
                FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
            )
        } returns flowOf(DataResource.Data(FeatureAccess.Granted()))

        val kycTiers: KycTiers = mockk {
            every { isApprovedFor(KycTier.GOLD) } returns true
        }
        every { kycService.getTiers(any()) } returns flowOf(DataResource.Data(kycTiers))
        every { simpleBuyService.isEligible(any()) } returns flowOf(DataResource.Data(true))
        every { currencyPrefs.selectedFiatCurrency } returns FiatCurrency.Dollars
        every { custodialWalletManager.getSupportedFundsFiats(FiatCurrency.Dollars) } returns flowOf(
            listOf(FiatCurrency.Dollars)
        )

        val currencyPair: CurrencyPair = mockk {
            every { source } returns TEST_ASSET
            every { destination } returns FiatCurrency.Dollars
        }
        every { custodialWalletManager.getSupportedBuySellCryptoCurrencies() } returns Single.just(listOf(currencyPair))

        subject.sellEligibility(FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)).test {
            expectMostRecentItem().run {
                assertTrue(this is DataResource.Data<SellEligibility>)
                assertTrue(this.data is SellEligibility.Eligible)
                assertTrue((this.data as SellEligibility.Eligible).sellAssets.size == 1)
                assertEquals((this.data as SellEligibility.Eligible).sellAssets[0], TEST_ASSET)
            }
        }

        verify(exactly = 1) {
            userFeaturePermissionService.getAccessForFeature(
                Feature.Sell,
                FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
            )
        }
        verify(exactly = 1) { custodialWalletManager.getSupportedFundsFiats(FiatCurrency.Dollars) }
        verify(exactly = 1) { currencyPrefs.selectedFiatCurrency }
        verify(exactly = 1) { custodialWalletManager.getSupportedBuySellCryptoCurrencies() }
    }

    @Test
    fun `given sell checks when user state valid, gold & eligible sell list errors then error is returned`() = runTest {
        every {
            userFeaturePermissionService.getAccessForFeature(
                Feature.Sell,
                FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
            )
        } returns flowOf(DataResource.Data(FeatureAccess.Granted()))

        val kycTiers: KycTiers = mockk {
            every { isApprovedFor(KycTier.GOLD) } returns true
        }
        every { kycService.getTiers(any()) } returns flowOf(DataResource.Data(kycTiers))
        every { simpleBuyService.isEligible(any()) } returns flowOf(DataResource.Data(true))
        every { currencyPrefs.selectedFiatCurrency } returns FiatCurrency.Dollars
        every { custodialWalletManager.getSupportedFundsFiats(FiatCurrency.Dollars) } returns flowOf(
            listOf(FiatCurrency.Dollars)
        )

        val testException = IllegalArgumentException("my exception")

        every { custodialWalletManager.getSupportedBuySellCryptoCurrencies() } returns Single.error(testException)

        subject.sellEligibility(FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)).test {
            expectMostRecentItem().run {
                assertTrue(this is DataResource.Error)
                assertTrue(this.error is IllegalArgumentException)
                assertEquals(this.error.message, "my exception")
            }
        }

        verify(exactly = 1) {
            userFeaturePermissionService.getAccessForFeature(
                Feature.Sell,
                FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
            )
        }
        verify(exactly = 1) { custodialWalletManager.getSupportedFundsFiats(FiatCurrency.Dollars) }
        verify(exactly = 1) { currencyPrefs.selectedFiatCurrency }
        verify(exactly = 1) { custodialWalletManager.getSupportedBuySellCryptoCurrencies() }
    }

    @Test
    fun `given sell checks when user state valid, gold & eligible supported fiats error then error is returned`() =
        runTest {
            every {
                userFeaturePermissionService.getAccessForFeature(
                    Feature.Sell,
                    FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
                )
            } returns flowOf(DataResource.Data(FeatureAccess.Granted()))

            val kycTiers: KycTiers = mockk {
                every { isApprovedFor(KycTier.GOLD) } returns true
            }
            every { kycService.getTiers(any()) } returns flowOf(DataResource.Data(kycTiers))
            every { simpleBuyService.isEligible(any()) } returns flowOf(DataResource.Data(true))
            every { currencyPrefs.selectedFiatCurrency } returns FiatCurrency.Dollars

            val testException = IllegalArgumentException("my exception")

            every { custodialWalletManager.getSupportedFundsFiats(FiatCurrency.Dollars) } returns flow {
                throw testException
            }

            val currencyPair: CurrencyPair = mockk {
                every { source } returns TEST_ASSET
                every { destination } returns FiatCurrency.Dollars
            }
            every { custodialWalletManager.getSupportedBuySellCryptoCurrencies() } returns Single.just(
                listOf(currencyPair)
            )

            subject.sellEligibility(FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)).test {
                expectMostRecentItem().run {
                    assertTrue(this is DataResource.Error)
                    assertTrue(this.error is IllegalArgumentException)
                    assertEquals(this.error.message, "my exception")
                }
            }

            verify(exactly = 1) {
                userFeaturePermissionService.getAccessForFeature(
                    Feature.Sell,
                    FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
                )
            }
            verify(exactly = 1) { custodialWalletManager.getSupportedFundsFiats(FiatCurrency.Dollars) }
            verify(exactly = 1) { currencyPrefs.selectedFiatCurrency }
            verify(exactly = 1) { custodialWalletManager.getSupportedBuySellCryptoCurrencies() }
        }
}
