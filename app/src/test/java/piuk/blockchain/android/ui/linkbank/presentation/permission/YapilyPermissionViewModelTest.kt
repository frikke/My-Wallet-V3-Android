package piuk.blockchain.android.ui.linkbank.presentation.permission

import app.cash.turbine.test
import com.blockchain.outcome.Outcome
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.File
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.fileutils.domain.usecase.DownloadFileUseCase
import piuk.blockchain.android.ui.linkbank.domain.yapily.SafeConnectService

@ExperimentalCoroutinesApi
class YapilyPermissionViewModelTest {
    private val safeConnectRemoteConfig = mockk<SafeConnectService>()
    private val downloadFileUseCase = mockk<DownloadFileUseCase>()

    private lateinit var viewModel: YapilyPermissionViewModel

    private val path = "absolute/path/to/pdf"
    private val link = "fb/link/to/pdf"
    private val data = Outcome.Success(mockk<File>())

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        viewModel = YapilyPermissionViewModel(
            safeConnectRemoteConfig,
            downloadFileUseCase
        )

        coEvery { safeConnectRemoteConfig.getTosPdfLink() } returns link
        coEvery { downloadFileUseCase(any(), any()) } returns data
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `WHEN DownloadTermsOfService is triggered, THEN downloadFileUseCase should be called`() =
        runTest {
            viewModel.onIntent(YapilyPermissionIntents.DownloadTermsOfService(path))

            coVerify(exactly = 1) { downloadFileUseCase(absolutePath = path, fileGsLink = link) }
        }

    @Test
    fun `WHEN downloadFileUseCase returns, THEN OpenFile should be triggered`() =
        runTest {
            viewModel.navigationEventFlow.test {
                viewModel.onIntent(YapilyPermissionIntents.DownloadTermsOfService(path))

                val expected = YapilyPermissionNavigationEvent.OpenFile(data.value)
                assertEquals(expected, expectMostRecentItem())
            }
        }
}
