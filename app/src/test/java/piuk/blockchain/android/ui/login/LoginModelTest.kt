package piuk.blockchain.android.ui.login

import android.content.Intent
import android.net.Uri
import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.login.auth.LoginAuthActivity
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class LoginModelTest {

    private lateinit var model: LoginModel

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    private val interactor: LoginInteractor = mock()

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = LoginModel(
            initialState = LoginState(),
            mainScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            crashLogger = mock(),
            interactor = interactor
        )
    }

    @Test
    fun `pairWithQR success`() {
        // Arrange
        val qrCode = "QR_CODE"
        whenever(interactor.loginWithQrCode(qrCode)).thenReturn(Completable.complete())
        val testState = model.state.test()
        model.process(LoginIntents.LoginWithQr(qrCode))

        // Assert
        testState
            .assertValues(
                LoginState(),
                LoginState(currentStep = LoginStep.LOG_IN),
                LoginState(currentStep = LoginStep.ENTER_PIN)
            )
    }

    @Test
    fun `pairWithQR fail`() {
        // Arrange
        val qrCode = "QR_CODE"
        whenever(interactor.loginWithQrCode(qrCode)).thenReturn(Completable.error(Throwable()))

        val testState = model.state.test()
        model.process(LoginIntents.LoginWithQr(qrCode))

        // Assert
        testState
            .assertValues(
                LoginState(),
                LoginState(currentStep = LoginStep.LOG_IN),
                LoginState(currentStep = LoginStep.SHOW_SCAN_ERROR)
            )
    }

    @Test
    fun `enter email manually`() {
        // Arrange
        val email = "test@gmail.com"

        val testState = model.state.test()
        model.process(LoginIntents.UpdateEmail(email))

        // Assert
        testState
            .assertValues(
                LoginState(),
                LoginState(email = email, currentStep = LoginStep.ENTER_EMAIL)
            )
    }

    @Test
    fun `create session ID and send email successfully`() {
        // Arrange
        val email = "test@gmail.com"
        val sessionId = "sessionId"
        val captcha = "captcha"
        whenever(interactor.obtainSessionId(email)).thenReturn(
            Single.just(
                "{token: $sessionId}".toResponseBody("application/json".toMediaTypeOrNull())
            )
        )
        whenever(interactor.sendEmailForVerification(sessionId, email, captcha)).thenReturn(
            Completable.complete()
        )

        val testState = model.state.test()
        model.process(LoginIntents.ObtainSessionIdForEmail(email, captcha))

        // Assert
        testState
            .assertValues(
                LoginState(),
                LoginState(email = email, captcha = captcha, currentStep = LoginStep.GET_SESSION_ID),
                LoginState(email = email, sessionId = sessionId, captcha = captcha, currentStep = LoginStep.SEND_EMAIL),
                LoginState(
                    email = email, sessionId = sessionId, captcha = captcha, currentStep = LoginStep.VERIFY_DEVICE
                )
            )
    }

    @Test
    fun `fail to create session ID`() {
        // Arrange
        val email = "test@gmail.com"
        val captcha = "captcha"
        whenever(interactor.obtainSessionId(email)).thenReturn(
            Single.error(Exception())
        )

        val testState = model.state.test()
        model.process(LoginIntents.ObtainSessionIdForEmail(email, captcha))

        // Assert
        testState
            .assertValues(
                LoginState(),
                LoginState(email = email, captcha = captcha, currentStep = LoginStep.GET_SESSION_ID),
                LoginState(email = email, captcha = captcha, currentStep = LoginStep.SHOW_SESSION_ERROR)
            )
    }

    @Test
    fun `fail to send email`() {
        // Arrange
        val email = "test@gmail.com"
        val sessionId = "sessionId"
        val captcha = "captcha"

        whenever(interactor.obtainSessionId(email)).thenReturn(
            Single.just(
                "{token: $sessionId}".toResponseBody("application/json".toMediaTypeOrNull())
            )
        )
        whenever(interactor.sendEmailForVerification(sessionId, email, captcha)).thenReturn(
            Completable.error(Throwable())
        )

        val testState = model.state.test()
        model.process(LoginIntents.ObtainSessionIdForEmail(email, captcha))

        // Assert
        testState
            .assertValues(
                LoginState(),
                LoginState(email = email, captcha = captcha, currentStep = LoginStep.GET_SESSION_ID),
                LoginState(email = email, sessionId = sessionId, captcha = captcha, currentStep = LoginStep.SEND_EMAIL),
                LoginState(
                    email = email,
                    sessionId = sessionId,
                    captcha = captcha,
                    currentStep = LoginStep.SHOW_EMAIL_ERROR
                )
            )
    }

    @Test
    fun `check deeplink returns logged in intent`() {
        val uri: Uri = mock()
        val action = Intent.ACTION_VIEW

        whenever(interactor.checkSessionDetails(action, uri)).thenReturn(
            LoginIntents.UserIsLoggedIn
        )

        val testState = model.state.test()
        model.process(LoginIntents.CheckForExistingSessionOrDeepLink(action, uri))

        testState
            .assertValues(
                LoginState(),
                LoginState(currentStep = LoginStep.ENTER_PIN)
            )
    }

    @Test
    fun `check deeplink returns auth required intent`() {
        val uri: Uri = mock()
        val action = Intent.ACTION_VIEW
        val intent = Intent(action, uri, mock(), LoginAuthActivity::class.java)
        whenever(interactor.checkSessionDetails(action, uri)).thenReturn(
            LoginIntents.UserAuthenticationRequired(action, uri)
        )

        val testState = model.state.test()
        model.process(LoginIntents.CheckForExistingSessionOrDeepLink(action, uri))

        testState
            .assertValues(
                LoginState(),
                LoginState(intentAction = action, intentUri = uri, currentStep = LoginStep.NAVIGATE_FROM_DEEPLINK)
            )
    }

    @Test
    fun `check deeplink returns error intent`() {
        val uri: Uri = mock()
        val action = Intent.ACTION_VIEW

        whenever(interactor.checkSessionDetails(action, uri)).thenReturn(
            LoginIntents.UnknownError
        )

        val testState = model.state.test()
        model.process(LoginIntents.CheckForExistingSessionOrDeepLink(action, uri))

        testState
            .assertValues(
                LoginState(),
                LoginState(currentStep = LoginStep.UNKNOWN_ERROR)
            )
    }
}