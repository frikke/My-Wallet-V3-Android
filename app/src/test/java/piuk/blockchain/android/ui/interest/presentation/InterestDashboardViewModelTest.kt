package piuk.blockchain.android.ui.interest.presentation

import app.cash.turbine.test
import com.blockchain.coincore.impl.CryptoAccountTradingGroup
import com.blockchain.coincore.impl.CryptoInterestAccount
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.outcome.Outcome
import info.blockchain.balance.CryptoCurrency
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.interest.domain.model.InterestAsset
import piuk.blockchain.android.ui.interest.domain.model.InterestDashboard
import piuk.blockchain.android.ui.interest.domain.usecase.GetAccountGroupUseCase
import piuk.blockchain.android.ui.interest.domain.usecase.GetAssetsInterestUseCase
import piuk.blockchain.android.ui.interest.domain.usecase.GetInterestDashboardUseCase

@ExperimentalCoroutinesApi
class InterestDashboardViewModelTest {
    private val getAssetInterestInfoUseCase = mockk<GetAssetsInterestUseCase>()
    private val getInterestDetailUseCase = mockk<GetInterestDashboardUseCase>()
    private val getAccountGroupUseCase = mockk<GetAccountGroupUseCase>()

    private lateinit var viewModel: InterestDashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        viewModel = InterestDashboardViewModel(
            getAssetInterestInfoUseCase,
            getInterestDetailUseCase,
            getAccountGroupUseCase
        )

        coEvery { getInterestDetailUseCase() } returns
            Outcome.Success(
                InterestDashboard(
                    KycTiers.default(),
                    listOf(CryptoCurrency.BTC, CryptoCurrency.ETHER, CryptoCurrency.BCH)
                )
            )

        coEvery { getAssetInterestInfoUseCase(any()) } returns
            Outcome.Success(
                listOf(
                    InterestAsset(CryptoCurrency.BTC, mockk()),
                    InterestAsset(CryptoCurrency.ETHER, mockk()),
                    InterestAsset(CryptoCurrency.BCH, mockk())
                )
            )

        coEvery { getAccountGroupUseCase(any(), any()) } returns
            Outcome.Success(
                CryptoAccountTradingGroup(
                    "label", listOf(mockk<CryptoInterestAccount>())
                )
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `WHEN LoadDashboard is triggered, THEN isLoading should be true, getInterestDetail should be called`() = runTest {
        coEvery { getInterestDetailUseCase() } returns mockk()

        viewModel.viewState.test {
            viewModel.onIntent(InterestDashboardIntents.LoadDashboard)

            assertEquals(true, expectMostRecentItem().isLoading)

            coVerify(exactly = 1) { getInterestDetailUseCase() }
        }
    }

    @Test
    fun `GIVEN getInterestDetail fails, WHEN LoadDashboard is triggered, THEN isError should be true`() = runTest {
        coEvery { getInterestDetailUseCase() } returns Outcome.Failure(Throwable())

        viewModel.viewState.test {
            viewModel.onIntent(InterestDashboardIntents.LoadDashboard)

            assertEquals(true, expectMostRecentItem().isError)
        }
    }

    @Test
    fun `GIVEN getInterestDetail OK, WHEN LoadDashboard is triggered, THEN getAssetInterestInfo should be called`() =
        runTest {
            viewModel.viewState.test {
                viewModel.onIntent(InterestDashboardIntents.LoadDashboard)

                coVerify(exactly = 1) { getAssetInterestInfoUseCase(any()) }

                expectMostRecentItem().let {
                    assertEquals(false, it.isLoading)
                    assertEquals(false, it.isError)
                    assertEquals(false, it.isKycGold)
                    assertEquals(3, it.data.size)
                }
            }
        }

    @Test
    fun `WHEN FilterData is triggered, THEN data should be filtered`() = runTest {
        viewModel.viewState.test {
            viewModel.onIntent(InterestDashboardIntents.LoadDashboard)

            viewModel.onIntent(InterestDashboardIntents.FilterData("bitc"))

            assertEquals(2, expectMostRecentItem().data.size) // BITCoin and BITCoin cash
        }
    }

    @Test
    fun `GIVEN hasBalance, WHEN InterestItemClicked is triggered, THEN InterestSummarySheet should be triggered`() =
        runTest {
            viewModel.navigationEventFlow.test {
                viewModel.onIntent(InterestDashboardIntents.LoadDashboard)

                viewModel.onIntent(InterestDashboardIntents.InterestItemClicked(CryptoCurrency.BTC, true))

                assertTrue { expectMostRecentItem() is InterestDashboardNavigationEvent.InterestSummary }
            }
        }

    @Test
    fun `GIVEN hasBalance false, WHEN InterestItemClicked is triggered, THEN TransactionFlow should be triggered`() =
        runTest {
            viewModel.navigationEventFlow.test {
                viewModel.onIntent(InterestDashboardIntents.LoadDashboard)

                viewModel.onIntent(InterestDashboardIntents.InterestItemClicked(CryptoCurrency.BTC, false))

                assertTrue { expectMostRecentItem() is InterestDashboardNavigationEvent.InterestDeposit }
            }
        }

    @Test
    fun `WHEN StartKyc intent is triggered, THEN StartKyc nav should be triggered`() =
        runTest {
            viewModel.navigationEventFlow.test {
                viewModel.onIntent(InterestDashboardIntents.LoadDashboard)

                viewModel.onIntent(InterestDashboardIntents.StartKyc)

                assertTrue { expectMostRecentItem() is InterestDashboardNavigationEvent.StartKyc }
            }
        }
}
