package piuk.blockchain.android.ui.start

import com.blockchain.core.auth.AuthDataManager
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.logging.RemoteLogger
import com.blockchain.preferences.AuthPrefs
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.payload.data.Wallet
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import piuk.blockchain.android.testutils.RxTest
import piuk.blockchain.android.util.AppUtil

class PasswordRequiredPresenterTest : RxTest() {

    private lateinit var subject: PasswordRequiredPresenter
    private val view: PasswordRequiredView = mock()
    private val appUtil: AppUtil = mock()
    private val prefs: AuthPrefs = mock()
    private val authDataManager: AuthDataManager = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val wallet: Wallet = mock()
    private val remoteLogger: RemoteLogger = mock()

    @Before
    fun setUp() {
        whenever(prefs.walletGuid).thenReturn(GUID)

        whenever(payloadDataManager.wallet).thenReturn(wallet)
        whenever(payloadDataManager.wallet!!.sharedKey).thenReturn("shared_key")
        whenever(payloadDataManager.wallet!!.guid).thenReturn(GUID)

        subject = PasswordRequiredPresenter(
            appUtil,
            prefs,
            authDataManager,
            payloadDataManager,
            remoteLogger
        )

        subject.attachView(view)
    }

    @Test
    fun onContinueClickedNoPassword() {
        // Arrange
        whenever(prefs.walletGuid).thenReturn("")

        // Act
        subject.onContinueClicked("")

        // Assert
        verify(view).showSnackbar(anyInt(), any())
    }

    @Test
    fun onForgetWalletClickedShowWarningAndDismiss() {
        // Arrange

        // Act
        subject.onForgetWalletClicked()

        // Assert
        verify(view).showForgetWalletWarning()
        verifyNoMoreInteractions(view)
    }

    companion object {
        private const val GUID = "1234567890"
    }
}
