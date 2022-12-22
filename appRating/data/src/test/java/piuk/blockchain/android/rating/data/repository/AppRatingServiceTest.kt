package piuk.blockchain.android.rating.data.repository

import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.kyc.domain.model.KycTiers
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.nabu.Feature
import com.blockchain.nabu.UserIdentity
import com.blockchain.outcome.Outcome
import com.blockchain.preferences.AppRatingPrefs
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal
import java.util.Calendar
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.rating.data.api.AppRatingApi
import piuk.blockchain.android.rating.data.model.AppRatingApiKeys
import piuk.blockchain.android.rating.data.remoteconfig.AppRatingApiKeysRemoteConfig
import piuk.blockchain.android.rating.data.remoteconfig.AppRatingRemoteConfig
import piuk.blockchain.android.rating.domain.model.AppRating
import piuk.blockchain.android.rating.domain.service.AppRatingService

@OptIn(ExperimentalCoroutinesApi::class)
class AppRatingServiceTest {

    private val appRatingRemoteConfig = mockk<AppRatingRemoteConfig>()
    private val appRatingApiKeysRemoteConfig = mockk<AppRatingApiKeysRemoteConfig>()
    private val defaultThreshold = 3
    private val appRatingApi = mockk<AppRatingApi>()
    private val appRatingPrefs = mockk<AppRatingPrefs>()
    private val userIdentity = mockk<UserIdentity>()
    private val currencyPrefs = mockk<CurrencyPrefs>()
    private val bankService = mockk<BankService>()

    private val appRatingService: AppRatingService = AppRatingRepository(
        coroutineScope = TestScope(),
        dispatcher = UnconfinedTestDispatcher(),

        appRatingRemoteConfig = appRatingRemoteConfig,
        appRatingApiKeysRemoteConfig = appRatingApiKeysRemoteConfig,
        defaultThreshold = defaultThreshold,
        appRatingApi = appRatingApi,
        appRatingPrefs = appRatingPrefs,
        userIdentity = userIdentity,
        currencyPrefs = currencyPrefs,
        bankService = bankService
    )

    private val appRating = AppRating(rating = 3, feedback = "feedback")
    private val apiKeys = AppRatingApiKeys(surveyId = "surveyId", masterKey = "masterKey", key = "key")
    private val kycTiersGold = mockk<KycTiers>()
    private val kycTiersNotGold = mockk<KycTiers>()
    private val fundsLocksOnHold = mockk<FundsLocks>()
    private val fundsLocksNotOnHold = mockk<FundsLocks>()
    private val fetureTierGold = Feature.TierLevel(KycTier.GOLD)

    @Before
    fun setUp() {

        every { currencyPrefs.selectedFiatCurrency } returns FiatCurrency.Dollars

        every { appRatingPrefs.promptDateMillis = any() } just Runs
        every { appRatingPrefs.completed = any() } just Runs

        every { kycTiersGold.isApprovedFor(KycTier.GOLD) } returns true
        every { kycTiersNotGold.isApprovedFor(KycTier.GOLD) } returns false

        every { fundsLocksOnHold.onHoldTotalAmount } returns Money.fromMajor(CryptoCurrency.BTC, BigDecimal.TEN)
        every { fundsLocksNotOnHold.onHoldTotalAmount } returns Money.fromMajor(CryptoCurrency.BTC, BigDecimal.ZERO)
    }

    @Test
    fun `GIVEN successful threshold, WHEN getThreshold is called, THEN threshold should be returned`() = runTest {
        val threshold = 4
        coEvery { appRatingRemoteConfig.getThreshold() } returns Outcome.Success(threshold)

        val result = appRatingService.getThreshold()

        assertEquals(threshold, result)
    }

    @Test
    fun `GIVEN failure threshold, WHEN getThreshold is called, THEN defaultThreshold should be returned`() = runTest {
        coEvery { appRatingRemoteConfig.getThreshold() } returns Outcome.Failure(NumberFormatException())

        val result = appRatingService.getThreshold()

        assertEquals(defaultThreshold, result)
    }

    @Test
    fun `GIVEN success apiKeys, success appRatingApi, WHEN postRatingData is called, THEN completed should be true`() =
        runTest {
            coEvery { appRatingApiKeysRemoteConfig.getApiKeys() } returns Outcome.Success(apiKeys)
            coEvery { appRatingApi.postRatingData(apiKeys, appRating) } returns Outcome.Success(true)

            appRatingService.postRatingData(appRating)

            verify(exactly = 1) { appRatingPrefs.promptDateMillis = any() }
            verify(exactly = 1) { appRatingPrefs.completed = true }
        }

    @Test
    fun `GIVEN success apiKeys, failure appRatingApi, WHEN postRatingData is called, THEN false should be returned`() =
        runTest {
            coEvery { appRatingApiKeysRemoteConfig.getApiKeys() } returns Outcome.Success(apiKeys)
            coEvery { appRatingApi.postRatingData(apiKeys, appRating) } returns Outcome.Failure(mockk())

            appRatingService.postRatingData(appRating)

            verify(exactly = 1) { appRatingPrefs.promptDateMillis = any() }
            verify(exactly = 0) { appRatingPrefs.completed = true }
        }

    @Test
    fun `GIVEN failure apiKeys, WHEN postRatingData is called, THEN false should be returned`() = runTest {
        coEvery { appRatingApiKeysRemoteConfig.getApiKeys() } returns Outcome.Failure(Throwable())

        appRatingService.postRatingData(appRating)

        verify(exactly = 1) { appRatingPrefs.promptDateMillis = any() }
        verify(exactly = 0) { appRatingPrefs.completed = true }
    }

    @Test
    fun `GIVEN rating not complete, kyc GOLD, no withdrawal locks, promptDate is more than 1 month, WHEN shouldShowRating is called, THEN true should be returned`() =
        runTest {
            every { appRatingPrefs.completed } returns false
            every { userIdentity.isVerifiedFor(fetureTierGold) } returns Single.just(true)
            every { bankService.getWithdrawalLocksLegacy(any()) } returns Single.just(fundsLocksNotOnHold)
            every { appRatingPrefs.promptDateMillis } returns 0L

            val result = appRatingService.shouldShowRating()

            assertEquals(true, result)
        }

    @Test
    fun `GIVEN rating complete, WHEN shouldShowRating is called, THEN false should be returned`() = runTest {
        every { appRatingPrefs.completed } returns true

        val result = appRatingService.shouldShowRating()

        assertEquals(false, result)
    }

    @Test
    fun `GIVEN rating not complete, kyc not GOLD, WHEN shouldShowRating is called, THEN false should be returned`() =
        runTest {
            every { appRatingPrefs.completed } returns false
            every { userIdentity.isVerifiedFor(fetureTierGold) } returns Single.just(false)

            val result = appRatingService.shouldShowRating()

            assertEquals(false, result)
        }

    @Test
    fun `GIVEN rating not complete, kyc GOLD, withdrawal locks WHEN shouldShowRating is called, THEN false should be returned`() =
        runTest {
            every { appRatingPrefs.completed } returns false
            every { userIdentity.isVerifiedFor(fetureTierGold) } returns Single.just(true)
            every { bankService.getWithdrawalLocksLegacy(any()) } returns Single.just(fundsLocksOnHold)

            val result = appRatingService.shouldShowRating()

            assertEquals(false, result)
        }

    @Test
    fun `GIVEN rating not complete, kyc GOLD, no withdrawal locks, promptDate is less than 1 month, WHEN shouldShowRating is called, THEN false should be returned`() =
        runTest {
            every { appRatingPrefs.completed } returns false
            every { userIdentity.isVerifiedFor(fetureTierGold) } returns Single.just(true)
            every { bankService.getWithdrawalLocksLegacy(any()) } returns Single.just(fundsLocksNotOnHold)
            // simulate prompt was show 20 seconds ago = less than a month
            every { appRatingPrefs.promptDateMillis } returns
                Calendar.getInstance().apply { add(Calendar.SECOND, -20) }.timeInMillis

            val result = appRatingService.shouldShowRating()

            assertEquals(false, result)
        }
}
