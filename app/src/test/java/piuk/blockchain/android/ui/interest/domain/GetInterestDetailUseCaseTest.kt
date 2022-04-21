package piuk.blockchain.android.ui.interest.domain

import com.blockchain.outcome.Outcome
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import piuk.blockchain.android.ui.interest.domain.model.InterestDetail
import piuk.blockchain.android.ui.interest.domain.repository.AssetInterestService
import piuk.blockchain.android.ui.interest.domain.usecase.GetInterestDetailUseCase
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class GetInterestDetailUseCaseTest {
    private val service = mockk<AssetInterestService>()
    private val useCase = GetInterestDetailUseCase(service)

    private val interestDetail = mockk<InterestDetail>()

    @Test
    fun `WHEN service returns failure, THEN failure should be returned`() = runTest {
        coEvery { service.getInterestDetail() } returns Outcome.Failure(Throwable("error"))

        assertTrue { useCase() is Outcome.Failure }
    }

    @Test
    fun `WHEN service returns success, THEN InterestDetail should be returned`() = runTest {
        coEvery { service.getInterestDetail() } returns Outcome.Success(interestDetail)

        assertEquals(Outcome.Success(interestDetail), useCase())
    }
}