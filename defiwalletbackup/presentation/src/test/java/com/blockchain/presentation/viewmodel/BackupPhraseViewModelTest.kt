package com.blockchain.presentation.viewmodel

import app.cash.turbine.test
import com.blockchain.defiwalletbackup.domain.service.BackupPhraseService
import com.blockchain.outcome.Outcome
import com.blockchain.presentation.BackUpStatus
import com.blockchain.presentation.BackupPhraseArgs
import com.blockchain.presentation.BackupPhraseIntent
import com.blockchain.presentation.CopyState
import com.blockchain.presentation.UserMnemonicVerificationStatus
import com.blockchain.presentation.navigation.BackupPhraseNavigationEvent
import com.blockchain.testutils.CoroutineTestRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupPhraseViewModelTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    var coroutineTestRule = CoroutineTestRule()

    private val backupPhraseService = mockk<BackupPhraseService>()

    private lateinit var viewModel: BackupPhraseViewModel

    private val args = BackupPhraseArgs(secondPassword = "secondPassword")
    private val mnemonic = listOf("A", "B")

    @Before
    fun setUp() {
        viewModel = BackupPhraseViewModel(
            backupPhraseService = backupPhraseService
        )

        every { backupPhraseService.isBackedUp() } returns true
        every { backupPhraseService.getMnemonic(any()) } returns Outcome.Success(mnemonic)
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
    fun `GIVEN mnemonic unavailable, WHEN loadData is called, THEN getMnemonic should be called and state should be updated`() =
        runTest {
            every { backupPhraseService.getMnemonic(any()) } returns Outcome.Success(mnemonic)

            viewModel.viewState.test {
                viewModel.onIntent(BackupPhraseIntent.LoadData)

                verify(exactly = 1) { backupPhraseService.getMnemonic(any()) }
                val state = expectMostRecentItem()

                // todo
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
    fun `WHEN MnemonicCopied is called, THEN state should be Idle`() =
        runTest {
            viewModel.viewState.test {
                viewModel.onIntent(BackupPhraseIntent.ResetCopy)

                val state = expectMostRecentItem()

                assertEquals(CopyState.Idle, state.copyState)
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
            viewModel.viewState.test {
                viewModel.onIntent(BackupPhraseIntent.LoadData)

                viewModel.onIntent(BackupPhraseIntent.VerifyPhrase(userMnemonic = mnemonic))

                val state = expectMostRecentItem()

                assertEquals(UserMnemonicVerificationStatus.VERIFIED, state.mnemonicVerificationStatus)

                // todo
            }
        }
}
