package piuk.blockchain.android.ui.backup.completed

import com.blockchain.preferences.WalletStatusPrefs
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import piuk.blockchain.androidcore.data.auth.AuthDataManager

class BackupWalletCompletedPresenterTest {

    private lateinit var subject: BackupWalletCompletedPresenter
    private val view: BackupWalletCompletedView = mock()
    private val walletStatusPrefs: WalletStatusPrefs = mock()
    private val authDataManager: AuthDataManager = mock()

    @Before
    fun setUp() {
        subject = BackupWalletCompletedPresenter(walletStatusPrefs, authDataManager)
        subject.initView(view)
    }

    @Test
    fun `onViewReady set backup date`() {
        // Arrange
        val date = 1499181978000L
        whenever(walletStatusPrefs.lastBackupTime).thenReturn(date)
        // Act
        subject.onViewReady()
        // Assert
        verify(walletStatusPrefs).lastBackupTime
        verifyNoMoreInteractions(walletStatusPrefs)
        verify(view).showLastBackupDate(date)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady hide backup date`() {
        // Arrange
        whenever(walletStatusPrefs.lastBackupTime).thenReturn(0L)
        // Act
        subject.onViewReady()
        // Assert
        verify(walletStatusPrefs).lastBackupTime
        verifyNoMoreInteractions(walletStatusPrefs)
        verify(view).hideLastBackupDate()
        verifyNoMoreInteractions(view)
    }
}
