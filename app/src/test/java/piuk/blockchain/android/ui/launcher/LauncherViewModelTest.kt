package piuk.blockchain.android.ui.launcher

import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.blockchain.enviroment.Environment
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.ReferralPrefs
import com.blockchain.preferences.SecurityPrefs
import com.blockchain.testutils.CoroutineTestRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import piuk.blockchain.android.maintenance.domain.model.AppMaintenanceStatus
import piuk.blockchain.android.maintenance.domain.usecase.GetAppMaintenanceConfigUseCase
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.utils.EncryptedPrefs
import piuk.blockchain.androidcore.utils.SessionPrefs

@RunWith(MockitoJUnitRunner::class)
class LauncherViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var coroutineTestRule = CoroutineTestRule()

    private val deepLinkPersistence: DeepLinkPersistence = mockk()
    private val environmentConfig: EnvironmentConfig = mockk()
    private val appUtil: AppUtil = mockk()
    private val sessionPrefs: SessionPrefs = mockk()
    private val authPrefs: AuthPrefs = mockk()
    private val securityPrefs: SecurityPrefs = mockk()
    private val referralPrefs: ReferralPrefs = mockk()
    private val encryptedPrefs: EncryptedPrefs = mockk()

    private val getAppMaintenanceConfigUseCase: GetAppMaintenanceConfigUseCase = mockk()

    private lateinit var viewModel: LauncherViewModel

    @Before
    fun setUp() {
        viewModel = LauncherViewModel(
            appUtil,
            deepLinkPersistence,
            environmentConfig,
            authPrefs,
            getAppMaintenanceConfigUseCase,
            sessionPrefs,
            securityPrefs,
            referralPrefs,
            encryptedPrefs
        )

        every { sessionPrefs.keySchemeUrl = any() } just Runs
        every { deepLinkPersistence.pushDeepLink(any()) } just Runs
        every { referralPrefs.referralSuccessTitle = any() } just Runs
        every { referralPrefs.referralSuccessBody = any() } just Runs
        every { sessionPrefs.metadataUri = any() } just Runs
        every { securityPrefs.setIsUnderTest() } just Runs
        every { environmentConfig.environment } returns Environment.STAGING
        every { environmentConfig.environment } returns Environment.STAGING
        every { encryptedPrefs.hasBackup() } returns false
        every { authPrefs.walletGuid } returns ""
        every { authPrefs.pinId } returns ""
        every { appUtil.clearCredentialsAndRestart() } just Runs
    }

    @Test
    fun `GIVEN bitcoin scheme, WHEN viewCreated is called, THEN keySchemeUrl should be bitcoin uri`() = runTest {
        // Arrange
        val args = LauncherState(
            action = Intent.ACTION_VIEW,
            scheme = "bitcoin",
            data = "bitcoin uri"
        )

        every { authPrefs.walletGuid } returns WALLET_GUID
        every { authPrefs.pinId } returns PIN_ID
        coEvery { getAppMaintenanceConfigUseCase() } returns AppMaintenanceStatus.NonActionable.AllClear

        // Act
        viewModel.viewCreated(args)

        // Assert
        verify { sessionPrefs.keySchemeUrl = "bitcoin uri" }
    }

    @Test
    fun `GIVEN dataString blockchain, WHEN viewCreated is called, THEN metadataUri should be blockchain`() {
        // Arrange
        val args = LauncherState(
            action = Intent.ACTION_VIEW,
            dataString = "blockchain"
        )

        every { authPrefs.walletGuid } returns ""
        every { authPrefs.pinId } returns ""
        coEvery { getAppMaintenanceConfigUseCase() } returns AppMaintenanceStatus.NonActionable.AllClear

        // Act
        viewModel.viewCreated(args)

        // Assert
        verify { sessionPrefs.metadataUri = "blockchain" }
    }

    @Test
    fun `GIVEN invalid guid, WHEN viewCreated is called, THEN CorruptPayload is called`() = runTest {
        // Arrange
        val args = LauncherState()

        every { authPrefs.walletGuid } returns INVALID_WALLET_GUID
        every { authPrefs.pinId } returns PIN_ID
        every { encryptedPrefs.hasBackup() } returns false
        coEvery { getAppMaintenanceConfigUseCase() } returns AppMaintenanceStatus.NonActionable.AllClear

        viewModel.navigationEventFlow.test {
            // Act
            viewModel.viewCreated(args)

            // Assert
            awaitItem().run {
                assertEquals(LaunchNavigationEvent.CorruptPayload, this)
            }
        }
    }

    @Test
    fun `GIVEN valid data, WHEN viewCreated is called, THEN RequestPin is called`() = runTest {
        // Arrange
        val args = LauncherState()

        every { authPrefs.walletGuid } returns WALLET_GUID
        every { authPrefs.pinId } returns PIN_ID
        every { encryptedPrefs.hasBackup() } returns false
        coEvery { getAppMaintenanceConfigUseCase() } returns AppMaintenanceStatus.NonActionable.AllClear

        viewModel.navigationEventFlow.test {
            // Act
            viewModel.viewCreated(args)

            // Assert
            awaitItem().run {
                assertEquals(LaunchNavigationEvent.RequestPin, this)
            }
        }
    }

    @Test
    fun `GIVEN pinId empty, WHEN viewCreated is called, THEN ReenterPassword is called`() = runTest {
        // Arrange
        val args = LauncherState()

        every { authPrefs.walletGuid } returns WALLET_GUID
        every { authPrefs.pinId } returns ""
        every { encryptedPrefs.hasBackup() } returns false
        coEvery { getAppMaintenanceConfigUseCase() } returns AppMaintenanceStatus.NonActionable.AllClear

        viewModel.navigationEventFlow.test {
            // Act
            viewModel.viewCreated(args)

            // Assert
            awaitItem().run {
                assertEquals(LaunchNavigationEvent.ReenterPassword, this)
            }
        }
    }

    @Test
    fun `GIVEN pinId and walletGuid empty, WHEN viewCreated is called, THEN NoGuid is called`() = runTest {
        // Arrange
        val args = LauncherState()

        every { authPrefs.walletGuid } returns ""
        every { authPrefs.pinId } returns ""
        every { encryptedPrefs.hasBackup() } returns false
        coEvery { getAppMaintenanceConfigUseCase() } returns AppMaintenanceStatus.NonActionable.AllClear

        viewModel.navigationEventFlow.test {
            // Act
            viewModel.viewCreated(args)

            // Assert
            awaitItem().run {
                assertEquals(LaunchNavigationEvent.NoGuid, this)
            }
        }
    }

    @Test
    fun `GIVEN walletGuid empty, hasBackup, WHEN viewCreated is called, THEN RequestPin is called`() = runTest {
        // Arrange
        val args = LauncherState()

        every { authPrefs.walletGuid } returns ""
        every { authPrefs.pinId } returns ""
        every { encryptedPrefs.hasBackup() } returns true
        coEvery { getAppMaintenanceConfigUseCase() } returns AppMaintenanceStatus.NonActionable.AllClear

        viewModel.navigationEventFlow.test {
            // Act
            viewModel.viewCreated(args)

            // Assert
            awaitItem().run {
                assertEquals(LaunchNavigationEvent.RequestPin, this)
            }
        }
    }

    @Test
    fun clearCredentialsAndRestart() {
        // Act
        viewModel.onIntent(LauncherIntent.ClearCredentialsAndRestart)

        // Assert
        verify { appUtil.clearCredentialsAndRestart() }
    }

    companion object {
        private const val WALLET_GUID = "d5f7c5db-072c-4178-b563-393259ec173a"
        private const val INVALID_WALLET_GUID = "0000-0000-0000-0000-00231231223400"
        private const val PIN_ID = "1234"
    }
}
