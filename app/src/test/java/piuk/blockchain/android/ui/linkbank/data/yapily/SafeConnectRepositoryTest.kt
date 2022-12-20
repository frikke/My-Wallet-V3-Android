package piuk.blockchain.android.ui.linkbank.data.yapily

import com.blockchain.domain.experiments.RemoteConfigService
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Single
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.linkbank.data.openbanking.SafeConnectRepository
import piuk.blockchain.android.ui.linkbank.domain.openbanking.service.SafeConnectService

@ExperimentalCoroutinesApi
class SafeConnectRepositoryTest {
    private val remoteConfigService = mockk<RemoteConfigService>()
    private val service: SafeConnectService = SafeConnectRepository(remoteConfigService)

    private val tosLink = "TosLink"

    @Before
    fun setUp() {
        every { remoteConfigService.getRawJson(any()) } returns Single.just(tosLink)
    }

    @Test
    fun `GIVEN remoteConfigService returns value, WHEN service calls getTosLink, THEN value should be returned`() =
        runTest {
            val result = service.getTosLink()

            assertEquals(tosLink, result)
        }
}
