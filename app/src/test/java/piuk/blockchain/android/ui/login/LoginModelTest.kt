package piuk.blockchain.android.ui.login

import android.content.Intent
import android.net.Uri
import com.blockchain.android.testutils.rxInit
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.network.PollResult
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.maintenance.domain.usecase.GetAppMaintenanceConfigUseCase
import piuk.blockchain.android.ui.login.auth.LoginAuthInfo

class LoginModelTest {

    private lateinit var model: LoginModel

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    private val interactor: LoginInteractor = mock()

    private val getAppMaintenanceConfigUseCase: GetAppMaintenanceConfigUseCase = mock()

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
            remoteLogger = mock(),
            interactor = interactor,
            getAppMaintenanceConfigUseCase,
            analytics = mock()
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

        whenever(interactor.pollForAuth(any(), any())).thenReturn(
            Single.just(PollResult.Cancel(mock()))
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
                ),
                LoginState(
                    email = email, captcha = captcha, sessionId = sessionId, currentStep = LoginStep.VERIFY_DEVICE,
                    pollingState = AuthPollingState.POLLING
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
            LoginIntents.UserLoggedInWithoutDeeplinkData
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

    @Test
    fun `check polling starts on initial intent`() {
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

        whenever(interactor.pollForAuth(eq(sessionId), any())).thenReturn(
            Single.just(PollResult.Cancel(mock())) // cancel here is a return with no effect on the model
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
                    email = email, sessionId = sessionId, captcha = captcha, currentStep = LoginStep.VERIFY_DEVICE,
                    pollingState = AuthPollingState.NOT_STARTED
                ),
                LoginState(
                    email = email, sessionId = sessionId, captcha = captcha, currentStep = LoginStep.VERIFY_DEVICE,
                    pollingState = AuthPollingState.POLLING
                )
            )
    }

    @Test
    fun `check timeout works`() {
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

        whenever(interactor.pollForAuth(eq(sessionId), any())).thenReturn(
            Single.just(PollResult.TimeOut(mock())) // cancel here is a return with no effect on the model
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
                    email = email, sessionId = sessionId, captcha = captcha, currentStep = LoginStep.VERIFY_DEVICE,
                    pollingState = AuthPollingState.NOT_STARTED
                ),
                LoginState(
                    email = email, sessionId = sessionId, captcha = captcha, currentStep = LoginStep.VERIFY_DEVICE,
                    pollingState = AuthPollingState.POLLING
                ),
                LoginState(
                    email = email, sessionId = sessionId, captcha = captcha,
                    currentStep = LoginStep.POLLING_PAYLOAD_ERROR,
                    pollingState = AuthPollingState.TIMEOUT
                )
            )
    }

    @Test
    fun `check polling full payload parsed correctly`() {
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

        whenever(interactor.pollForAuth(eq(sessionId), any())).thenReturn(
            Single.just(PollResult.FinalResult(fullPayload.toResponseBody()))
        )

        val jsonBuilder = Json {
            ignoreUnknownKeys = true
        }
        val expectedPayload = jsonBuilder.decodeFromString<LoginAuthInfo.ExtendedAccountInfo>(fullPayload)

        val testState = model.state.test()
        model.process(LoginIntents.ObtainSessionIdForEmail(email, captcha))

        // Assert
        testState
            .assertValues(
                LoginState(),
                LoginState(
                    email = email,
                    captcha = captcha,
                    currentStep = LoginStep.GET_SESSION_ID
                ),
                LoginState(
                    email = email,
                    sessionId = sessionId,
                    captcha = captcha,
                    currentStep = LoginStep.SEND_EMAIL
                ),
                LoginState(
                    email = email,
                    sessionId = sessionId,
                    captcha = captcha,
                    currentStep = LoginStep.VERIFY_DEVICE,
                    pollingState = AuthPollingState.NOT_STARTED
                ),
                LoginState(
                    email = email,
                    sessionId = sessionId,
                    captcha = captcha,
                    currentStep = LoginStep.VERIFY_DEVICE,
                    pollingState = AuthPollingState.POLLING
                ),
                LoginState(
                    email = email,
                    sessionId = sessionId,
                    captcha = captcha,
                    currentStep = LoginStep.NAVIGATE_FROM_PAYLOAD,
                    pollingState = AuthPollingState.COMPLETE,
                    payload = expectedPayload
                )
            )
    }

    @Test
    fun `check polling denial payload true parsed correctly`() {
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

        whenever(interactor.pollForAuth(eq(sessionId), any())).thenReturn(
            Single.just(PollResult.FinalResult(deniedPayload.toResponseBody()))
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
                    email = email, sessionId = sessionId, captcha = captcha, currentStep = LoginStep.VERIFY_DEVICE,
                    pollingState = AuthPollingState.NOT_STARTED
                ),
                LoginState(
                    email = email, sessionId = sessionId, captcha = captcha, currentStep = LoginStep.VERIFY_DEVICE,
                    pollingState = AuthPollingState.POLLING
                ),
                LoginState(
                    email = email, sessionId = sessionId, captcha = captcha,
                    currentStep = LoginStep.POLLING_PAYLOAD_ERROR,
                    pollingState = AuthPollingState.DENIED
                )
            )
    }

    @Test
    fun `check polling denial payload false throws error`() {
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

        whenever(interactor.pollForAuth(eq(sessionId), any())).thenReturn(
            Single.just(PollResult.FinalResult(deniedPayloadFalse.toResponseBody()))
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
                    email = email, sessionId = sessionId, captcha = captcha, currentStep = LoginStep.VERIFY_DEVICE,
                    pollingState = AuthPollingState.NOT_STARTED
                ),
                LoginState(
                    email = email, sessionId = sessionId, captcha = captcha, currentStep = LoginStep.VERIFY_DEVICE,
                    pollingState = AuthPollingState.POLLING
                ),
                LoginState(
                    email = email, sessionId = sessionId, captcha = captcha,
                    currentStep = LoginStep.POLLING_PAYLOAD_ERROR,
                    pollingState = AuthPollingState.ERROR
                )
            )
    }

    @Test
    fun `check polling unknown payload throws error`() {
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

        whenever(interactor.pollForAuth(eq(sessionId), any())).thenReturn(
            Single.just(PollResult.FinalResult(unknownPayload.toResponseBody()))
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
                    email = email, sessionId = sessionId, captcha = captcha, currentStep = LoginStep.VERIFY_DEVICE,
                    pollingState = AuthPollingState.NOT_STARTED
                ),
                LoginState(
                    email = email, sessionId = sessionId, captcha = captcha, currentStep = LoginStep.VERIFY_DEVICE,
                    pollingState = AuthPollingState.POLLING
                ),
                LoginState(
                    email = email, sessionId = sessionId, captcha = captcha,
                    currentStep = LoginStep.POLLING_PAYLOAD_ERROR,
                    pollingState = AuthPollingState.ERROR
                )
            )
    }

    @Test
    fun `check cancel polling works`() {
        // TODO in AND-5317
    }

    @Test
    fun `check approval status updates when approved`() {
        // TODO in AND-5317
    }

    @Test
    fun `check approval status updates when denied`() {
        // TODO in AND-5317
    }

    @Test
    fun `check approval status updates when api conflict`() {
        // TODO in AND-5317
    }

    @Test
    fun `check navigation after error when logged in`() {
        // TODO in AND-5317
    }

    companion object {
        private const val fullPayload = "{\n" +
            "    \"wallet\": {\n" +
            "        \"guid\": \"9919ef4a-0206-4993-a03d-5829a81b0a36\",\n" +
            "        \"email\": \"test@gmail.com\",\n" +
            "        \"session_id\": \"sessionId\",\n" +
            "        \"email_code\": \"123\",\n" +
            "        \"is_mobile_setup\": true,\n" +
            "        \"mobile_device_type\": 0,\n" +
            "        \"last_mnemonic_backup\": 1621954780,\n" +
            "        \"has_cloud_backup\": true,\n" +
            "        \"two_fa_type\": 4,\n" +
            "        \"nabu\": {\n" +
            "            \"user_id\": \"d97a1c42-8928-4159-9053-39cd2c33f997\",\n" +
            "\t\t\t\t\t\t\"recovery_token\": \"03fc86b2-4dcb-4fab-8a54-e0c961a3b183\"\n" +
            "        },\n" +
            "        \"exchange\": {\n" +
            "            \"user_credentials_id\": \"b315a95c-32ea-438d-bcb1-f3135deb8c9b\",\n" +
            "            \"two_fa_mode\": true\n" +
            "        }\n" +
            "    },\n" +
            "    \"unified\": false,\n" +
            "    \"upgradeable\": false,\n" +
            "    \"mergeable\": false,\n" +
            "    \"user_type\": \"WALLET_EXCHANGE_LINKED\"\n" +
            "}"

        private const val deniedPayload = "{\n" +
            "\"request_denied\": true\n" +
            "\"response_type\": \"REQUEST_DENIED\"\n" +
            "}"

        private const val deniedPayloadFalse = "{\n" +
            "\t\"request_denied\": false\n" +
            "}"

        private const val unknownPayload = "{GARBAGE}"
    }
}
