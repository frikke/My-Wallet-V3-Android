package piuk.blockchain.android.ui.auth

import com.blockchain.remoteconfig.RemoteConfig
import com.nhaarman.mockitokotlin2.given
import com.nhaarman.mockitokotlin2.mock
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString

class FirebaseMobileNoticeRemoteConfigTest {

    private val remoteConfig: RemoteConfig = mock()

    private val json = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
        isLenient = true
    }

    private lateinit var subject: FirebaseMobileNoticeRemoteConfig

    @Before
    fun setup() {
        subject = FirebaseMobileNoticeRemoteConfig(remoteConfig, json)
    }

    @Test
    fun `remoteConfig returns valid payload`() {
        // ARRANGE
        given(remoteConfig.getRawJson(anyString())).willReturn(
            Single.just("{\"title\":\"1\",\"body\":\"2\",\"ctaText\":\"3\",\"ctaLink\":\"4\"}")
        )

        // ACT
        val result = subject.mobileNoticeDialog()

        // ASSERT
        result
            .test()
            .assertComplete()
            .assertValue {
                it.equals(MobileNoticeDialog("1", "2", "3", "4"))
            }
    }

    @Test
    fun `remoteConfig returns invalid payload`() {
        // ARRANGE
        given(remoteConfig.getRawJson(anyString())).willReturn(Single.just("{}"))

        // ACT
        val result = subject.mobileNoticeDialog()

        // ASSERT
        result
            .test()
            .assertComplete()
            .assertValue {
                it.equals(MobileNoticeDialog())
            }
    }

    @Test
    fun `remoteConfig returns empty payload`() {
        // ARRANGE
        given(remoteConfig.getRawJson(anyString())).willReturn(Single.just(""))

        // ACT
        val result = subject.mobileNoticeDialog()

        // ASSERT
        result
            .test()
            .assertError { it is NoSuchElementException }
    }
}
