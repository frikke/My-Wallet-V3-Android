package piuk.blockchain.android.rating.data.remoteconfig

import com.blockchain.domain.experiments.RemoteConfigService
import com.blockchain.outcome.Outcome
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Single
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import piuk.blockchain.android.rating.data.model.AppRatingApiKeys

@OptIn(ExperimentalCoroutinesApi::class)
class AppRatingApiKeysRemoteConfigTest {
    private val remoteConfig = mockk<RemoteConfigService>()
    private val json = Json

    private val appRatingApiKeysRemoteConfig = AppRatingApiKeysRemoteConfig(remoteConfig, json)

    private val apiKeys = AppRatingApiKeys(surveyId = "surveyId", masterKey = "masterKey", key = "key")

    @Test
    fun `GIVEN successful data, WHEN getApiKeys is called, THEN Success should be returned`() = runTest {
        every { remoteConfig.getRawJson(any()) } returns Single.just(json.encodeToString(apiKeys))

        val result = appRatingApiKeysRemoteConfig.getApiKeys()

        assertEquals(Outcome.Success(apiKeys), result)
    }

    @Test
    fun `GIVEN failure data, WHEN getApiKeys is called, THEN Failure should be returned`() = runTest {
        every { remoteConfig.getRawJson(any()) } returns Single.just("")

        val result = appRatingApiKeysRemoteConfig.getApiKeys()

        assert(result is Outcome.Failure)
    }
}
