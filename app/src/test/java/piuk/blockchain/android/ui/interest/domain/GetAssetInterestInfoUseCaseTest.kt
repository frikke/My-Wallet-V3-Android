package piuk.blockchain.android.ui.interest.domain

import com.blockchain.outcome.Outcome
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import piuk.blockchain.android.ui.interest.domain.model.AssetInterestDetail
import piuk.blockchain.android.ui.interest.domain.model.AssetInterestInfo
import piuk.blockchain.android.ui.interest.domain.repository.AssetInterestService
import piuk.blockchain.android.ui.interest.domain.usecase.GetAssetInterestInfoUseCase

@ExperimentalCoroutinesApi
class GetAssetInterestInfoUseCaseTest {
    private val service = mockk<AssetInterestService>()
    private val useCase = GetAssetInterestInfoUseCase(service)

    @Test
    fun `WHEN service returns failure, THEN failure should be returned`() = runTest {
        coEvery { service.getAssetsInterestInfo(any()) } returns Outcome.Failure(Throwable("error"))

        assertTrue { useCase(listOf()) is Outcome.Failure }
    }

    @Test
    fun `WHEN service returns mixed data, THEN filter before returning`() = runTest {
        val list = listOf(
            AssetInterestInfo(
                CryptoCurrency.ETHER,
                AssetInterestDetail(
                    mockk(), mockk(), mockk(relaxed = true), mockk(relaxed = true), mockk(),
                    totalBalanceFiat = Money.fromMajor(CryptoCurrency.ETHER, 100.toBigDecimal())
                )
            ),
            AssetInterestInfo(
                CryptoCurrency.BTC,
                AssetInterestDetail(
                    mockk(), mockk(), mockk(relaxed = true), mockk(relaxed = true), mockk(),
                    totalBalanceFiat = Money.fromMajor(CryptoCurrency.BTC, 0.toBigDecimal())
                )
            ),
            AssetInterestInfo(
                CryptoCurrency.XLM,
                AssetInterestDetail(
                    mockk(), mockk(), mockk(relaxed = true), mockk(relaxed = true), mockk(),
                    totalBalanceFiat = Money.fromMajor(CryptoCurrency.XLM, 200.toBigDecimal())
                )
            ),
            AssetInterestInfo(
                CryptoCurrency.BCH,
                AssetInterestDetail(
                    mockk(), mockk(), mockk(relaxed = true), mockk(relaxed = true), mockk(),
                    totalBalanceFiat = Money.fromMajor(CryptoCurrency.XLM, 0.toBigDecimal())
                )
            )
        )
        // sort should be xlm (higher balance) - eth (lower balance) - btc (balance 0 but has priority) - bch
        coEvery { service.getAssetsInterestInfo(any()) } returns Outcome.Success(list)

        val result = useCase(listOf())
        assertTrue { result is Outcome.Success }

        //
        (result as Outcome.Success).value.let { value ->
            assertEquals(CryptoCurrency.XLM, value[0].assetInfo)
            assertEquals(CryptoCurrency.ETHER, value[1].assetInfo)
            assertEquals(CryptoCurrency.BTC, value[2].assetInfo)
            assertEquals(CryptoCurrency.BCH, value[3].assetInfo)
        }
    }
}
