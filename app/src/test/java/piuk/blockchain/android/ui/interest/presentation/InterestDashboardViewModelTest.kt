package piuk.blockchain.android.ui.interest.presentation

import app.cash.turbine.test
import com.blockchain.coincore.impl.CryptoAccountCustodialSingleGroup
import com.blockchain.coincore.impl.CustodialInterestAccount
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.kyc.domain.model.KycTiers
import com.blockchain.data.DataResource
import com.blockchain.outcome.Outcome
import info.blockchain.balance.CryptoCurrency
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.interest.domain.model.InterestAsset
import piuk.blockchain.android.ui.interest.domain.usecase.GetAccountGroupUseCase
import piuk.blockchain.android.ui.interest.domain.usecase.GetInterestDashboardUseCase

@ExperimentalCoroutinesApi
class InterestDashboardViewModelTest {
    private val getInterestDashboardUseCase = mockk<GetInterestDashboardUseCase>()
    private val getAccountGroupUseCase = mockk<GetAccountGroupUseCase>()
    private val kycService = mockk<KycService>()

    private lateinit var viewModel: InterestDashboardViewModel

    private val kycTiers = mockk<KycTiers>()
    private val interestAssets = listOf(
        InterestAsset(CryptoCurrency.BTC, mockk()),
        InterestAsset(CryptoCurrency.ETHER, mockk()),
        InterestAsset(CryptoCurrency.BCH, mockk())
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        viewModel = InterestDashboardViewModel(
            kycService = kycService,
            getInterestDashboardUseCase = getInterestDashboardUseCase,
            getAccountGroupUseCase = getAccountGroupUseCase
        )

        coEvery { getAccountGroupUseCase(any(), any()) } returns
            Outcome.Success(
                CryptoAccountCustodialSingleGroup(
                    "label",
                    listOf(mockk<CustodialInterestAccount>())
                )
            )

        every { kycTiers.isApprovedFor(KycTier.GOLD) } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN kyc not Gold, WHEN LoadDashboard is triggered, THEN isKycGold should be false`() =
        runTest {
            every { kycTiers.isApprovedFor(KycTier.GOLD) } returns false

            /**
             * emits Loading -> Data
             */
            val dataResource = MutableSharedFlow<DataResource<KycTiers>>()
            coEvery { kycService.getTiers() } returns dataResource

            viewModel.viewState.test {
                expectMostRecentItem()
                viewModel.onIntent(InterestDashboardIntents.LoadDashboard)

                // loading
                dataResource.emit(DataResource.Loading)
                awaitItem().run {
                    isLoading shouldBeEqualTo true
                }

                // data
                dataResource.emit(DataResource.Data(kycTiers))
                awaitItem().run {
                    isLoading shouldBeEqualTo false
                    isError shouldBeEqualTo false
                    isKycGold shouldBeEqualTo false
                }
            }
        }

    @Test
    fun `GIVEN getTiers fails, WHEN LoadDashboard is triggered, THEN isError should be true`() =
        runTest {
            /**
             * emits Loading -> Error
             */
            val dataResource = MutableSharedFlow<DataResource<KycTiers>>()
            coEvery { kycService.getTiers() } returns dataResource

            viewModel.viewState.test {
                expectMostRecentItem()
                viewModel.onIntent(InterestDashboardIntents.LoadDashboard)

                // loading
                dataResource.emit(DataResource.Loading)
                awaitItem().run {
                    isLoading shouldBeEqualTo true
                }

                // data
                dataResource.emit(DataResource.Error(mockk()))
                awaitItem().run {
                    isLoading shouldBeEqualTo false
                    isError shouldBeEqualTo true
                }
            }
        }

    @Test
    fun `WHEN LoadDashboard is triggered, THEN flow should go according to data received`() =
        runTest {
            /**
             * emits Loading -> Data
             */
            val dataResourceKyc = MutableSharedFlow<DataResource<KycTiers>>()
            coEvery { kycService.getTiers() } returns dataResourceKyc

            /**
             * emits Loading - cache Data - Loading - remote Data - Error
             */
            val dataResourceInterest = MutableSharedFlow<DataResource<List<InterestAsset>>>()
            every { getInterestDashboardUseCase() } returns dataResourceInterest

            viewModel.viewState.test {
                expectMostRecentItem()
                viewModel.onIntent(InterestDashboardIntents.LoadDashboard)

                // loading
                dataResourceKyc.emit(DataResource.Loading)
                awaitItem().run {
                    isLoading shouldBeEqualTo true
                }

                // data kyc
                dataResourceKyc.emit(DataResource.Data(kycTiers))
                // kyc gold -> interest use case called -> triggers loading first
                verify { getInterestDashboardUseCase() }
                dataResourceInterest.emit(DataResource.Loading)
                // because data is empty so far - loading should be true
                // trying to emit viewState with loading:true will not emit anything since the new object equals the old
                expectNoEvents()

                // interest data
                dataResourceInterest.emit(DataResource.Data(interestAssets))
                awaitItem().run {
                    isLoading shouldBeEqualTo false
                    isError shouldBeEqualTo false
                    isKycGold shouldBeEqualTo true
                    data shouldBeEqualTo interestAssets
                }

                // interest is getting data from remote now -> triggers loading
                dataResourceInterest.emit(DataResource.Loading)
                // because now data is not empty - loading should be false
                // trying to emit viewState with loading:false will not emit anything since the new object equals the old
                expectNoEvents()

                // interest remote data
                dataResourceInterest.emit(DataResource.Data(interestAssets))
                // trying to emit viewState with same data will not emit anything since the new object equals the old
                expectNoEvents()

                // error case
                dataResourceInterest.emit(DataResource.Error(mockk()))
                awaitItem().run {
                    isLoading shouldBeEqualTo false
                    isError shouldBeEqualTo true
                }
            }
        }

    @Test
    fun `WHEN FilterData is triggered, THEN data should be filtered`() = runTest {
        coEvery { kycService.getTiers() } returns flowOf(DataResource.Data(kycTiers))
        every { getInterestDashboardUseCase() } returns flowOf(DataResource.Data(interestAssets))

        viewModel.viewState.test {
            viewModel.onIntent(InterestDashboardIntents.LoadDashboard)

            viewModel.onIntent(InterestDashboardIntents.FilterData("bitc"))

            expectMostRecentItem().run {
                assertEquals(2, data.size) // BITCoin and BITCoin cash
                assertEquals(true, data.any { it.assetInfo == CryptoCurrency.BTC })
                assertEquals(true, data.any { it.assetInfo == CryptoCurrency.BCH })
            }
        }
    }

    @Test
    fun `GIVEN hasBalance, WHEN InterestItemClicked is triggered, THEN InterestSummarySheet should be triggered`() =
        runTest {
            coEvery { kycService.getTiers() } returns flowOf(DataResource.Data(kycTiers))
            every { getInterestDashboardUseCase() } returns flowOf(DataResource.Data(interestAssets))

            viewModel.navigationEventFlow.test {
                viewModel.onIntent(InterestDashboardIntents.LoadDashboard)

                viewModel.onIntent(InterestDashboardIntents.InterestItemClicked(CryptoCurrency.BTC, true))

                assertTrue { expectMostRecentItem() is InterestDashboardNavigationEvent.InterestSummary }
            }
        }

    @Test
    fun `GIVEN hasBalance false, WHEN InterestItemClicked is triggered, THEN TransactionFlow should be triggered`() =
        runTest {
            coEvery { kycService.getTiers() } returns flowOf(DataResource.Data(kycTiers))
            every { getInterestDashboardUseCase() } returns flowOf(DataResource.Data(interestAssets))

            viewModel.navigationEventFlow.test {
                viewModel.onIntent(InterestDashboardIntents.LoadDashboard)

                viewModel.onIntent(InterestDashboardIntents.InterestItemClicked(CryptoCurrency.BTC, false))

                assertTrue { expectMostRecentItem() is InterestDashboardNavigationEvent.InterestDeposit }
            }
        }

    @Test
    fun `WHEN StartKyc intent is triggered, THEN StartKyc nav should be triggered`() =
        runTest {
            coEvery { kycService.getTiers() } returns flowOf(DataResource.Data(kycTiers))
            every { getInterestDashboardUseCase() } returns flowOf(DataResource.Data(interestAssets))

            viewModel.navigationEventFlow.test {
                viewModel.onIntent(InterestDashboardIntents.LoadDashboard)

                viewModel.onIntent(InterestDashboardIntents.StartKyc)

                assertTrue { expectMostRecentItem() is InterestDashboardNavigationEvent.StartKyc }
            }
        }
}
