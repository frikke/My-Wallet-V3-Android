package piuk.blockchain.android.ui.interest.domain

import app.cash.turbine.test
import com.blockchain.core.interest.domain.InterestService
import com.blockchain.core.interest.domain.model.InterestAccountBalance
import com.blockchain.core.interest.domain.model.InterestEligibility
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.testutils.USD
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency.BTC
import info.blockchain.balance.CryptoCurrency.ETHER
import info.blockchain.balance.Money
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.interest.domain.usecase.GetInterestDashboardUseCase

@ExperimentalCoroutinesApi
class GetInterestDashboardUseCaseTest {

    private val interestService = mockk<InterestService>()
    private val exchangeRatesManager = mockk<ExchangeRatesDataManager>()
    private val useCase = GetInterestDashboardUseCase(interestService, exchangeRatesManager)

    private val assets = listOf(BTC, ETHER)
    private val dataResourceAssets = MutableSharedFlow<DataResource<List<AssetInfo>>>()
    private val dataResourceBalanceBtc = MutableSharedFlow<DataResource<InterestAccountBalance>>()
    private val dataResourceBalanceEth = MutableSharedFlow<DataResource<InterestAccountBalance>>()
    private val dataResourceInterestRateBtc = MutableSharedFlow<DataResource<Double>>()
    private val dataResourceInterestRateEth = MutableSharedFlow<DataResource<Double>>()
    private val dataResourceEligibilityBtc = MutableSharedFlow<DataResource<InterestEligibility>>()
    private val dataResourceEligibilityEth = MutableSharedFlow<DataResource<InterestEligibility>>()
    private val dataResourceExchangeRateBtc = MutableSharedFlow<DataResource<ExchangeRate>>()
    private val dataResourceExchangeRateEth = MutableSharedFlow<DataResource<ExchangeRate>>()

    private val interestAccountBalanceBtc = InterestAccountBalance(
        totalInterest = Money.fromMajor(BTC, 200.toBigDecimal()),
        totalBalance = Money.fromMajor(BTC, 200.toBigDecimal()),
        pendingInterest = mockk(), pendingDeposit = mockk(), lockedBalance = mockk(),
        hasTransactions = mockk(relaxed = true)
    )
    private val interestAccountBalanceEth = InterestAccountBalance(
        totalInterest = Money.fromMajor(ETHER, 300.toBigDecimal()),
        totalBalance = Money.fromMajor(ETHER, 300.toBigDecimal()),
        pendingInterest = mockk(), pendingDeposit = mockk(), lockedBalance = mockk(),
        hasTransactions = mockk(relaxed = true)
    )

    private val exchangeRateBtc = ExchangeRate(1.toBigDecimal(), BTC, USD)
    private val exchangeRateEth = ExchangeRate(1.toBigDecimal(), ETHER, USD)

    private val interestRateBtc: Double = 1.0
    private val interestRateEth: Double = 1.0

    private val eligibilityBtc = InterestEligibility.Eligible
    private val eligibilityEth = InterestEligibility.Ineligible.REGION

    @Before
    fun setUp() {
        every { interestService.getAvailableAssetsForInterestFlow() } returns dataResourceAssets

        every { interestService.getBalanceForFlow(BTC) } returns dataResourceBalanceBtc
        every { interestService.getBalanceForFlow(ETHER) } returns dataResourceBalanceEth

        every { interestService.getInterestRateFlow(BTC) } returns dataResourceInterestRateBtc
        every { interestService.getInterestRateFlow(ETHER) } returns dataResourceInterestRateEth

        every { interestService.getEligibilityForAssetFlow(BTC) } returns dataResourceEligibilityBtc
        every { interestService.getEligibilityForAssetFlow(ETHER) } returns dataResourceEligibilityEth

        every { exchangeRatesManager.exchangeRateToUserFiatFlow(BTC) } returns dataResourceExchangeRateBtc
        every { exchangeRatesManager.exchangeRateToUserFiatFlow(ETHER) } returns dataResourceExchangeRateEth
    }

    @Test
    fun `GIVEN all data is ok, WHEN useCase is called, THEN interest data should be returned and sorted correctly`() =
        runTest {
            useCase().test {
                dataResourceAssets.emit(DataResource.Data(assets))

                dataResourceBalanceBtc.emit(DataResource.Data(interestAccountBalanceBtc))
                dataResourceInterestRateBtc.emit(DataResource.Data(interestRateBtc))
                dataResourceEligibilityBtc.emit(DataResource.Data(eligibilityBtc))
                dataResourceExchangeRateBtc.emit(DataResource.Data(exchangeRateBtc))

                dataResourceBalanceEth.emit(DataResource.Data(interestAccountBalanceEth))
                dataResourceInterestRateEth.emit(DataResource.Data(interestRateEth))
                dataResourceEligibilityEth.emit(DataResource.Data(eligibilityEth))
                dataResourceExchangeRateEth.emit(DataResource.Data(exchangeRateEth))

                expectMostRecentItem().run {
                    assert(this is DataResource.Data)

                    (this as DataResource.Data).run {
                        assertEquals(2, data.size)

                        assertEquals(ETHER, data[0].assetInfo)
                        assertEquals(BTC, data[1].assetInfo)
                    }
                }
            }
        }

    @Test
    fun `GIVEN one of eth data is error, WHEN useCase is called, THEN interest data should be returned and sorted correctly, eth interestDetail should be null`() =
        runTest {
            useCase().test {
                dataResourceAssets.emit(DataResource.Data(assets))

                dataResourceBalanceBtc.emit(DataResource.Data(interestAccountBalanceBtc))
                dataResourceInterestRateBtc.emit(DataResource.Data(interestRateBtc))
                dataResourceEligibilityBtc.emit(DataResource.Data(eligibilityBtc))
                dataResourceExchangeRateBtc.emit(DataResource.Data(exchangeRateBtc))

                dataResourceBalanceEth.emit(DataResource.Data(interestAccountBalanceEth))
                dataResourceInterestRateEth.emit(DataResource.Data(interestRateEth))
                dataResourceEligibilityEth.emit(DataResource.Data(eligibilityEth))
                dataResourceExchangeRateEth.emit(DataResource.Error(mockk()))

                expectMostRecentItem().run {
                    assert(this is DataResource.Data)

                    (this as DataResource.Data).run {
                        assertEquals(2, data.size)

                        assertEquals(BTC, data[0].assetInfo)
                        assertEquals(ETHER, data[1].assetInfo)

                        assertNull(data[1].interestDetail)
                    }
                }
            }
        }
}
