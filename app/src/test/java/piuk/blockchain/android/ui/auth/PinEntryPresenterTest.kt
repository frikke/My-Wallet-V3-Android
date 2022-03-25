package piuk.blockchain.android.ui.auth

import android.view.View
import com.blockchain.android.testutils.rxInit
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.datamanagers.ApiStatus
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.wallet.DefaultLabels
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.api.data.UpdateType
import info.blockchain.wallet.exceptions.AccountLockedException
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import info.blockchain.wallet.exceptions.ServerConnectionException
import info.blockchain.wallet.exceptions.UnsupportedVersionException
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.Wallet
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.net.SocketTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Answers
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.spongycastle.crypto.InvalidCipherTextException
import piuk.blockchain.android.data.biometrics.BiometricsController
import piuk.blockchain.android.ui.home.CredentialsWiper
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.data.access.PinRepository
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs

class PinEntryPresenterTest {

    private val view: PinEntryView = mock()

    private val authDataManager: AuthDataManager = mock {
        on { verifyCloudBackup() }.thenReturn(Completable.complete())
    }

    private val appUtil: AppUtil = mock()
    private val prefsUtil: PersistentPrefs = mock {
        on { walletGuid }.thenReturn(WALLET_GUID)
        on { sharedKey }.thenReturn(SHARED_KEY)
    }

    private val mockWallet: Wallet = mock {
        on { sharedKey }.thenReturn(SHARED_KEY)
    }

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private val payloadManager: PayloadDataManager = mock {
        on { wallet }.thenReturn(mockWallet)
    }
    private val defaultLabels: DefaultLabels = mock {
        on { getDefaultNonCustodialWalletLabel() }.thenReturn("string resource")
    }

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
    }

    private val biometricsController: BiometricsController = mock()
    private val pinRepository: PinRepository = mock()
    private val walletOptionsDataManager: WalletOptionsDataManager = mock()
    private val mobileNoticeRemoteConfig: MobileNoticeRemoteConfig = mock()
    private val crashLogger: CrashLogger = mock()
    private val analytics: Analytics = mock()
    private val credentialsWiper: CredentialsWiper = mock()

    private val apiStatus: ApiStatus = mock {
        on { isHealthy() }.thenReturn(Single.just(true))
    }

    private lateinit var subject: PinEntryPresenter

    @Before
    fun init() {
        subject = PinEntryPresenter(
            analytics = analytics,
            authDataManager = authDataManager,
            appUtil = appUtil,
            prefs = prefsUtil,
            payloadDataManager = payloadManager,
            defaultLabels = defaultLabels,
            pinRepository = pinRepository,
            walletOptionsDataManager = walletOptionsDataManager,
            mobileNoticeRemoteConfig = mobileNoticeRemoteConfig,
            crashLogger = crashLogger,
            apiStatus = apiStatus,
            credentialsWiper = credentialsWiper,
            specificAnalytics = mock(),
            biometricsController = biometricsController
        )
        subject.initView(view)
    }

    @Test
    fun onViewReadyValidatingPinForResult() {
        // Arrange
        whenever(view.isForValidatingPinForResult).thenReturn(true)
        // Act
        subject.onViewReady()

        // Assert
        assertTrue(subject.isForValidatingPinForResult)
    }

    @Test
    fun onViewReadyMaxAttemptsExceeded() {
        // Arrange
        whenever(prefsUtil.pinFails).thenReturn(4)
        whenever(payloadManager.wallet).thenReturn(mock())
        whenever(prefsUtil.pinId).thenReturn("")

        // Act
        subject.onViewReady()

        // Assert
        assertTrue(subject.allowExit())
        verify(view).showParameteredSnackbar(anyInt(), any(), anyInt(), any())
        verify(view).showMaxAttemptsDialog()
    }

    @Test
    fun checkFingerprintStatusShouldShowDialog() {
        // Arrange
        subject.isForValidatingPinForResult = false
        whenever(prefsUtil.pinId).thenReturn("1234")
        whenever(biometricsController.isBiometricUnlockEnabled).thenReturn(true)

        // Act
        subject.checkFingerprintStatus()

        // Assert
        verify(view).showFingerprintDialog()
    }

    @Test
    fun checkFingerprintStatusDontShow() {
        // Arrange
        subject.isForValidatingPinForResult = true
        // Act
        subject.checkFingerprintStatus()
        // Assert
        verify(view).showKeyboard()
    }

    @Test
    fun canShowFingerprintDialog() {
        // Arrange
        subject.canShowFingerprintDialog = true
        // Act
        val value = subject.canShowFingerprintDialog()
        // Assert
        assertTrue(value)
    }

    @Test
    fun loginWithDecryptedPin() {
        // Arrange
        val pincode = "1234"
        whenever(authDataManager.validatePin(pincode)).thenReturn(Observable.just("password"))
        whenever(payloadManager.initializeAndDecrypt(anyString(), anyString(), anyString())).thenReturn(
            Completable.error(Exception())
        )
        // Act
        subject.loginWithDecryptedPin(pincode)
        // Assert
        assertFalse(subject.canShowFingerprintDialog())

        verify(authDataManager).validatePin(pincode)
        verify(view).fillPinBoxes()
    }

    @Test
    fun onDeleteClicked() {
        // Arrange
        subject.userEnteredPin = "1234"
        // Act
        subject.onDeleteClicked()
        // Assert
        assertEquals("123", subject.userEnteredPin)
        verify(view).clearPinBoxAtIndex(3)
    }

    @Test
    fun padClickedPinAlreadyFourDigits() {
        // Arrange
        subject.userEnteredPin = "0000"
        // Act
        subject.onPadClicked("0")
        // Assert
        verifyZeroInteractions(view)
    }

    @Test
    fun padClickedAllZeros() {
        // Arrange
        subject.userEnteredPin = "000"

        whenever(prefsUtil.pinId).thenReturn("")

        // Act
        subject.onPadClicked("0")

        // Assert
        assertEquals("", subject.userEnteredPin)
        assertNull(subject.userEnteredConfirmationPin)

        verify(view).clearPinBoxes()
        verify(view).showSnackbar(anyInt(), any(), any())
        verify(view).dismissProgressDialog()
        verify(view).fillPinBoxAtIndex(0)
        verify(view).fillPinBoxAtIndex(1)
        verify(view).fillPinBoxAtIndex(2)
        verify(view).fillPinBoxAtIndex(3)
        verify(view).showKeyboard()
        verify(view).setTitleString(anyInt())

        verifyNoMoreInteractions(view)
    }

    @Test
    fun padClickedShowCommonPinWarning() {
        // Arrange
        subject.userEnteredPin = "123"
        whenever(prefsUtil.pinId).thenReturn("")

        // Act
        subject.onPadClicked("4")

        // Assert
        verify(view).showCommonPinWarning(any())
    }

    @Test fun padClickedShowCommonPinWarningAndClickRetry() {
        // Arrange
        subject.userEnteredPin = "123"
        whenever(prefsUtil.pinId).thenReturn("")
        doAnswer { invocation: InvocationOnMock ->
            (invocation.arguments[0] as DialogButtonCallback).onPositiveClicked()
            null
        }.whenever(view).showCommonPinWarning(any())

        // Act
        subject.onPadClicked("4")

        // Assert
        assertEquals("", subject.userEnteredPin)
        assertNull(subject.userEnteredConfirmationPin)

        verify(view).showCommonPinWarning(any())
        verify(view).clearPinBoxes()
    }

    @Test fun padClickedShowCommonPinWarningAndClickContinue() {
        // Arrange
        subject.userEnteredPin = "123"
        whenever(prefsUtil.pinId).thenReturn("")
        doAnswer { invocation: InvocationOnMock ->
            (invocation.arguments[0] as DialogButtonCallback).onNegativeClicked()
            null
        }.whenever(view).showCommonPinWarning(any())

        // Act
        subject.onPadClicked("4")

        // Assert
        assertEquals("", subject.userEnteredPin)
        assertEquals("1234", subject.userEnteredConfirmationPin)

        verify(view).showCommonPinWarning(any())
    }

    @Test
    fun padClickedShowPinReuseWarning() {
        // Arrange
        subject.userEnteredPin = "258"
        whenever(prefsUtil.pinId).thenReturn("")
        whenever(pinRepository.pin).thenReturn("2580")

        // Act
        subject.onPadClicked("0")

        // Assert
        verify(view).dismissProgressDialog()
        verify(view).showSnackbar(anyInt(), eq(SnackbarType.Error), any())
        verify(view).clearPinBoxes()
    }

    @Test
    fun padClickedVerifyPinValidateCalled() {
        // Arrange
        subject.userEnteredPin = "133"
        whenever(prefsUtil.pinId).thenReturn("1234567890")
        whenever(authDataManager.validatePin(anyString())).thenReturn(Observable.just(""))
        whenever(payloadManager.initializeAndDecrypt(anyString(), anyString(), anyString())).thenReturn(
            Completable.complete()
        )

        // Act
        subject.onPadClicked("7")

        // Assert
        verify(view).setTitleVisibility(View.INVISIBLE)
        verify(view, times(2)).showProgressDialog(anyInt())
        verify(view, times(2)).dismissProgressDialog()
        verify(authDataManager).validatePin(anyString())
    }

    @Test
    fun padClickedVerifyPinForResultReturnsValidPassword() {
        // Arrange
        subject.userEnteredPin = "133"
        subject.isForValidatingPinForResult = true
        whenever(prefsUtil.pinId).thenReturn("1234567890")
        whenever(authDataManager.validatePin(anyString())).thenReturn(Observable.just(""))

        // Act
        subject.onPadClicked("7")

        // Assert
        verify(view).setTitleVisibility(View.INVISIBLE)
        verify(view).showProgressDialog(anyInt())
        verify(view).dismissProgressDialog()
        verify(authDataManager).validatePin(anyString())
        verify(view).finishWithResultOk("1337")
    }

    @Test
    fun padClickedVerifyPinValidateCalledReturnsErrorIncrementsFailureCount() {
        // Arrange
        subject.userEnteredPin = "133"
        whenever(prefsUtil.pinId).thenReturn("1234567890")
        whenever(authDataManager.validatePin(anyString()))
            .thenReturn(
                Observable.error(
                    InvalidCredentialsException()
                )
            )

        // Act
        subject.onPadClicked("7")

        // Assert
        verify(view).setTitleVisibility(View.INVISIBLE)
        verify(view).showProgressDialog(anyInt())
        verify(authDataManager).validatePin(anyString())
        verify(prefsUtil).pinFails = anyInt()
        verify(prefsUtil).pinFails
        verify(view).showSnackbar(anyInt(), any(), any())
    }

    @Test
    fun padClickedVerifyPinValidateCalledReturnsServerError() {
        // Arrange
        subject.userEnteredPin = "133"
        whenever(prefsUtil.pinId).thenReturn("1234567890")
        whenever(authDataManager.validatePin(anyString()))
            .thenReturn(
                Observable.error(
                    ServerConnectionException()
                )
            )

        // Act
        subject.onPadClicked("7")

        // Assert
        verify(view).setTitleVisibility(View.INVISIBLE)
        verify(view).showProgressDialog(anyInt())
        verify(authDataManager).validatePin(anyString())
        verify(view).showSnackbar(anyInt(), any(), any())
    }

    @Test
    fun padClickedVerifyPinValidateCalledReturnsTimeout() {
        // Arrange
        subject.userEnteredPin = "133"
        whenever(prefsUtil.pinId).thenReturn("1234567890")
        whenever(authDataManager.validatePin(anyString()))
            .thenReturn(Observable.error(SocketTimeoutException()))

        // Act
        subject.onPadClicked("7")

        // Assert
        verify(view).setTitleVisibility(View.INVISIBLE)
        verify(view).showProgressDialog(anyInt())
        verify(authDataManager).validatePin(anyString())
        verify(view).showSnackbar(anyInt(), any(), any())
    }

    @Test
    fun padClickedVerifyPinValidateCalledReturnsInvalidCipherText() {
        // Arrange
        val password = "password"
        subject.userEnteredPin = "133"
        whenever(prefsUtil.pinId).thenReturn("1234567890")
        whenever(authDataManager.validatePin(anyString())).thenReturn(Observable.just(password))
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(Completable.error(InvalidCipherTextException()))

        // Act
        subject.onPadClicked("7")

        // Assert
        verify(view).setTitleVisibility(View.INVISIBLE)
        verify(view, times(2)).showProgressDialog(anyInt())
        verify(view, atLeastOnce()).dismissProgressDialog()
        verify(authDataManager).validatePin(anyString())
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(view).showSnackbar(anyInt(), any(), any())
        verify(pinRepository).clearPin()
        verify(appUtil).clearCredentials()
        verify(prefsUtil).sharedKey
        verify(prefsUtil).walletGuid
        verify(prefsUtil, atLeastOnce()).pinId
        verify(prefsUtil).pinFails = anyInt()

        verifyNoMoreInteractions(prefsUtil)
    }

    @Test
    fun padClickedVerifyPinValidateCalledReturnsGenericException() {
        // Arrange
        val password = "password"
        subject.userEnteredPin = "133"
        whenever(prefsUtil.pinId).thenReturn("1234567890")
        whenever(authDataManager.validatePin(anyString())).thenReturn(Observable.just(password))
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(Completable.error(Exception()))

        // Act
        subject.onPadClicked("7")

        // Assert
        verify(view).setTitleVisibility(View.INVISIBLE)
        verify(view).showSnackbar(anyInt(), any(), any())

        verify(view).fillPinBoxAtIndex(0)
        verify(view).fillPinBoxAtIndex(1)
        verify(view).fillPinBoxAtIndex(2)
        verify(view).fillPinBoxAtIndex(3)

        verify(view, times(2)).showProgressDialog(anyInt())
        verify(view, times(2)).dismissProgressDialog()
        verify(authDataManager).validatePin(anyString())
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(prefsUtil).pinFails = anyInt()
        verify(prefsUtil, atLeastOnce()).pinId
        verify(prefsUtil).walletGuid
        verify(prefsUtil).sharedKey

        verifyNoMoreInteractions(prefsUtil)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun padClickedCreatePinCreateSuccessful() {
        // Arrange
        val confirmedPin = "1337"
        subject.userEnteredPin = "133"
        subject.userEnteredConfirmationPin = confirmedPin
        whenever(payloadManager.tempPassword).thenReturn("temp password")
        whenever(payloadManager.initializeAndDecrypt(anyString(), anyString(), anyString())).thenReturn(
            Completable.complete()
        )
        whenever(prefsUtil.pinId).thenReturn("")
        whenever(authDataManager.createPin(anyString(), anyString())).thenReturn(Completable.complete())
        whenever(authDataManager.validatePin(anyString())).thenReturn(Observable.just("password"))
        whenever(pinRepository.pin).thenReturn("1337")

        // Act
        subject.onPadClicked("7")

        // Assert
        verify(view, times(2)).showProgressDialog(anyInt())
        verify(view, times(2)).dismissProgressDialog()
        verify(authDataManager).createPin(anyString(), anyString())
        verify(biometricsController).setBiometricUnlockDisabled()
    }

    @Test
    fun padClickedCreatePinCreateFailed() {
        // Arrange
        subject.userEnteredPin = "133"
        subject.userEnteredConfirmationPin = "1337"
        whenever(payloadManager.tempPassword).thenReturn("temp password")
        whenever(prefsUtil.pinId).thenReturn("")
        whenever(authDataManager.createPin(anyString(), anyString())).thenReturn(Completable.error(Throwable()))

        whenever(pinRepository.pin).thenReturn("")

        // Act
        subject.onPadClicked("7")

        // Assert
        verify(view).showProgressDialog(anyInt())
        verify(view, atLeastOnce()).dismissProgressDialog()
        verify(view).showSnackbar(anyInt(), any(), any())
        verify(authDataManager).createPin(anyString(), anyString())
        verify(prefsUtil).clear()
    }

    @Test
    fun padClickedCreatePinWritesNewConfirmationValue() {
        // Arrange
        subject.userEnteredPin = "133"
        whenever(prefsUtil.pinId).thenReturn("")
        whenever(authDataManager.createPin(anyString(), anyString())).thenReturn(Completable.complete())
        whenever(pinRepository.pin).thenReturn("")

        // Act
        subject.onPadClicked("7")

        // Assert
        assertEquals("1337", subject.userEnteredConfirmationPin)
        assertEquals("", subject.userEnteredPin)
    }

    @Test
    fun padClickedCreatePinMismatched() {
        // Arrange
        subject.userEnteredPin = "133"
        subject.userEnteredConfirmationPin = "1234"
        whenever(prefsUtil.pinId).thenReturn("")
        whenever(authDataManager.createPin(anyString(), anyString())).thenReturn(Completable.complete())
        whenever(pinRepository.pin).thenReturn("")

        // Act
        subject.onPadClicked("7")

        // Assert
        verify(view).showSnackbar(anyInt(), any(), any())
        verify(view).dismissProgressDialog()
    }

    @Test
    fun clearPinBoxes() {
        subject.clearPinBoxes()

        verify(view).clearPinBoxes()
        assertEquals("", subject.userEnteredPin)
    }

    @Test
    fun validatePasswordSuccessful() {
        // Arrange
        val password = "1234567890"
        whenever(payloadManager.initializeAndDecrypt(anyString(), anyString(), eq(password)))
            .thenReturn(Completable.complete())

        // Act
        subject.validatePassword(password)

        // Assert
        verify(payloadManager).initializeAndDecrypt(anyString(), anyString(), eq(password))
        verify(view).showProgressDialog(anyInt())
        verify(view).dismissProgressDialog()
        verify(view).showSnackbar(anyInt(), any(), any())
        verify(prefsUtil).removeValue(PersistentPrefs.KEY_PIN_FAILS)
        verify(prefsUtil).pinId = anyString()
        verify(pinRepository).clearPin()
    }

    @Test fun validatePasswordThrowsGenericException() {
        // Arrange
        val password = "1234567890"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(Completable.error(Throwable()))

        // Act
        subject.validatePassword(password)

        // Assert
        verify(view).showProgressDialog(anyInt())
        verify(view, atLeastOnce()).dismissProgressDialog()
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(view).showSnackbar(anyInt(), any(), any())
        verify(view).showValidationDialog()
    }

    @Test
    fun validatePasswordThrowsServerConnectionException() {
        // Arrange
        val password = "1234567890"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(
                Completable.error(
                    ServerConnectionException()
                )
            )

        // Act
        subject.validatePassword(password)

        // Assert
        verify(view).showProgressDialog(anyInt())
        verify(view).dismissProgressDialog()
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(view).showSnackbar(anyInt(), any(), any())
    }

    @Test
    fun validatePasswordThrowsSocketTimeoutException() {
        // Arrange
        val password = "1234567890"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(Completable.error(SocketTimeoutException()))

        // Act
        subject.validatePassword(password)

        // Assert
        verify(view).showProgressDialog(anyInt())
        verify(view).dismissProgressDialog()
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(view).showSnackbar(anyInt(), any(), any())
    }

    @Test
    fun validatePasswordThrowsHDWalletExceptionException() {
        // Arrange
        val password = "1234567890"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(
                Completable.error(
                    HDWalletException()
                )
            )

        // Act
        subject.validatePassword(password)

        // Assert
        verify(view).showProgressDialog(anyInt())
        verify(view).dismissProgressDialog()
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(view).showSnackbar(anyInt(), any(), any())
    }

    @Test
    fun validatePasswordThrowsAccountLockedException() {
        // Arrange
        val password = "1234567890"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(
                Completable.error(
                    AccountLockedException()
                )
            )

        // Act
        subject.validatePassword(password)

        // Assert
        verify(view).showProgressDialog(anyInt())
        verify(view).dismissProgressDialog()
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(view).showAccountLockedDialog()
    }

    @Test
    fun updatePayloadInvalidCredentialsException() {
        // Arrange
        val password = "change_me"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(
                Completable.error(
                    InvalidCredentialsException()
                )
            )

        // Act
        subject.updatePayload(password, false)

        // Assert
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(view).showProgressDialog(anyInt())
        verify(view).dismissProgressDialog()
        verify(view).goToPasswordRequiredActivity()
        verify(prefsUtil).walletGuid
        verify(prefsUtil).sharedKey

        verifyNoMoreInteractions(prefsUtil)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun updatePayloadServerConnectionException() {
        // Arrange
        val password = "password"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(
                Completable.error(
                    ServerConnectionException()
                )
            )

        val mockPayload: Wallet = mock {
            on { sharedKey }.thenReturn(SHARED_KEY)
        }
        whenever(payloadManager.wallet).thenReturn(mockPayload)

        // Act
        subject.updatePayload(password, false)

        // Assert
        verify(view).showProgressDialog(anyInt())
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(view).showSnackbar(anyInt(), any(), any())
    }

    @Test
    fun updatePayloadDecryptionException() {
        // Arrange
        val password = "password"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(
                Completable.error(
                    DecryptionException()
                )
            )

        val mockPayload: Wallet = mock {
            on { sharedKey }.thenReturn(SHARED_KEY)
        }
        whenever(payloadManager.wallet).thenReturn(mockPayload)

        // Act
        subject.updatePayload(password, false)

        // Assert
        verify(view).showProgressDialog(anyInt())
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(view).goToPasswordRequiredActivity()
    }

    @Test
    fun updatePayloadHDWalletException() {
        // Arrange
        val password = "password"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(
                Completable.error(
                    HDWalletException()
                )
            )
        val mockPayload: Wallet = mock {
            on { sharedKey }.thenReturn(SHARED_KEY)
        }
        whenever(payloadManager.wallet).thenReturn(mockPayload)

        // Act
        subject.updatePayload(password, false)

        // Assert
        verify(view).showProgressDialog(anyInt())
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(view).showSnackbar(anyInt(), any(), any())
    }

    @Test
    fun updatePayloadVersionNotSupported() {
        // Arrange
        val password = "password"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(
                Completable.error(
                    UnsupportedVersionException()
                )
            )

        val mockPayload: Wallet = mock {
            on { sharedKey }.thenReturn(SHARED_KEY)
        }
        whenever(payloadManager.wallet).thenReturn(mockPayload)
        // Act
        subject.updatePayload(password, false)

        // Assert
        verify(view).showProgressDialog(anyInt())
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(view).showWalletVersionNotSupportedDialog(isNull())
    }

    @Test
    fun updatePayloadAccountLocked() {
        // Arrange
        val password = "Change_Me"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(
                Completable.error(
                    AccountLockedException()
                )
            )

        val mockPayload = mock<Wallet> {
            on { sharedKey }.thenReturn(SHARED_KEY)
        }
        whenever(payloadManager.wallet).thenReturn(mockPayload)
        // Act
        subject.updatePayload(password, false)

        // Assert
        verify(view).showProgressDialog(anyInt())
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(view).showAccountLockedDialog()
    }

    @Test
    fun updatePayloadSuccessfulSetLabels() {
        // Arrange
        val password = "Change_Me"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(Completable.complete())

        val mockAccount: Account = mock {
            on { label }.thenReturn("")
        }
        whenever(payloadManager.accounts).thenReturn(listOf(mockAccount))
        whenever(payloadManager.getAccount(0)).thenReturn(mockAccount)

        val mockWallet: Wallet = mock {
            on { sharedKey }.thenReturn(SHARED_KEY)
        }
        whenever(payloadManager.wallet).thenReturn(mockWallet)
        whenever(prefsUtil.isNewlyCreated).thenReturn(true)

        // Act
        subject.updatePayload(password, false)

        // Assert
        assertTrue(subject.canShowFingerprintDialog)

        verify(view).showProgressDialog(anyInt())
        verify(view).dismissProgressDialog()
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(prefsUtil).walletGuid
        verify(prefsUtil).sharedKey
        verify(prefsUtil).isNewlyCreated
        verify(prefsUtil).sharedKey = SHARED_KEY
        verify(payloadManager, Mockito.atLeastOnce()).wallet
        verify(defaultLabels).getDefaultNonCustodialWalletLabel()
        verify(mockWallet).sharedKey

        verifyNoMoreInteractions(prefsUtil)
        verifyNoMoreInteractions(mockWallet)
    }

    @Test
    fun updatePayloadSuccessfulUpgradeWallet() {
        // Arrange
        val password = "password"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(Completable.complete())

        val mockAccount: Account = mock {
            on { label }.thenReturn("label")
        }
        whenever(payloadManager.getAccount(0)).thenReturn(mockAccount)
        val mockWallet: Wallet = mock {
            on { sharedKey }.thenReturn(SHARED_KEY)
        }
        whenever(payloadManager.wallet).thenReturn(mockWallet)
        whenever(payloadManager.isWalletUpgradeRequired).thenReturn(true)
        whenever(prefsUtil.isNewlyCreated).thenReturn(false)

        // Act
        subject.updatePayload(password, false)

        // Assert
        assertTrue(subject.canShowFingerprintDialog)

        verify(view).showProgressDialog(anyInt())
        verify(view).dismissProgressDialog()
        verify(view).walletUpgradeRequired(anyInt(), anyBoolean())
        verify(prefsUtil).walletGuid
        verify(prefsUtil).sharedKey
        verify(prefsUtil).isNewlyCreated
        verify(prefsUtil).sharedKey = SHARED_KEY
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(payloadManager).isWalletUpgradeRequired
        verify(payloadManager, atLeastOnce()).wallet

        verifyNoMoreInteractions(view)
        verifyNoMoreInteractions(prefsUtil)
        verifyNoMoreInteractions(payloadManager)
    }

    @Test
    fun updatePayloadSuccessfulVerifyPin() {
        // Arrange
        val password = "password"
        whenever(payloadManager.initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password))
            .thenReturn(Completable.complete())

        val mockAccount: Account = mock {
            on { label }.thenReturn("label")
        }
        whenever(payloadManager.getAccount(0)).thenReturn(mockAccount)
        whenever(payloadManager.isWalletUpgradeRequired).thenReturn(false)

        val mockWallet: Wallet = mock {
            on { sharedKey }.thenReturn(SHARED_KEY)
        }
        whenever(payloadManager.wallet).thenReturn(mockWallet)
        whenever(prefsUtil.isNewlyCreated).thenReturn(false)

        // Act
        subject.updatePayload(password, false)

        // Assert
        assertTrue(subject.canShowFingerprintDialog)

        verify(view).showProgressDialog(anyInt())
        verify(view).dismissProgressDialog()
        verify(payloadManager).initializeAndDecrypt(SHARED_KEY, WALLET_GUID, password)
        verify(payloadManager).isWalletUpgradeRequired
        verify(payloadManager).wallet
        verify(prefsUtil).sharedKey = SHARED_KEY
        verify(view).restartAppWithVerifiedPin()
        verify(mockWallet).sharedKey

        verifyNoMoreInteractions(mockWallet)
        verifyNoMoreInteractions(payloadManager)
    }

    @Test
    fun incrementFailureCount() {
        // Act
        subject.incrementFailureCountAndRestart()
        // Assert
        verify(prefsUtil).pinFails
        verify(prefsUtil).pinFails = anyInt()
        verify(view).showSnackbar(anyInt(), any(), any())
    }

    @Test
    fun resetApp() {
        // Act
        subject.resetApp()
        // Assert
        verify(credentialsWiper).wipe()
    }

    @Test
    fun allowExit() {
        // Act
        val allowExit = subject.allowExit()
        // Assert
        assertEquals(subject.allowExit(), allowExit)
    }

    @Test
    fun isCreatingNewPin() {
        whenever(prefsUtil.pinId).thenReturn("")
        // Act
        val creatingNewPin = subject.isCreatingNewPin
        // Assert
        assertTrue(creatingNewPin)
    }

    @Test
    fun isNotCreatingNewPin() {
        // Arrange
        whenever(prefsUtil.pinId).thenReturn("1234567890")
        // Act
        val creatingNewPin = subject.isCreatingNewPin
        // Assert
        assertFalse(creatingNewPin)
    }

    @Test
    fun fetchInfoMessage() {
        // Arrange
        val mobileNoticeDialog = MobileNoticeDialog(
            "title",
            "body",
            "primarybutton",
            "link"
        )
        whenever(mobileNoticeRemoteConfig.mobileNoticeDialog()).thenReturn(Single.just(mobileNoticeDialog))

        // Act
        subject.fetchInfoMessage()
        // Assert
        verify(view).showMobileNotice(mobileNoticeDialog)
    }

    @Test
    fun checkForceUpgradeStatus_false() {
        // Arrange
        val versionName = "281"
        whenever(walletOptionsDataManager.checkForceUpgrade(versionName))
            .thenReturn(Observable.just(UpdateType.NONE))

        // Act
        subject.checkForceUpgradeStatus(versionName)

        // Assert
        verify(walletOptionsDataManager).checkForceUpgrade(versionName)
        verifyZeroInteractions(view)
    }

    @Test
    fun checkForceUpgradeStatus_true() {
        // Arrange
        val versionName = "281"
        whenever(walletOptionsDataManager.checkForceUpgrade(versionName))
            .thenReturn(Observable.just(UpdateType.FORCE))

        // Act
        subject.checkForceUpgradeStatus(versionName)

        // Assert
        verify(walletOptionsDataManager).checkForceUpgrade(versionName)
        verify(view).appNeedsUpgrade(true)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun finishSignup_success() {
        subject.finishSignupProcess()

        verify(view).restartAppWithVerifiedPin()
    }

    @Test
    fun handlePayloadUpdateComplete_needsUpgrade() {
        val mockWallet = mock<Wallet>()
        whenever(payloadManager.wallet).thenReturn(mockWallet)
        whenever(mockWallet.sharedKey).thenReturn("")
        whenever(payloadManager.isWalletUpgradeRequired).thenReturn(true)

        subject.handlePayloadUpdateComplete(false)

        verify(view).walletUpgradeRequired(anyInt(), anyBoolean())
    }

    @Test
    fun handlePayloadUpdateComplete_fromPinCreationAndBiometricsEnabled() {
        val mockWallet: Wallet = mock {
            on { sharedKey }.thenReturn(SHARED_KEY)
        }
        whenever(payloadManager.wallet).thenReturn(mockWallet)
        whenever(biometricsController.isBiometricAuthEnabled).thenReturn(true)

        subject.handlePayloadUpdateComplete(true)

        verify(view).askToUseBiometrics()
    }

    @Test
    fun handlePayloadUpdateComplete_fromPinCreationAndBiometricsNotEnabled() {
        val mockWallet: Wallet = mock {
            on { sharedKey }.thenReturn(SHARED_KEY)
        }
        whenever(payloadManager.wallet).thenReturn(mockWallet)
        whenever(biometricsController.isBiometricAuthEnabled).thenReturn(false)

        subject.handlePayloadUpdateComplete(true)

        verify(view).restartAppWithVerifiedPin()
    }

    @Test
    fun handlePayloadUpdateComplete_notFromPinCreation() {
        val mockWallet: Wallet = mock {
            on { sharedKey }.thenReturn(SHARED_KEY)
        }
        whenever(payloadManager.wallet).thenReturn(mockWallet)
        whenever(biometricsController.isBiometricAuthEnabled).thenReturn(true)

        subject.handlePayloadUpdateComplete(false)

        verify(view).restartAppWithVerifiedPin()
        verify(mockWallet).sharedKey

        verifyNoMoreInteractions(mockWallet)
    }

    companion object {
        private const val WALLET_GUID = "0000-0000-0000-0000-0000"
        private const val SHARED_KEY = "121212121212"
    }
}
