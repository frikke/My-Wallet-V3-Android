package piuk.blockchain.android.ui.linkbank.presentation.yapily.permission

import app.cash.turbine.test
import com.blockchain.core.payments.model.YapilyInstitution
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
import org.junit.Test
import piuk.blockchain.android.ui.linkbank.domain.yapily.usecase.GetSafeConnectTosLinkUseCase
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class YapilyPermissionViewModelTest {
    private val getSafeConnectTosLinkUseCase = mockk<GetSafeConnectTosLinkUseCase>()
    private lateinit var viewModel: YapilyPermissionViewModel
    private val tosLink = "TosLink"

    private val institution = mockk<YapilyInstitution>()
    private val args = YapilyPermissionArgs(
        institution = institution,
        entity = "", authSource = mockk()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        viewModel = YapilyPermissionViewModel(
            getSafeConnectTosLinkUseCase
        )

        coEvery { getSafeConnectTosLinkUseCase() } returns tosLink
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `WHEN GetTermsOfServiceLink is triggered, THEN UseCase should be called and tos should be updated`() =
        runTest {
            viewModel.viewState.test {
                viewModel.onIntent(YapilyPermissionIntents.GetTermsOfServiceLink)

                coVerify(exactly = 1) { getSafeConnectTosLinkUseCase() }

                assertEquals(tosLink, expectMostRecentItem().termsOfServiceLink)
            }
        }

    @Test
    fun `WHEN ApproveClicked is triggered, THEN AgreementAccepted should be triggered`() =
        runTest {

            viewModel.viewCreated(args)

            viewModel.navigationEventFlow.test {
                viewModel.onIntent(YapilyPermissionIntents.ApproveClicked)

                assertEquals(YapilyPermissionNavigationEvent.AgreementAccepted(institution), expectMostRecentItem())
            }
        }


    @Test
    fun `WHEN DenyClicked is triggered, THEN AgreementDenied should be triggered`() =
        runTest {
            viewModel.navigationEventFlow.test {
                viewModel.onIntent(YapilyPermissionIntents.DenyClicked)

                assertEquals(YapilyPermissionNavigationEvent.AgreementDenied, expectMostRecentItem())
            }
        }


}
