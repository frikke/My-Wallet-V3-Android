package piuk.blockchain.android.maintenance.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import piuk.blockchain.android.maintenance.domain.repository.AppMaintenanceRepository

class IsDownloadInProgressUseCaseTest {

    private val repository = mockk<AppMaintenanceRepository>()
    private val useCase by lazy { IsDownloadInProgressUseCase(repository = repository) }

    @Test
    fun testDownloadInProgressTrue() = runBlocking {
        coEvery { repository.isDownloadInProgress() } returns true

        val result = useCase()

        assertEquals(true, result)
    }

    @Test
    fun testDownloadInProgressFalse() = runBlocking {
        coEvery { repository.isDownloadInProgress() } returns false

        val result = useCase()

        assertEquals(false, result)
    }
}
