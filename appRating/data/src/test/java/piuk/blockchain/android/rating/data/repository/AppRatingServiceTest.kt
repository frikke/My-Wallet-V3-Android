package piuk.blockchain.android.rating.data.repository

import com.blockchain.api.adapters.ApiError
import com.blockchain.outcome.Outcome
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import piuk.blockchain.android.rating.data.api.AppRatingApi
import piuk.blockchain.android.rating.data.model.AppRatingApiKeys
import piuk.blockchain.android.rating.data.remoteconfig.AppRatingApiKeysRemoteConfig
import piuk.blockchain.android.rating.data.remoteconfig.AppRatingRemoteConfig
import piuk.blockchain.android.rating.domain.model.AppRating
import piuk.blockchain.android.rating.domain.service.AppRatingService
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AppRatingServiceTest {
    private val appRatingRemoteConfig = mockk<AppRatingRemoteConfig>()
    private val appRatingApiKeysRemoteConfig = mockk<AppRatingApiKeysRemoteConfig>()
    private val defaultThreshold = 3
    private val appRatingApi = mockk<AppRatingApi>(relaxed = true)

    private val appRatingService: AppRatingService = AppRatingRepository(
        appRatingRemoteConfig = appRatingRemoteConfig,
        appRatingApiKeysRemoteConfig = appRatingApiKeysRemoteConfig,
        defaultThreshold = defaultThreshold,
        appRatingApi = appRatingApi
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
}
