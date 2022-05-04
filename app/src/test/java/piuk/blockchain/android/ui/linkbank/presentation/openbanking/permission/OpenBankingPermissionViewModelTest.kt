package piuk.blockchain.android.ui.linkbank.presentation.openbanking.permission

import app.cash.turbine.test
import com.blockchain.core.payments.model.YapilyInstitution
import com.blockchain.testutils.CoroutineTestRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.linkbank.domain.openbanking.usecase.GetSafeConnectTosLinkUseCase

@ExperimentalCoroutinesApi
class OpenBankingPermissionViewModelTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    var coroutineTestRule = CoroutineTestRule()

    private val getSafeConnectTosLinkUseCase = mockk<GetSafeConnectTosLinkUseCase>()
    private lateinit var viewModel: OpenBankingPermissionViewModel
    private val tosLink = "TosLink"

    private val institution = mockk<YapilyInstitution>()
    private val args = OpenBankingPermissionArgs(
        institution = institution,
        entity = "", authSource = mockk()
    )

    @Before
    fun setUp() {
        viewModel = OpenBankingPermissionViewModel(
            getSafeConnectTosLinkUseCase
        )

        coEvery { getSafeConnectTosLinkUseCase() } returns tosLink
    }

    @Test
    fun `WHEN GetTermsOfServiceLink is triggered, THEN UseCase should be called and tos should be updated`() =
        runTest {
            viewModel.viewState.test {
                viewModel.onIntent(OpenBankingPermissionIntents.GetTermsOfServiceLink)

                coVerify(exactly = 1) { getSafeConnectTosLinkUseCase() }

                assertEquals(tosLink, expectMostRecentItem().termsOfServiceLink)
            }
        }

    @Test
    fun `WHEN ApproveClicked is triggered, THEN AgreementAccepted should be triggered`() =
        runTest {

            viewModel.viewCreated(args)

            viewModel.navigationEventFlow.test {
                viewModel.onIntent(OpenBankingPermissionIntents.ApproveClicked)

                assertEquals(OpenBankingPermissionNavEvent.AgreementAccepted(institution), expectMostRecentItem())
            }
        }

    @Test
    fun `WHEN DenyClicked is triggered, THEN AgreementDenied should be triggered`() =
        runTest {
            viewModel.navigationEventFlow.test {
                viewModel.onIntent(OpenBankingPermissionIntents.DenyClicked)

                assertEquals(OpenBankingPermissionNavEvent.AgreementDenied, expectMostRecentItem())
            }
        }
}
