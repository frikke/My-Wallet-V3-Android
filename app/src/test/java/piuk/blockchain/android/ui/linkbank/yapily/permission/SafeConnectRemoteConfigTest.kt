package piuk.blockchain.android.ui.linkbank.yapily.permission

import com.blockchain.remoteconfig.RemoteConfig
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Single
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.linkbank.yapily.permission.domain.SafeConnectRemoteConfig

@ExperimentalCoroutinesApi
class SafeConnectRemoteConfigTest {
    private val remoteConfig = mockk<RemoteConfig>()
    private val safeConnectRemoteConfig = SafeConnectRemoteConfig(remoteConfig)

    private val rawString = "rawString"

    @Before
    fun setUp() {
        every { remoteConfig.getRawJson(any()) } returns Single.just(rawString)
    }

    @Test
    fun `WHEN remoteConfig returns, THEN getTosPdfLink should return that raw value`() = runTest {
        val result = safeConnectRemoteConfig.getTosPdfLink()
        assertEquals(rawString, result)
    }
}
