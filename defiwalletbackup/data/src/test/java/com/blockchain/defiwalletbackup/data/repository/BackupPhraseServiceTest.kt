package com.blockchain.defiwalletbackup.data.repository

import com.blockchain.defiwalletbackup.domain.errors.BackupPhraseError
import com.blockchain.defiwalletbackup.domain.service.BackupPhraseService
import com.blockchain.outcome.Outcome
import com.blockchain.preferences.WalletStatus
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.reactivex.rxjava3.core.Completable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import piuk.blockchain.androidcore.data.payload.BackupWalletUtil
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class BackupPhraseServiceTest {
    private val payloadManager = mockk<PayloadDataManager>()
    private val backupWalletUtil = mockk<BackupWalletUtil>()
    private val walletStatus = mockk<WalletStatus>()

    private val backupPhraseService: BackupPhraseService = BackupPhraseRepository(
        payloadManager = payloadManager,
        backupWalletUtil = backupWalletUtil,
        walletStatus = walletStatus
    )

    private val mnemonic = listOf("A", "B")

    @Before
    fun setUp() {
        every { payloadManager.wallet!!.walletBody?.mnemonicVerified = any() } just Runs
        every { walletStatus.lastBackupTime = any() } just Runs
    }

    @Test
    fun `GIVEN phrase backed up, WHEN isBackedUp is called, THEN true should be returned`() {
        every { payloadManager.isBackedUp } returns true

        val result = backupPhraseService.isBackedUp()

        assertEquals(true, result)
    }

    @Test
    fun `GIVEN phrase not backed up, WHEN isBackedUp is called, THEN false should be returned`() {
        every { payloadManager.isBackedUp } returns false

        val result = backupPhraseService.isBackedUp()

        assertEquals(false, result)
    }

    @Test
    fun `GIVEN mnemonic exists, WHEN getMnemonic is called, THEN mnemonic should be returned`() {
        every { backupWalletUtil.getMnemonic(any()) } returns mnemonic

        val result = backupPhraseService.getMnemonic(null)

        assertEquals(Outcome.Success(mnemonic), result)
    }

    @Test
    fun `GIVEN mnemonic null, WHEN getMnemonic is called, THEN Failure NoMnemonicFound should be returned`() {
        every { backupWalletUtil.getMnemonic(any()) } returns null

        val result = backupPhraseService.getMnemonic(null)

        assertEquals(Outcome.Failure(BackupPhraseError.NoMnemonicFound), result)
    }

    @Test
    fun `GIVEN sync successful, WHEN confirmRecoveryPhraseBackedUp is called, THEN Success should be returned`() =
        runTest {
            every { payloadManager.syncPayloadWithServer() } returns Completable.complete()

            val result = backupPhraseService.confirmRecoveryPhraseBackedUp()

            assertEquals(Outcome.Success(Unit), result)
        }

    @Test
    fun `GIVEN sync unsuccessful, WHEN confirmRecoveryPhraseBackedUp is called, THEN Failure BackupConfirmationError should be returned`() =
        runTest {
            every { payloadManager.syncPayloadWithServer() } returns Completable.error(Exception())

            val result = backupPhraseService.confirmRecoveryPhraseBackedUp()

            assertEquals(Outcome.Failure(BackupPhraseError.BackupConfirmationError), result)
        }
}