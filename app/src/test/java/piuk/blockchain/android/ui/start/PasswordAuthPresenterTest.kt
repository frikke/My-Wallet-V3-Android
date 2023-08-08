package piuk.blockchain.android.ui.start

import com.blockchain.android.testutils.rxInit
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.core.auth.AuthDataManager
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.coreandroid.utils.PrefsUtil
import com.blockchain.logging.RemoteLogger
import com.blockchain.preferences.AuthPrefs
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.payload.data.Wallet
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.verify
import piuk.blockchain.android.R
import piuk.blockchain.android.util.AppUtil
import retrofit2.Response

class TestAuthPresenter(
    override val appUtil: AppUtil,
    override val authDataManager: AuthDataManager,
    override val payloadDataManager: PayloadDataManager,
    override val authPrefs: AuthPrefs,
    override val remoteLogger: RemoteLogger
) : PasswordAuthPresenter<PasswordAuthView>() {
    override fun onAuthFailed() {
        super.onAuthFailed()
        showErrorSnackbar(1)
    }

    override fun onAuthComplete() {
        super.onAuthComplete()
        showErrorSnackbar(2)
    }
}

class PasswordAuthPresenterTest {

    private lateinit var subject: TestAuthPresenter

    private val view: PasswordAuthView = mock()
    private val appUtil: AppUtil = mock()
    private val authDataManager: AuthDataManager = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val wallet: Wallet = mock()
    private val prefsUtil: PrefsUtil = mock()
    private val remoteLogger: RemoteLogger = mock()

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = TestAuthPresenter(
            appUtil,
            authDataManager,
            payloadDataManager,
            prefsUtil,
            remoteLogger
        )
        subject.attachView(view)

        whenever(wallet.guid).thenReturn(GUID)
        whenever(wallet.sharedKey).thenReturn("shared_key")
        whenever(payloadDataManager.wallet).thenReturn(wallet)
    }

    /**
     * Password is correct, should trigger [ManualPairingActivity.goToPinPage]
     */
    @Test
    fun onContinueClickedCorrectPassword() {
        // Arrange
        whenever(authDataManager.getSessionId()).thenReturn(Single.just("1234567890"))
        val responseBody = "{}".toResponseBody("application/json".toMediaTypeOrNull())
        val response = Response.success(responseBody)
        whenever(authDataManager.getEncryptedPayload(anyString(), anyBoolean()))
            .thenReturn(Single.just(response))
        whenever(payloadDataManager.initializeFromPayload(anyString(), anyString()))
            .thenReturn(Completable.complete())

        // Act
        subject.verifyPassword(PASSWORD, GUID)

        // Assert
        verify(view).goToPinPage()
        verify(prefsUtil).walletGuid = GUID
        verify(prefsUtil).sharedKey = any()
        verify(prefsUtil).emailVerified = true
    }

    /**
     * AuthDataManager returns a [DecryptionException], should trigger [ ][ManualPairingActivity.showSnackbar].
     */
    @Test
    fun onContinueClickedDecryptionFailure() {
        // Arrange
        whenever(authDataManager.getSessionId()).thenReturn(Single.just("1234567890"))
        val responseBody = "{}".toResponseBody("application/json".toMediaTypeOrNull())
        val response = Response.success(responseBody)
        whenever(authDataManager.getEncryptedPayload(anyString(), anyBoolean()))
            .thenReturn(Single.just(response))
        whenever(payloadDataManager.initializeFromPayload(anyString(), anyString()))
            .thenReturn(
                Completable.error(
                    DecryptionException()
                )
            )

        // Act
        subject.verifyPassword(PASSWORD, GUID)

        // Assert
        verify(view).showSnackbar(any(), any())
        verify(view).resetPasswordField()
        verify(view).dismissProgressDialog()
    }

    /**
     * AuthDataManager returns a [HDWalletException], should trigger [ ][ManualPairingActivity.showSnackbar].
     */
    @Test
    fun onContinueClickedHDWalletExceptionFailure() {
        // Arrange
        whenever(authDataManager.getSessionId()).thenReturn(Single.just("1234567890"))
        val responseBody = "{}".toResponseBody("application/json".toMediaTypeOrNull())
        val response = Response.success(responseBody)
        whenever(authDataManager.getEncryptedPayload(anyString(), anyBoolean()))
            .thenReturn(Single.just(response))
        whenever(payloadDataManager.initializeFromPayload(anyString(), anyString()))
            .thenReturn(
                Completable.error(
                    HDWalletException()
                )
            )

        // Act
        subject.verifyPassword(PASSWORD, GUID)

        // Assert
        verify(view).showSnackbar(any(), any())
        verify(view).resetPasswordField()
        verify(view).dismissProgressDialog()
    }

    /**
     * AuthDataManager returns a fatal exception, should restart the app and clear credentials.
     */
    @Test
    fun onContinueClickedFatalErrorClearData() {
        // Arrange
        whenever(authDataManager.getSessionId()).thenReturn(Single.just("1234567890"))
        val responseBody = "{}".toResponseBody("application/json".toMediaTypeOrNull())
        val response = Response.success(responseBody)
        whenever(authDataManager.getEncryptedPayload(anyString(), anyBoolean()))
            .thenReturn(Single.just(response))
        whenever(payloadDataManager.initializeFromPayload(anyString(), anyString()))
            .thenReturn(Completable.error(RuntimeException()))

        // Act
        subject.verifyPassword(PASSWORD, GUID)

        // Assert
        verify(view).showSnackbar(any(), any())
        verify(view).resetPasswordField()
        verify(view).dismissProgressDialog()
    }

    /**
     * Password is correct but 2FA is enabled, should trigger [ManualPairingActivity.showTwoFactorCodeNeededDialog]
     */
    @Test
    fun onContinueClickedCorrectPasswordTwoFa() {
        // Arrange
        whenever(authDataManager.getSessionId()).thenReturn(Single.just("1234567890"))
        val responseBody = TWO_FA_RESPONSE.toResponseBody("application/json".toMediaTypeOrNull())
        val response = Response.success(responseBody)
        whenever(authDataManager.getEncryptedPayload(anyString(), anyBoolean()))
            .thenReturn(Single.just(response))
        whenever(payloadDataManager.initializeFromPayload(anyString(), anyString()))
            .thenReturn(Completable.complete())

        // Act
        subject.verifyPassword(PASSWORD, GUID)

        // Assert
        verify(view).dismissProgressDialog()
        verify(view).showTwoFactorCodeNeededDialog(
            any(),
            any(),
            any(),
            any()
        )
    }

    /**
     * AuthDataManager returns a failure when getting encrypted payload, should trigger [ ][ManualPairingActivity.showSnackbar]
     */
    @Test
    fun onContinueClickedPairingFailure() {
        // Arrange
        whenever(authDataManager.getEncryptedPayload(anyString(), anyBoolean()))
            .thenReturn(Single.error(Throwable()))
        whenever(authDataManager.getSessionId()).thenReturn(Single.just("1234567890"))

        // Act
        subject.verifyPassword(PASSWORD, GUID)

        // Assert
        verify(view).showSnackbar(any(), any())
        verify(view).resetPasswordField()
        verify(view).dismissProgressDialog()
    }

    /**
     * AuthDataManager returns an error when getting session ID, should trigger
     * AppUtil#clearCredentialsAndRestart()
     */
    @Test
    fun onContinueClickedFatalError() {
        // Arrange
        whenever(authDataManager.getSessionId())
            .thenReturn(Single.error(Throwable()))
        // Act
        subject.verifyPassword(PASSWORD, GUID)
        // Assert

        verify(view).showSnackbar(any(), any())
        verify(view).resetPasswordField()
        verify(view).dismissProgressDialog()
    }

    @Test
    fun onContinueClickedEncryptedPayloadFailure() {
        // Arrange
        whenever(authDataManager.getSessionId())
            .thenReturn(Single.just("1234567890"))
        whenever(authDataManager.getEncryptedPayload(anyString(), anyBoolean()))
            .thenReturn(Single.error(Throwable()))
        // Act
        subject.verifyPassword(PASSWORD, GUID)
        // Assert

        verify(view).showSnackbar(any(), any())
        verify(view).resetPasswordField()
        verify(view).dismissProgressDialog()
    }

    /**
     * [AuthDataManager.startPollingAuthStatus] returns Access Required.
     * Should restart the app via AppUtil#clearCredentialsAndRestart()
     */
    @Test
    fun onContinueClickedWaitingForAuthRequired() {
        // Arrange
        whenever(authDataManager.getSessionId()).thenReturn(Single.just("1234567890"))
        val responseBody = KEY_AUTH_REQUIRED_JSON.toResponseBody("application/json".toMediaTypeOrNull())
        val response = Response.error<ResponseBody>(500, responseBody)
        whenever(authDataManager.getEncryptedPayload(anyString(), anyBoolean()))
            .thenReturn(Single.just(response))
        whenever(authDataManager.createCheckEmailTimer()).thenReturn(Observable.just(1))

        // Act
        subject.verifyPassword(PASSWORD, GUID)

        // Assert
        verify(view).showSnackbar(any(), any())
        verify(view).resetPasswordField()
    }

    /**
     * [AuthDataManager.startPollingAuthStatus] returns payload. Should
     * attempt to decrypt the payload.
     */
    @Test
    fun onContinueClickedWaitingForAuthSuccess() {
        // Arrange
        whenever(authDataManager.getSessionId()).thenReturn(Single.just("1234567890"))
        val responseBody = "{}".toResponseBody("application/json".toMediaTypeOrNull())
        val response = Response.success<ResponseBody>(200, responseBody)
        whenever(authDataManager.getEncryptedPayload(anyString(), anyBoolean()))
            .thenReturn(Single.just(response))
        whenever(authDataManager.createCheckEmailTimer()).thenReturn(Observable.just(1))
        whenever(payloadDataManager.initializeFromPayload(anyString(), anyString()))
            .thenReturn(Completable.complete())
        whenever(wallet.guid).thenReturn(GUID)
        whenever(wallet.sharedKey).thenReturn("shared_key")
        // Act
        subject.verifyPassword(PASSWORD, GUID)

        // Assert
        verify(payloadDataManager).initializeFromPayload(any(), any())
    }

    /**
     * [AuthDataManager.createCheckEmailTimer] throws an error. Should show error toast.
     */
    @Test
    fun onContinueClickedWaitingForAuthEmailTimerError() {
        // Arrange
        whenever(authDataManager.getSessionId()).thenReturn(Single.just("1234567890"))
        val responseBody = KEY_AUTH_REQUIRED_JSON.toResponseBody("application/json".toMediaTypeOrNull())
        val response = Response.error<ResponseBody>(500, responseBody)
        whenever(authDataManager.getEncryptedPayload(anyString(), anyBoolean()))
            .thenReturn(Single.just(response))
        whenever(authDataManager.createCheckEmailTimer())
            .thenReturn(Observable.error(Throwable()))
        whenever(payloadDataManager.initializeFromPayload(anyString(), anyString()))
            .thenReturn(Completable.complete())
        // Act
        subject.verifyPassword(PASSWORD, GUID)

        // Assert
        verify(view).showSnackbar(any(), any())
        verify(view).dismissProgressDialog()
        verify(view).resetPasswordField()
    }

    @Test
    fun onContinueClickedInitialErrorReturned() {
        // Arrange
        whenever(authDataManager.getSessionId()).thenReturn(Single.just("1234567890"))
        val responseBody = INITIAL_ERROR_RESPONSE.toResponseBody("application/json".toMediaTypeOrNull())
        val response = Response.error<ResponseBody>(500, responseBody)
        whenever(authDataManager.getEncryptedPayload(anyString(), anyBoolean()))
            .thenReturn(Single.just(response))

        // Act
        subject.verifyPassword(PASSWORD, GUID)

        // Assert
        verify(remoteLogger).logState("initial_error", "This is an error")
        verify(view).showErrorSnackbarWithParameter(
            com.blockchain.stringResources.R.string.common_replaceable_value,
            "This is an error"
        )
    }

    @Test
    fun onContinueClickedInitialErrorReturnedMalformed() {
        // Arrange
        whenever(authDataManager.getSessionId()).thenReturn(Single.just("1234567890"))
        val responseBody = INITIAL_ERROR_RESPONSE_MALFORMED.toResponseBody("application/json".toMediaTypeOrNull())
        val response = Response.error<ResponseBody>(500, responseBody)
        whenever(authDataManager.getEncryptedPayload(anyString(), anyBoolean()))
            .thenReturn(Single.just(response))

        // Act
        subject.verifyPassword(PASSWORD, GUID)

        // Assert
        verify(remoteLogger).logState(eq("initial_error"), any())
        verify(view).showSnackbar(com.blockchain.stringResources.R.string.common_error, SnackbarType.Error)
    }

    /**
     * [AuthDataManager.startPollingAuthStatus] returns an error. Should
     * restart the app via AppUtil#clearCredentialsAndRestart()
     */
    @Test
    fun onContinueClickedWaitingForAuthFailure() {
        // Arrange
        whenever(authDataManager.getSessionId()).thenReturn(Single.just("1234567890"))

        val responseBody = KEY_AUTH_REQUIRED_JSON.toResponseBody("application/json".toMediaTypeOrNull())
        val response = Response.error<ResponseBody>(500, responseBody)

        whenever(authDataManager.getEncryptedPayload(anyString(), anyBoolean()))
            .thenReturn(Single.error(Throwable()))
        whenever(authDataManager.createCheckEmailTimer()).thenReturn(Observable.just(1))

        // Act
        subject.verifyPassword(PASSWORD, GUID)

        // Assert
        verify(view).showSnackbar(any(), any())
        verify(view).resetPasswordField()
        verify(view).dismissProgressDialog()
    }

    /**
     * [AuthDataManager.startPollingAuthStatus] counts down to zero. Should
     * restart the app via AppUtil#clearCredentialsAndRestart()
     */
    @Test
    fun onContinueClickedWaitingForAuthCountdownComplete() {
        whenever(authDataManager.getSessionId()).thenReturn(Single.just("1234567890"))
        val responseBody = KEY_AUTH_REQUIRED_JSON.toResponseBody("application/json".toMediaTypeOrNull())
        val response = Response.error<ResponseBody>(500, responseBody)
        whenever(authDataManager.getEncryptedPayload(anyString(), anyBoolean()))
            .thenReturn(Single.just(response))
        whenever(authDataManager.createCheckEmailTimer()).thenReturn(Observable.just(0))

        // Act
        subject.verifyPassword(PASSWORD, GUID)

        // Assert
        verify(view).showSnackbar(any(), any())
        verify(view).resetPasswordField()
        verify(view).dismissProgressDialog()
    }

    @Test
    fun submitTwoFactorCodeNull() {
        // Arrange
        val responseObject = JSONObject()
        val sessionId = "SESSION_ID"
        // Act
        subject.submitTwoFactorCode(responseObject, GUID, PASSWORD, null)
        // Assert
        verify(view).showSnackbar(com.blockchain.stringResources.R.string.two_factor_null_error, SnackbarType.Error)
    }

    @Test
    fun submitTwoFactorCodeFailed() {
        // Arrange
        val responseObject = JSONObject()
        val sessionId = "SESSION_ID"
        val code = "123456"
        whenever(authDataManager.submitTwoFactorCode(GUID, code))
            .thenReturn(Single.error(Throwable()))
        // Act
        subject.submitTwoFactorCode(responseObject, GUID, PASSWORD, code)
        // Assert
        verify(view).showProgressDialog(com.blockchain.stringResources.R.string.please_wait, null)
        verify(view, atLeastOnce()).dismissProgressDialog()
        verify(view).showSnackbar(
            com.blockchain.stringResources.R.string.two_factor_incorrect_error,
            SnackbarType.Error
        )
        verify(authDataManager).submitTwoFactorCode(GUID, code)
    }

    @Test
    fun submitTwoFactorCodeSuccess() {
        // Arrange
        val responseObject = JSONObject()
        val sessionId = "SESSION_ID"
        val code = "123456"
        whenever(authDataManager.submitTwoFactorCode(GUID, code))
            .thenReturn(
                Single.just(
                    TWO_FA_RESPONSE.toResponseBody("application/json".toMediaTypeOrNull())
                )
            )
        whenever(payloadDataManager.initializeFromPayload(anyString(), anyString()))
            .thenReturn(
                Completable.complete()
            )

        // Act
        subject.submitTwoFactorCode(responseObject, GUID, PASSWORD, code)

        // Assert
        verify(view).showProgressDialog(com.blockchain.stringResources.R.string.please_wait, null)
        verify(view, atLeastOnce()).dismissProgressDialog()
        verify(view).goToPinPage()
        verify(authDataManager).submitTwoFactorCode(GUID, code)
        verify(payloadDataManager).initializeFromPayload(TWO_FA_PAYLOAD, PASSWORD)
    }

    @Test
    fun onProgressCancelled() {
        // Arrange

        // Act
        subject.onProgressCancelled()
        // Assert

        assertEquals(0, subject.compositeDisposable.size().toLong())
        assertEquals(0, subject.timerDisposable.size().toLong())
    }

    companion object {
        private const val KEY_AUTH_REQUIRED_JSON = "{\"authorization_required\": true}"
        private const val TWO_FA_RESPONSE = "{auth_type: 5}"
        private const val TWO_FA_PAYLOAD = "{\"payload\":\"{auth_type: 5}\"}"
        private const val INITIAL_ERROR_RESPONSE = "{\"initial_error\":\"This is an error\"}"
        private const val INITIAL_ERROR_RESPONSE_MALFORMED = "{{}]\"initial_error\":\"This is an error\"}]}}"

        private const val GUID = "1234567890"
        private const val PASSWORD = "PASSWORD"
    }
}
