package piuk.blockchain.android.rating.data.repository

import com.blockchain.api.adapters.ApiError
import com.blockchain.outcome.Outcome
import com.blockchain.preferences.AppRatingPrefs
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.util.Calendar
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
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

    private val appRatingService: AppRatingService = AppRatingRepository(
        appRatingRemoteConfig = appRatingRemoteConfig,
        appRatingApiKeysRemoteConfig = appRatingApiKeysRemoteConfig,
        defaultThreshold = defaultThreshold,
        appRatingApi = appRatingApi,
        appRatingPrefs = appRatingPrefs
    )

    private val appRating = AppRating(rating = 3, feedback = "feedback")
    private val apiKeys = AppRatingApiKeys(surveyId = "surveyId", masterKey = "masterKey", key = "key")

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
    fun `GIVEN rating not complete, promptDate is more than 1 month, WHEN shouldShowRating is called, THEN true should be returned`() {
        every { appRatingPrefs.completed } returns false
        every { appRatingPrefs.promptDateMillis } returns 0L

        val result = appRatingService.shouldShowRating()

        assertEquals(true, result)
    }

    @Test
    fun `GIVEN rating complete, WHEN shouldShowRating is called, THEN false should be returned`() {
        every { appRatingPrefs.completed } returns true

        val result = appRatingService.shouldShowRating()

        assertEquals(false, result)
    }

    @Test
    fun `GIVEN rating not complete, promptDate is less than 1 month, WHEN shouldShowRating is called, THEN false should be returned`() {
        every { appRatingPrefs.completed } returns false
        every { appRatingPrefs.promptDateMillis } returns
            Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -2) }.timeInMillis

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
