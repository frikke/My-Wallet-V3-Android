package piuk.blockchain.android.ui.linkbank.data.yapily

import com.blockchain.remoteconfig.RemoteConfig
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Single
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.linkbank.domain.yapily.service.SafeConnectService

@ExperimentalCoroutinesApi
class SafeConnectRepositoryTest {
    private val remoteConfig = mockk<RemoteConfig>()
    private val service: SafeConnectService = SafeConnectRepository(remoteConfig)

    private val tosLink = "TosLink"

    @Before
    fun setUp() {
        every { remoteConfig.getRawJson(any()) } returns Single.just(tosLink)
    }

    @Test
    fun `GIVEN remoteConfig returns value, WHEN service calls getTosLink, THEN value should be returned`() = runTest {
        val result = service.getTosLink()

        assertEquals(tosLink, result)
    }
}
