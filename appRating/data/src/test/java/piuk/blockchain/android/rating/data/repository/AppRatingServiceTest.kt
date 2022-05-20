package piuk.blockchain.android.rating.data.repository

import com.blockchain.api.adapters.ApiError
import com.blockchain.core.payments.PaymentsDataManager
import com.blockchain.core.payments.model.FundsLocks
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.nabu.service.TierService
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.rating.data.api.AppRatingApi
import piuk.blockchain.android.rating.data.model.AppRatingApiKeys
import piuk.blockchain.android.rating.data.remoteconfig.AppRatingApiKeysRemoteConfig
import piuk.blockchain.android.rating.data.remoteconfig.AppRatingRemoteConfig
import piuk.blockchain.android.rating.domain.model.AppRating
import piuk.blockchain.android.rating.domain.service.AppRatingService
import java.math.BigDecimal
import java.util.Calendar
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AppRatingServiceTest {
    private val appRatingRemoteConfig = mockk<AppRatingRemoteConfig>()
    private val appRatingApiKeysRemoteConfig = mockk<AppRatingApiKeysRemoteConfig>()
    private val defaultThreshold = 3
    private val appRatingApi = mockk<AppRatingApi>()
    private val appRatingPrefs = mockk<AppRatingPrefs>()
    private val tierService = mockk<TierService>()
    private val currencyPrefs = mockk<CurrencyPrefs>()
    private val paymentsDataManager = mockk<PaymentsDataManager>()

    private val appRatingService: AppRatingService = AppRatingRepository(
        appRatingRemoteConfig = appRatingRemoteConfig,
        appRatingApiKeysRemoteConfig = appRatingApiKeysRemoteConfig,
        defaultThreshold = defaultThreshold,
        appRatingApi = appRatingApi,
        appRatingPrefs = appRatingPrefs,
        tierService = tierService,
        currencyPrefs = currencyPrefs,
        paymentsDataManager = paymentsDataManager
    )

    private val appRating = AppRating(rating = 3, feedback = "feedback")
    private val apiKeys = AppRatingApiKeys(surveyId = "surveyId", masterKey = "masterKey", key = "key")
    private val kycTiersGold = mockk<KycTiers>()
    private val kycTiersNotGold = mockk<KycTiers>()
    private val fundsLocksOnHold = mockk<FundsLocks>()
    private val fundsLocksNotOnHold = mockk<FundsLocks>()

    @Before
    fun setUp() {
        every { currencyPrefs.selectedFiatCurrency } returns FiatCurrency.Dollars

        every { kycTiersGold.isApprovedFor(KycTierLevel.GOLD) } returns true
        every { kycTiersNotGold.isApprovedFor(KycTierLevel.GOLD) } returns false

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
    fun `GIVEN success apiKeys, success appRatingApi, WHEN postRatingData is called, THEN true should be returned`() =
        runTest {
            coEvery { appRatingApiKeysRemoteConfig.getApiKeys() } returns Outcome.Success(apiKeys)
            coEvery { appRatingApi.postRatingData(apiKeys, appRating) } returns Outcome.Success(true)

            val result = appRatingService.postRatingData(appRating)

            assertEquals(true, result)
        }

    @Test
    fun `GIVEN success apiKeys, failure appRatingApi, WHEN postRatingData is called, THEN false should be returned`() =
        runTest {
            coEvery { appRatingApiKeysRemoteConfig.getApiKeys() } returns Outcome.Success(apiKeys)
            coEvery { appRatingApi.postRatingData(apiKeys, appRating) } returns Outcome.Failure(
                ApiError.HttpError(
                    Throwable()
                )
            )

            val result = appRatingService.postRatingData(appRating)

            assertEquals(false, result)
        }

    @Test
    fun `GIVEN failure apiKeys, WHEN postRatingData is called, THEN false should be returned`() = runTest {
        coEvery { appRatingApiKeysRemoteConfig.getApiKeys() } returns Outcome.Failure(Throwable())

        val result = appRatingService.postRatingData(appRating)

        assertEquals(false, result)
    }

    @Test
    fun `GIVEN rating not complete, kyc GOLD, no withdrawal locks, promptDate is more than 1 month, WHEN shouldShowRating is called, THEN true should be returned`() =
        runTest {
            every { appRatingPrefs.completed } returns false
            every { tierService.tiers() } returns Single.just(kycTiersGold)
            every { paymentsDataManager.getWithdrawalLocks(any()) } returns Single.just(fundsLocksNotOnHold)
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
            every { tierService.tiers() } returns Single.just(kycTiersNotGold)

            val result = appRatingService.shouldShowRating()

            assertEquals(false, result)
        }

    @Test
    fun `GIVEN rating not complete, kyc GOLD, withdrawal locks WHEN shouldShowRating is called, THEN false should be returned`() =
        runTest {
            every { appRatingPrefs.completed } returns false
            every { tierService.tiers() } returns Single.just(kycTiersGold)
            every { paymentsDataManager.getWithdrawalLocks(any()) } returns Single.just(fundsLocksOnHold)

            val result = appRatingService.shouldShowRating()

            assertEquals(false, result)
        }

    @Test
    fun `GIVEN rating not complete, kyc GOLD, no withdrawal locks, promptDate is less than 1 month, WHEN shouldShowRating is called, THEN false should be returned`() =
        runTest {
            every { appRatingPrefs.completed } returns false
            every { tierService.tiers() } returns Single.just(kycTiersGold)
            every { paymentsDataManager.getWithdrawalLocks(any()) } returns Single.just(fundsLocksNotOnHold)
            // simulate prompt was show 20 seconds ago = less than a month
            every { appRatingPrefs.promptDateMillis } returns
                Calendar.getInstance().apply { add(Calendar.SECOND, -20) }.timeInMillis

            val result = appRatingService.shouldShowRating()

            assertEquals(false, result)
        }

    @Test
    fun `WHEN markRatingCompleted is called, THEN completed should be true`() {
        every { appRatingPrefs.promptDateMillis = any() } just Runs
        every { appRatingPrefs.completed = any() } just Runs

        appRatingService.markRatingCompleted()

        verify { appRatingPrefs.completed = true }
    }
}
