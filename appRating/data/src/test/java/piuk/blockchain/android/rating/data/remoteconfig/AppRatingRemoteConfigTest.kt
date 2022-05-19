package piuk.blockchain.android.rating.data.remoteconfig

import com.blockchain.outcome.Outcome
import com.blockchain.remoteconfig.RemoteConfig
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Single
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppRatingRemoteConfigTest {
    private val remoteConfig = mockk<RemoteConfig>()

    private val appRatingRemoteConfig = AppRatingRemoteConfig(remoteConfig)

    @Test
    fun `GIVEN successful data, WHEN getThreshold is called, THEN Success should be returned`() = runTest {
        val threshold = 3
        every { remoteConfig.getRawJson(any()) } returns Single.just(threshold.toString())

        val result = appRatingRemoteConfig.getThreshold()

        assertEquals(Outcome.Success(threshold), result)
    }

    @Test
    fun `GIVEN failure data, WHEN getThreshold is called, THEN Failure should be returned`() = runTest {
        every { remoteConfig.getRawJson(any()) } returns Single.just("")

        val result = appRatingRemoteConfig.getThreshold()

        assert(result is Outcome.Failure)
    }
}
