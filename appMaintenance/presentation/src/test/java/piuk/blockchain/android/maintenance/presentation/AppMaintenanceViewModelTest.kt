package piuk.blockchain.android.maintenance.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import app.cash.turbine.test
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.maintenance.domain.model.AppMaintenanceStatus
import piuk.blockchain.android.maintenance.domain.model.UpdateLocation
import piuk.blockchain.android.maintenance.domain.usecase.GetAppMaintenanceConfigUseCase
import piuk.blockchain.android.maintenance.domain.usecase.IsDownloadInProgressUseCase
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class AppMaintenanceViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val getAppMaintenanceConfigUseCase = mockk<GetAppMaintenanceConfigUseCase>()
    private val isDownloadInProgressUseCase = mockk<IsDownloadInProgressUseCase>()

    private lateinit var viewModel: AppMaintenanceViewModel

    private val lifecycle = LifecycleRegistry(mockk())

    /**
     * !! important to have viewModel init AFTER setting dispatcher
     * because [MviViewModel.viewState] is initialized with a stateIn(viewModelScope)
     * which needs to be provided by Dispatchers.setMain beforehand
     */
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = AppMaintenanceViewModel(getAppMaintenanceConfigUseCase, isDownloadInProgressUseCase)
        lifecycle.addObserver(viewModel)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        lifecycle.removeObserver(viewModel)
    }

    @Test
    fun `GIVEN download not in progress, WHEN viewCreated, THEN do nothing`() = runTest {
        coEvery { isDownloadInProgressUseCase() } returns false

        viewModel.navigationEventFlow.test {
            viewModel.viewCreated(ModelConfigArgs.NoArgs)

            expectNoEvents()
        }
    }

    @Test
    fun `GIVEN download in progress, WHEN viewCreated, THEN nav LaunchAppUpdate should be triggered`() = runTest {
        coEvery { isDownloadInProgressUseCase() } returns true

        viewModel.navigationEventFlow.test {
            viewModel.viewCreated(ModelConfigArgs.NoArgs)

            assertEquals(AppMaintenanceNavigationEvent.LaunchAppUpdate, expectMostRecentItem())
        }
    }

    @Test
    fun `WHEN viewCreated, THEN nav LaunchAppUpdate should be triggered`() = runTest {
        coEvery { isDownloadInProgressUseCase() } returns true

        viewModel.navigationEventFlow.test {
            viewModel.viewCreated(ModelConfigArgs.NoArgs)

            assertEquals(AppMaintenanceNavigationEvent.LaunchAppUpdate, expectMostRecentItem())
        }
    }

    @Test
    fun `WHEN onResume, THEN getAppMaintenanceConfig should be called`() = runTest {
        coEvery { getAppMaintenanceConfigUseCase() } returns AppMaintenanceStatus.NonActionable.AllClear

        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        coVerify(exactly = 1) { getAppMaintenanceConfigUseCase() }
    }

    @Test
    fun `GIVEN status is Unknown, WHEN onResume, THEN ResumeAppFlow should be triggered`() = runTest {
        coEvery { getAppMaintenanceConfigUseCase() } returns AppMaintenanceStatus.NonActionable.Unknown

        viewModel.navigationEventFlow.test {
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            assertEquals(AppMaintenanceNavigationEvent.ResumeAppFlow, expectMostRecentItem())
        }
    }

    @Test
    fun `GIVEN status is AllClear, WHEN onResume, THEN ResumeAppFlow should be triggered`() = runTest {
        coEvery { getAppMaintenanceConfigUseCase() } returns AppMaintenanceStatus.NonActionable.AllClear

        viewModel.navigationEventFlow.test {
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            assertEquals(AppMaintenanceNavigationEvent.ResumeAppFlow, expectMostRecentItem())
        }
    }

    @Test
    fun `GIVEN status is SiteWideMaintenance, WHEN onResume, THEN viewState should be SITE_WIDE_MAINTENANCE`() =
        runTest {
            coEvery { getAppMaintenanceConfigUseCase() } returns
                AppMaintenanceStatus.Actionable.SiteWideMaintenance("")

            viewModel.viewState.test {
                lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

                assertEquals(
                    AppMaintenanceViewState(AppMaintenanceStatusUiState.SITE_WIDE_MAINTENANCE),
                    expectMostRecentItem()
                )
            }
        }

    @Test
    fun `GIVEN status is RedirectToWebsite, WHEN onResume, THEN viewState should be REDIRECT_TO_WEBSITE`() = runTest {
        coEvery { getAppMaintenanceConfigUseCase() } returns
            AppMaintenanceStatus.Actionable.RedirectToWebsite("")

        viewModel.viewState.test {
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            assertEquals(
                AppMaintenanceViewState(AppMaintenanceStatusUiState.REDIRECT_TO_WEBSITE), expectMostRecentItem()
            )
        }
    }

    @Test
    fun `GIVEN status is MandatoryUpdate, WHEN onResume, THEN viewState should be MANDATORY_UPDATE`() = runTest {
        coEvery { getAppMaintenanceConfigUseCase() } returns
            AppMaintenanceStatus.Actionable.MandatoryUpdate(UpdateLocation.InAppUpdate)

        viewModel.viewState.test {
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            assertEquals(
                AppMaintenanceViewState(AppMaintenanceStatusUiState.MANDATORY_UPDATE), expectMostRecentItem()
            )
        }
    }

    @Test
    fun `GIVEN status is OptionalUpdate, WHEN onResume, THEN viewState should be OPTIONAL_UPDATE`() = runTest {
        coEvery { getAppMaintenanceConfigUseCase() } returns
            AppMaintenanceStatus.Actionable.OptionalUpdate(UpdateLocation.InAppUpdate)

        viewModel.viewState.test {
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            assertEquals(
                AppMaintenanceViewState(AppMaintenanceStatusUiState.OPTIONAL_UPDATE), expectMostRecentItem()
            )
        }
    }

    @Test
    fun `GIVEN status is SiteWideMaintenance, WHEN ViewStatus intent, THEN OpenUrl should be triggered`() =
        runTest {
            coEvery { getAppMaintenanceConfigUseCase() } returns
                AppMaintenanceStatus.Actionable.SiteWideMaintenance("statusUrl")

            viewModel.navigationEventFlow.test {
                lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

                viewModel.onIntent(AppMaintenanceIntents.ViewStatus)

                assertEquals(AppMaintenanceNavigationEvent.OpenUrl("statusUrl"), expectMostRecentItem())
            }
        }

    @Test
    fun `GIVEN status is RedirectToWebsite, WHEN RedirectToWebsite intent, THEN OpenUrl should be triggered`() =
        runTest {
            coEvery { getAppMaintenanceConfigUseCase() } returns
                AppMaintenanceStatus.Actionable.RedirectToWebsite("website")

            viewModel.navigationEventFlow.test {
                lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

                viewModel.onIntent(AppMaintenanceIntents.RedirectToWebsite)

                assertEquals(AppMaintenanceNavigationEvent.OpenUrl("website"), expectMostRecentItem())
            }
        }

    @Test
    fun `GIVEN status is OptionalUpdate, WHEN SkipUpdate intent, THEN ResumeAppFlow should be triggered`() =
        runTest {
            coEvery { getAppMaintenanceConfigUseCase() } returns
                AppMaintenanceStatus.Actionable.OptionalUpdate(UpdateLocation.InAppUpdate)

            viewModel.navigationEventFlow.test {
                lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

                viewModel.onIntent(AppMaintenanceIntents.SkipUpdate)

                assertEquals(AppMaintenanceNavigationEvent.ResumeAppFlow, expectMostRecentItem())
            }
        }

    @Test
    fun `GIVEN status is MandatoryUpdate & update location is inapp, WHEN UpdateApp intent, THEN LaunchAppUpdate should be triggered`() =
        runTest {
            coEvery { getAppMaintenanceConfigUseCase() } returns
                AppMaintenanceStatus.Actionable.MandatoryUpdate(UpdateLocation.InAppUpdate)

            viewModel.navigationEventFlow.test {
                lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

                viewModel.onIntent(AppMaintenanceIntents.UpdateApp)

                assertEquals(AppMaintenanceNavigationEvent.LaunchAppUpdate, expectMostRecentItem())
            }
        }

    @Test
    fun `GIVEN status is MandatoryUpdate & update location is url, WHEN UpdateApp intent, THEN OpenUrl should be triggered`() =
        runTest {
            coEvery { getAppMaintenanceConfigUseCase() } returns
                AppMaintenanceStatus.Actionable.MandatoryUpdate(UpdateLocation.fromUrl("url"))

            viewModel.navigationEventFlow.test {
                lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

                viewModel.onIntent(AppMaintenanceIntents.UpdateApp)

                assertEquals(AppMaintenanceNavigationEvent.OpenUrl("url"), expectMostRecentItem())
            }
        }
}