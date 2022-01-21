package piuk.blockchain.android.ui.settings

import com.blockchain.android.testutils.rxInit
import com.blockchain.enviroment.EnvironmentConfig
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.settings.v2.profile.SMSVerificationIntent
import piuk.blockchain.android.ui.settings.v2.profile.SMSVerificationInteractor
import piuk.blockchain.android.ui.settings.v2.profile.SMSVerificationModel
import piuk.blockchain.android.ui.settings.v2.profile.SMSVerificationState
import piuk.blockchain.android.ui.settings.v2.profile.VerificationError

class SMSVerificationModelTest {
    private lateinit var model: SMSVerificationModel

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    private val interactor: SMSVerificationInteractor = mock()

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = SMSVerificationModel(
            initialState = SMSVerificationState(),
            mainScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            crashLogger = mock(),
            interactor = interactor,
            _activityIndicator = mock()
        )
    }

    @Test
    fun `when ResendCode is successfully then state isVerificationSent codeSent will be true`() {
        val settings: Settings = mock {
            on { smsNumber }.thenReturn("+34655819515")
        }

        whenever(
            interactor.resendCodeSMS(settings.smsNumber)
        ).thenReturn(Single.just(settings))

        val testState = model.state.test()
        model.process(SMSVerificationIntent.ResendCodeSMS(settings.smsNumber))

        testState
            .assertValueAt(0) {
                it == SMSVerificationState()
            }.assertValueAt(1) {
                it == SMSVerificationState(
                    isLoading = true
                )
            }.assertValueAt(2) {
                it == SMSVerificationState(
                    isLoading = false,
                    isCodeSmsSent = true
                )
            }
    }

    @Test
    fun `when ResendCode fails then state should contain ResendSmsError`() {
        val phoneNumber = "+34655819515"
        whenever(
            interactor.resendCodeSMS(phoneNumber)
        ).thenReturn(
            Single.error { Throwable() }
        )

        val testState = model.state.test()
        model.process(SMSVerificationIntent.ResendCodeSMS(phoneNumber))

        testState.assertValueAt(0) {
            it == SMSVerificationState()
        }.assertValueAt(1) {
            it == SMSVerificationState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == SMSVerificationState(
                isLoading = false,
                error = VerificationError.ResendSmsError
            )
        }
    }

    @Test
    fun `when VerifyPhone is successfully then isPhoneVerified`() {
        val code = "1234AB"

        whenever(interactor.verifyPhoneNumber(code))
            .thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(SMSVerificationIntent.VerifyPhoneNumber(code))

        testState
            .assertValueAt(0) {
                it == SMSVerificationState()
            }.assertValueAt(1) {
                it == SMSVerificationState(
                    isLoading = true
                )
            }.assertValueAt(2) {
                it == SMSVerificationState(
                    isLoading = false,
                    isPhoneVerified = true
                )
            }
    }

    @Test
    fun `when VerifyPhone fails then state should contain VerifyPhoneError`() {
        val code = "1234AB"

        whenever(interactor.verifyPhoneNumber(code = code))
            .thenReturn(Completable.error { Throwable() })

        val testState = model.state.test()
        model.process(SMSVerificationIntent.VerifyPhoneNumber(code))

        testState.assertValueAt(0) {
            it == SMSVerificationState()
        }.assertValueAt(1) {
            it == SMSVerificationState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == SMSVerificationState(
                isLoading = false,
                error = VerificationError.VerifyPhoneError
            )
        }
    }
}
