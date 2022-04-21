package piuk.blockchain.android.ui.interest.domain

import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.AssetFilter
import com.blockchain.outcome.Outcome
import com.nhaarman.mockitokotlin2.mock
import info.blockchain.balance.CryptoCurrency
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import piuk.blockchain.android.ui.interest.domain.repository.AssetInterestService
import piuk.blockchain.android.ui.interest.domain.usecase.GetAccountGroupUseCase
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class GetAccountGroupUseCaseTest {
    private val service = mockk<AssetInterestService>()
    private val useCase = GetAccountGroupUseCase(service)

    @Test
    fun `WHEN service returns failure, THEN failure should be returned`() = runTest {
        coEvery { service.getAccountGroup(any(), any()) } returns Outcome.Failure(Throwable("error"))

        assertTrue { useCase(CryptoCurrency.BTC, AssetFilter.All) is Outcome.Failure }
    }

    @Test
    fun `WHEN service returns success, THEN AccountGroup should be returned`() = runTest {
        val accountGroup: AccountGroup = mock()

        coEvery { service.getAccountGroup(any(), any()) } returns Outcome.Success(accountGroup)

        assertEquals(Outcome.Success(accountGroup), useCase(CryptoCurrency.BTC, AssetFilter.All))
    }
}