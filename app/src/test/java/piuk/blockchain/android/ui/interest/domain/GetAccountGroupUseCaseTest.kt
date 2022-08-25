package piuk.blockchain.android.ui.interest.domain

import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.Asset
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.outcome.Outcome
import com.nhaarman.mockitokotlin2.mock
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Currency
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Maybe
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.interest.domain.usecase.GetAccountGroupUseCase

@ExperimentalCoroutinesApi
class GetAccountGroupUseCaseTest {
    private val coincore = mockk<Coincore>()
    private val useCase = GetAccountGroupUseCase(coincore)

    private val asset = mockk<Asset>()

    @Before
    fun setUp() {
        every { coincore[any<Currency>()] } returns asset
    }

    @Test
    fun `WHEN service returns failure, THEN failure should be returned`() = runTest {
        every { asset.accountGroup(any()) } returns Maybe.error(Exception())

        assertTrue { useCase(CryptoCurrency.BTC, AssetFilter.All) is Outcome.Failure }
    }

    @Test
    fun `WHEN service returns success, THEN AccountGroup should be returned`() = runTest {
        val accountGroup: AccountGroup = mock()

        every { asset.accountGroup(any()) } returns Maybe.just(accountGroup)

        assertEquals(Outcome.Success(accountGroup), useCase(CryptoCurrency.BTC, AssetFilter.All))
    }
}
