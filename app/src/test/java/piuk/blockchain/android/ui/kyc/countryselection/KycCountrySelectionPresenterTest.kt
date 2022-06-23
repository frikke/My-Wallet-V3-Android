package piuk.blockchain.android.ui.kyc.countryselection

import com.blockchain.android.testutils.rxInit
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.eligibility.model.EligibilityError
import com.blockchain.domain.eligibility.model.GetRegionScope
import com.blockchain.domain.eligibility.model.Region
import com.blockchain.outcome.Outcome
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.kyc.countryselection.models.CountrySelectionState
import piuk.blockchain.android.ui.kyc.countryselection.util.CountryDisplayModel

class KycCountrySelectionPresenterTest {

    private lateinit var subject: KycCountrySelectionPresenter
    private val view: KycCountrySelectionView = mock()
    private val eligibilityService: EligibilityService = mock()

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = KycCountrySelectionPresenter(eligibilityService)
        subject.initView(view)
    }

    @Test
    fun `onViewReady error loading countries`() = runTest {
        // Arrange
        whenever(view.regionType).thenReturn(RegionType.Country)
        whenever(eligibilityService.getCountriesList(GetRegionScope.None)).thenReturn(Outcome.Failure(EligibilityError.RequestFailed(null)))
        // Act
        subject.onViewReady()
        // Assert
        verify(eligibilityService).getCountriesList(GetRegionScope.None)
        verify(view).renderUiState(CountrySelectionState.Loading)
        verify(view).renderUiState(any<CountrySelectionState.Error>())
    }

    @Test
    fun `onViewReady loading countries success`() = runTest {
        // Arrange
        whenever(view.regionType).thenReturn(RegionType.Country)
        whenever(eligibilityService.getCountriesList(GetRegionScope.None))
            .thenReturn(Outcome.Success(emptyList()))
        // Act
        subject.onViewReady()
        // Assert
        verify(eligibilityService).getCountriesList(GetRegionScope.None)
        verify(view).renderUiState(CountrySelectionState.Loading)
        verify(view).renderUiState(CountrySelectionState.Data(emptyList()))
    }

    @Test
    fun `onViewReady loading states success`() = runTest {
        // Arrange
        whenever(view.regionType).thenReturn(RegionType.State)
        whenever(eligibilityService.getStatesList("US", GetRegionScope.None))
            .thenReturn(Outcome.Success(emptyList()))
        // Act
        subject.onViewReady()
        // Assert
        verify(eligibilityService).getStatesList("US", GetRegionScope.None)
        verify(view).renderUiState(CountrySelectionState.Loading)
        verify(view).renderUiState(CountrySelectionState.Data(emptyList()))
    }

    @Test
    fun `onRegionSelected requires state selection`() = runTest {
        // Arrange
        whenever(view.regionType).thenReturn(RegionType.Country)
        whenever(eligibilityService.getCountriesList(GetRegionScope.None))
            .thenReturn(Outcome.Success(emptyList()))
        val countryDisplayModel = CountryDisplayModel(
            name = "United States",
            countryCode = "US"
        )
        // Act
        subject.onRegionSelected(countryDisplayModel)
        // Assert
        verify(eligibilityService).getCountriesList(GetRegionScope.None)
        verify(view).requiresStateSelection()
    }

    @Test
    fun `onRegionSelected state not found, not in kyc region`() = runTest {
        // Arrange
        whenever(view.regionType).thenReturn(RegionType.State)
        whenever(eligibilityService.getStatesList("US", GetRegionScope.None))
            .thenReturn(Outcome.Success(emptyList()))
        val countryDisplayModel = CountryDisplayModel(
            name = "United States",
            countryCode = "US",
            isState = true,
            state = "US-AL"
        )
        // Act
        subject.onRegionSelected(countryDisplayModel)
        // Assert
        verify(eligibilityService).getStatesList("US", GetRegionScope.None)
        verify(view).invalidCountry(countryDisplayModel)
    }

    @Test
    fun `onRegionSelected state found, in kyc region`() = runTest {
        // Arrange
        whenever(view.regionType).thenReturn(RegionType.State)
        val countryCode = "US"
        whenever(eligibilityService.getStatesList("US", GetRegionScope.None))
            .thenReturn(
                Outcome.Success(
                    listOf(
                        Region.State(
                            stateCode = "US-AL",
                            name = "Alabama",
                            isKycAllowed = true,
                            countryCode = "US"
                        )
                    )
                )
            )
        val countryDisplayModel = CountryDisplayModel(
            name = "California",
            countryCode = "US",
            isState = true,
            state = "US-AL"
        )
        // Act
        subject.onRegionSelected(countryDisplayModel)
        // Assert
        verify(eligibilityService).getStatesList("US", GetRegionScope.None)
        verify(view).continueFlow(countryCode, "US-AL", "California")
    }

    @Test
    fun `onRegionSelected country found`() = runTest {
        // Arrange
        whenever(view.regionType).thenReturn(RegionType.Country)
        val countryCode = "UK"
        val countryList =
            listOf(Region.Country("UK", "United Kingdom", true, emptyList()))
        whenever(eligibilityService.getCountriesList(GetRegionScope.None))
            .thenReturn(Outcome.Success(countryList))
        val countryDisplayModel = CountryDisplayModel(
            name = "United Kingdom",
            countryCode = "UK"
        )
        // Act
        subject.onRegionSelected(countryDisplayModel)
        // Assert
        verify(eligibilityService).getCountriesList(GetRegionScope.None)
        verify(view).continueFlow(countryCode, null, null)
    }

    @Test
    fun `onRegionSelected country found but is US so requires state selection`() = runTest {
        // Arrange
        whenever(view.regionType).thenReturn(RegionType.Country)
        val countryList =
            listOf(Region.Country("US", "United States", true, emptyList()))
        whenever(eligibilityService.getCountriesList(GetRegionScope.None))
            .thenReturn(Outcome.Success(countryList))
        val countryDisplayModel = CountryDisplayModel(
            name = "United States",
            countryCode = "US"
        )
        // Act
        subject.onRegionSelected(countryDisplayModel)
        // Assert
        verify(eligibilityService).getCountriesList(GetRegionScope.None)
        verify(view).requiresStateSelection()
    }
}
