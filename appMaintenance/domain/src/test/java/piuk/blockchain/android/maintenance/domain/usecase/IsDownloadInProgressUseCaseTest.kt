package piuk.blockchain.android.maintenance.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import piuk.blockchain.android.maintenance.domain.repository.AppMaintenanceService

class IsDownloadInProgressUseCaseTest {

    private val repository = mockk<AppMaintenanceService>()
    private val useCase by lazy { IsDownloadInProgressUseCase(repository = repository) }

    @Test
    fun `WHEN repository returns true, THEN true should be returned`() = runBlocking {
        coEvery { repository.isDownloadInProgress() } returns true

        val result = useCase()

        assertEquals(true, result)
    }

    @Test
    fun `WHEN repository returns false, THEN false should be returned`() = runBlocking {
        coEvery { repository.isDownloadInProgress() } returns false

        val result = useCase()

        assertEquals(false, result)
    }
}
