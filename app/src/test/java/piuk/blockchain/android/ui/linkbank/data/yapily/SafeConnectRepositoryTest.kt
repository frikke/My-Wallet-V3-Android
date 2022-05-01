package piuk.blockchain.android.ui.linkbank.data.yapily

import com.blockchain.remoteconfig.RemoteConfig
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.linkbank.domain.yapily.service.SafeConnectService
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class SafeConnectRepositoryTest {
    private val remoteConfig = mockk<RemoteConfig>()
    private val service: SafeConnectService = SafeConnectRepository(remoteConfig)

    private val rawValue = "raw"

    @Before
    fun setUp() {
        every { remoteConfig.getRawJson(any()) } returns Single.just(rawValue)
    }

    @Test
    fun `GIVEN remoteConfig returns value, WHEN service calls getTosLink, THEN value should be returned`() = runTest {
        val result = service.getTosLink()

        assertEquals(rawValue, result)
    }
}