package piuk.blockchain.android.ui.interest.domain

import com.blockchain.outcome.Outcome
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import piuk.blockchain.android.ui.interest.domain.model.InterestDashboard
import piuk.blockchain.android.ui.interest.domain.repository.AssetInterestService
import piuk.blockchain.android.ui.interest.domain.usecase.GetInterestDashboardUseCase

@ExperimentalCoroutinesApi
class GetInterestDashboardUseCaseTest {
    private val service = mockk<AssetInterestService>()
    private val useCase = GetInterestDashboardUseCase(service)

    private val interestDashboard = mockk<InterestDashboard>()

    @Test
    fun `WHEN service returns failure, THEN failure should be returned`() = runTest {
        coEvery { service.getInterestDashboard() } returns Outcome.Failure(Throwable("error"))

        assertTrue { useCase() is Outcome.Failure }
    }

    @Test
    fun `WHEN service returns success, THEN interestDashboard should be returned`() = runTest {
        coEvery { service.getInterestDashboard() } returns Outcome.Success(interestDashboard)

        assertEquals(Outcome.Success(interestDashboard), useCase())
    }
}
