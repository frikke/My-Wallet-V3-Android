package com.blockchain.presentation.backup

import app.cash.turbine.test
import com.blockchain.analytics.Analytics
import com.blockchain.defiwalletbackup.domain.service.BackupPhraseService
import com.blockchain.outcome.Outcome
import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.presentation.backup.navigation.BackupPhraseNavigationEvent
import com.blockchain.presentation.backup.viewmodel.BackupPhraseViewModel
import com.blockchain.testutils.CoroutineTestRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
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
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.EncryptedPrefs

@OptIn(ExperimentalCoroutinesApi::class)
class BackupPhraseViewModelTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    var coroutineTestRule = CoroutineTestRule()

    private val backupPhraseService = mockk<BackupPhraseService>()
    private val settingsDataManager = mockk<SettingsDataManager>()
    private val backupPrefs = mockk<EncryptedPrefs>()
    private val walletStatusPrefs = mockk<WalletStatusPrefs>()
    private val authPrefs = mockk<AuthPrefs>()
    private val analytics = mockk<Analytics>()

    private lateinit var viewModel: BackupPhraseViewModel

    private val args = BackupPhraseArgs(secondPassword = "secondPassword", allowSkipBackup = true)
    private val mnemonic = listOf("A", "B")

    private val walletGuid = "walletGuid"
    private val sharedKey = "sharedKey"

    @Before
    fun setUp() {
        viewModel = BackupPhraseViewModel(
            backupPhraseService = backupPhraseService,
            settingsDataManager = settingsDataManager,
            backupPrefs = backupPrefs,
            walletStatusPrefs = walletStatusPrefs,
            authPrefs = authPrefs,
            analytics = analytics
        )

        every { backupPhraseService.isBackedUp() } returns true
        every { backupPhraseService.getMnemonic(any()) } returns Outcome.Success(mnemonic)

        coEvery { settingsDataManager.triggerEmailAlert(any(), any()) } just Runs
        coEvery { analytics.logEvent(any()) } just Runs

        every { walletStatusPrefs.isWalletBackUpSkipped = any() } just Runs

        every { authPrefs.walletGuid } returns walletGuid
        every { authPrefs.sharedKey } returns sharedKey
    }

    @Test
    fun `WHEN TriggerEmailAlert is called, THEN triggerEmailAlert should be called`() {
        viewModel.onIntent(BackupPhraseIntent.TriggerEmailAlert)

        coVerify(exactly = 1) { settingsDataManager.triggerEmailAlert(guid = walletGuid, sharedKey = sharedKey) }
    }

    @Test
    fun `WHEN viewCreated is called, THEN getMnemonic should be called with second password`() =
        runTest {
            viewModel.viewCreated(args)

            verify(exactly = 1) { backupPhraseService.getMnemonic(args.secondPassword) }
        }

    @Test
    fun `GIVEN phrase backed up, WHEN loadData is called, THEN isBackedUp should be called and state should be updated`() =
        runTest {
            every { backupPhraseService.isBackedUp() } returns true

            viewModel.viewState.test {
                viewModel.onIntent(BackupPhraseIntent.LoadData)

                verify(exactly = 1) { backupPhraseService.isBackedUp() }
                val state = expectMostRecentItem()
                assertEquals(BackUpStatus.BACKED_UP, state.backUpStatus)
            }
        }

    @Test
    fun `GIVEN phrase not backed up, WHEN loadData is called, THEN isBackedUp should be called and state should be updated`() =
        runTest {
            every { backupPhraseService.isBackedUp() } returns false

            viewModel.viewState.test {
                viewModel.onIntent(BackupPhraseIntent.LoadData)

                verify(exactly = 1) { backupPhraseService.isBackedUp() }
                val state = expectMostRecentItem()
                assertEquals(BackUpStatus.NO_BACKUP, state.backUpStatus)
            }
        }

    @Test
    fun `GIVEN mnemonic available, WHEN loadData is called, THEN getMnemonic should be called and state should be updated`() =
        runTest {
            every { backupPhraseService.getMnemonic(any()) } returns Outcome.Success(mnemonic)

            viewModel.viewState.test {
                viewModel.onIntent(BackupPhraseIntent.LoadData)

                verify(exactly = 1) { backupPhraseService.getMnemonic(any()) }
                val state = expectMostRecentItem()
                assertEquals(mnemonic, state.mnemonic)
            }
        }

    @Test
    fun `WHEN GoToSkipBackup is called, THEN SkipBackup should be called`() =
        runTest {
            viewModel.navigationEventFlow.test {
                viewModel.onIntent(BackupPhraseIntent.GoToSkipBackup)

                val navigation = expectMostRecentItem()

                assertEquals(BackupPhraseNavigationEvent.SkipBackup, navigation)
            }
        }

    @Test
    fun `WHEN SkipBackup is called, THEN isWalletBackUpSkipped should be true, EndFlow with isSuccessful called`() =
        runTest {
            viewModel.viewState.test {
                viewModel.onIntent(BackupPhraseIntent.SkipBackup)

                verify { walletStatusPrefs.isWalletBackUpSkipped = true }

                val state = expectMostRecentItem()
                assertEquals(FlowState.Ended(true), state.flowState)
            }
        }

    @Test
    fun `WHEN StartBackupProcess is called, THEN RecoveryPhrase should be called`() =
        runTest {
            viewModel.navigationEventFlow.test {
                viewModel.onIntent(BackupPhraseIntent.StartBackupProcess)

                val navigation = expectMostRecentItem()

                assertEquals(BackupPhraseNavigationEvent.RecoveryPhrase, navigation)
            }
        }

    @Test
    fun `WHEN StartManualBackup is called, THEN ManualBackup should be called`() =
        runTest {
            viewModel.navigationEventFlow.test {
                viewModel.onIntent(BackupPhraseIntent.StartManualBackup)

                val navigation = expectMostRecentItem()

                assertEquals(BackupPhraseNavigationEvent.ManualBackup, navigation)
            }
        }

    @Test
    fun `WHEN MnemonicCopied is called, THEN state should be Copied`() =
        runTest {
            viewModel.viewState.test {
                viewModel.onIntent(BackupPhraseIntent.MnemonicCopied)

                val state = expectMostRecentItem()

                assertEquals(CopyState.Copied, state.copyState)
            }
        }

    @Test
    fun `WHEN ResetCopy is called, THEN state should be Idle with resetClipboard true`() =
        runTest {
            viewModel.viewState.test {
                viewModel.onIntent(BackupPhraseIntent.ResetCopy)

                val state = expectMostRecentItem()

                assertEquals(CopyState.Idle(resetClipboard = true), state.copyState)
            }
        }

    @Test
    fun `WHEN ClipboardReset is called, THEN state should be Idle with resetClipboard false`() =
        runTest {
            viewModel.viewState.test {
                viewModel.onIntent(BackupPhraseIntent.ClipboardReset)

                val state = expectMostRecentItem()

                assertEquals(CopyState.Idle(resetClipboard = false), state.copyState)
            }
        }

    @Test
    fun `WHEN StartUserPhraseVerification is called, THEN VerifyPhrase should be called`() =
        runTest {
            viewModel.navigationEventFlow.test {
                viewModel.onIntent(BackupPhraseIntent.StartUserPhraseVerification)

                val navigation = expectMostRecentItem()

                assertEquals(BackupPhraseNavigationEvent.VerifyPhrase, navigation)
            }
        }

    @Test
    fun `GIVEN incorrect mnemonic, WHEN VerifyPhrase is called, THEN status should be INCORRECT`() =
        runTest {
            viewModel.viewState.test {
                viewModel.onIntent(BackupPhraseIntent.LoadData)

                viewModel.onIntent(BackupPhraseIntent.VerifyPhrase(userMnemonic = mnemonic + "C"))

                val state = expectMostRecentItem()

                assertEquals(UserMnemonicVerificationStatus.INCORRECT, state.mnemonicVerificationStatus)
            }
        }

    @Test
    fun `GIVEN correct mnemonic, WHEN VerifyPhrase is called, THEN status should be VERIFIED`() =
        runTest {
            coEvery { backupPhraseService.confirmRecoveryPhraseBackedUp() } returns Outcome.Success(Unit)

            viewModel.navigationEventFlow.test {
                viewModel.onIntent(BackupPhraseIntent.LoadData)

                viewModel.onIntent(BackupPhraseIntent.VerifyPhrase(userMnemonic = mnemonic))

                val navigation = expectMostRecentItem()

                coVerify(exactly = 1) { backupPhraseService.confirmRecoveryPhraseBackedUp() }
                assertEquals(BackupPhraseNavigationEvent.BackupConfirmation, navigation)
            }
        }

    @Test
    fun `GIVEN cloud backup, WHEN EnableCloudBackup is called, THEN status should be VERIFIED`() =
        runTest {
            coEvery { backupPhraseService.confirmRecoveryPhraseBackedUp() } returns Outcome.Success(Unit)
            every { backupPrefs.backupEnabled = any() } just Runs

            viewModel.navigationEventFlow.test {
                viewModel.onIntent(BackupPhraseIntent.LoadData)

                viewModel.onIntent(BackupPhraseIntent.EnableCloudBackup)

                val navigation = expectMostRecentItem()

                coVerify(exactly = 1) { backupPhraseService.confirmRecoveryPhraseBackedUp() }
                coVerify(exactly = 1) { backupPrefs.backupEnabled = true }
                assertEquals(BackupPhraseNavigationEvent.CloudBackupConfirmation, navigation)
            }
        }
}
