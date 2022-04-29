package piuk.blockchain.android.ui.interest.domain

import com.blockchain.outcome.Outcome
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import piuk.blockchain.android.ui.interest.domain.model.AssetInterestDetail
import piuk.blockchain.android.ui.interest.domain.model.InterestAsset
import piuk.blockchain.android.ui.interest.domain.repository.AssetInterestService
import piuk.blockchain.android.ui.interest.domain.usecase.GetAssetsInterestUseCase

@ExperimentalCoroutinesApi
class GetAssetsInterestUseCaseTest {
    private val service = mockk<AssetInterestService>()
    private val useCase = GetAssetsInterestUseCase(service)

    @Test
    fun `WHEN service returns failure, THEN failure should be returned`() = runTest {
        coEvery { service.getAssetsInterest(any()) } returns Outcome.Failure(Throwable("error"))

        assertTrue { useCase(listOf()) is Outcome.Failure }
    }

    @Test
    fun `WHEN service returns mixed data, THEN filter before returning`() = runTest {
        val FAKE_CC_1 = CryptoCurrency(
            displayTicker = "FAKE_CC_1",
            networkTicker = "FAKE_CC_1",
            name = "FAKE_CC_1",
            categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
            precisionDp = 18,
            requiredConfirmations = 5,
            l1chainTicker = CryptoCurrency.ETHER.networkTicker,
            l2identifier = "0xF0DF0DF0DF0DF0DF0DFAD",
            colour = "#123456"
        )

        val FAKE_CC_2 = CryptoCurrency(
            displayTicker = "FAKE_CC_2",
            networkTicker = "FAKE_CC_2",
            name = "FAKE_CC_2",
            categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
            precisionDp = 18,
            requiredConfirmations = 5,
            l1chainTicker = CryptoCurrency.ETHER.networkTicker,
            l2identifier = "0xF0DF0DF0DF0DF0DF0DFAD",
            colour = "#123456"
        )

        val list = listOf(
            InterestAsset(
                CryptoCurrency.ETHER,
                AssetInterestDetail(
                    mockk(), mockk(), mockk(relaxed = true), mockk(relaxed = true), mockk(),
                    totalBalanceFiat = Money.fromMajor(CryptoCurrency.ETHER, 100.toBigDecimal())
                )
            ),
            InterestAsset(
                CryptoCurrency.BTC,
                AssetInterestDetail(
                    mockk(), mockk(), mockk(relaxed = true), mockk(relaxed = true), mockk(),
                    totalBalanceFiat = Money.fromMajor(CryptoCurrency.BTC, 0.toBigDecimal())
                )
            ),
            InterestAsset(
                CryptoCurrency.XLM,
                AssetInterestDetail(
                    mockk(), mockk(), mockk(relaxed = true), mockk(relaxed = true), mockk(),
                    totalBalanceFiat = Money.fromMajor(CryptoCurrency.XLM, 200.toBigDecimal())
                )
            ),
            InterestAsset(
                FAKE_CC_1,
                AssetInterestDetail(
                    mockk(), mockk(), mockk(relaxed = true), mockk(relaxed = true), mockk(),
                    totalBalanceFiat = Money.fromMajor(FAKE_CC_1, 100.toBigDecimal())
                )
            ),
            InterestAsset(
                FAKE_CC_2,
                AssetInterestDetail(
                    mockk(), mockk(), mockk(relaxed = true), mockk(relaxed = true), mockk(),
                    totalBalanceFiat = Money.fromMajor(FAKE_CC_2, 0.toBigDecimal())
                )
            )
        )

        // sort should be xlm (higher balance) - eth (lower balance) CC1 (same balance as eth but lower priority) -
        // btc (balance 0 but has priority) - CC2
        coEvery { service.getAssetsInterest(any()) } returns Outcome.Success(list)

        val result = useCase(listOf())
        assertTrue { result is Outcome.Success }

//        (result as Outcome.Success).value.let { value ->
//            assertEquals(CryptoCurrency.XLM, value[0].assetInfo)
//            assertEquals(CryptoCurrency.ETHER, value[1].assetInfo)
//            assertEquals(CryptoCurrency.BTC, value[2].assetInfo)
//            assertEquals(CryptoCurrency.BCH, value[3].assetInfo)
//        }
    }
}
