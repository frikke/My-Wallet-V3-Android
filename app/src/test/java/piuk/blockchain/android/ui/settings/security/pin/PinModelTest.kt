package piuk.blockchain.android.ui.settings.security.pin

import com.blockchain.android.testutils.rxInit
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.ProviderSpecificAnalytics
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.settings.v2.security.pin.BiometricStatus
import piuk.blockchain.android.ui.settings.v2.security.pin.PasswordError
import piuk.blockchain.android.ui.settings.v2.security.pin.PasswordStatus
import piuk.blockchain.android.ui.settings.v2.security.pin.PayloadError
import piuk.blockchain.android.ui.settings.v2.security.pin.PayloadStatus
import piuk.blockchain.android.ui.settings.v2.security.pin.PinError
import piuk.blockchain.android.ui.settings.v2.security.pin.PinIntent
import piuk.blockchain.android.ui.settings.v2.security.pin.PinInteractor
import piuk.blockchain.android.ui.settings.v2.security.pin.PinModel
import piuk.blockchain.android.ui.settings.v2.security.pin.PinScreenView
import piuk.blockchain.android.ui.settings.v2.security.pin.PinState
import piuk.blockchain.android.ui.settings.v2.security.pin.PinStatus

// TODO working on it
class PinModelTest {

    private lateinit var model: PinModel

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    private val specificAnalytics = mock<ProviderSpecificAnalytics>()
    private val analytics = mock<Analytics>()
    private val interactor = mock<PinInteractor>()

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = PinModel(
            initialState = PinState(),
            mainScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            crashLogger = mock(),
            interactor = interactor,
            specificAnalytics = specificAnalytics,
            analytics = analytics
        )
    }

    @Test
    fun `load correct view for action CreateNewPin `() {
        whenever(interactor.isCreatingNewPin()).thenReturn(true)

        val testState = model.state.test()
        model.process(PinIntent.GetAction)

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                action = PinScreenView.CreateNewPin
            )
        }
    }

    @Test
    fun `load correct view for action ConfirmNewPin `() {
        whenever(interactor.isConfirmingPin()).thenReturn(true)

        val testState = model.state.test()
        model.process(PinIntent.GetAction)

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                action = PinScreenView.ConfirmNewPin
            )
        }
    }

    @Test
    fun `load correct view for action LoginWithPin`() {
        whenever(interactor.isCreatingNewPin()).thenReturn(false)
        whenever(interactor.isConfirmingPin()).thenReturn(false)

        val testState = model.state.test()
        model.process(PinIntent.GetAction)

        testState.assertValueAt(0) {
            it == PinState()
        }
    }

    @Test
    fun `load current pin for intent GetCurrentPin`() {
        val pin = "2222"
        whenever(interactor.getCurrentPin()).thenReturn(pin)

        val testState = model.state.test()
        model.process(PinIntent.GetCurrentPin)

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                pinStatus = PinStatus(
                    currentPin = pin,
                    isPinValidated = it.pinStatus.isPinValidated,
                    isFromPinCreation = it.pinStatus.isFromPinCreation
                )
            )
        }
    }

    @Test
    fun `CheckNumPinAttempts to show error because hasExceededPinAttempts`() {
        whenever(interactor.hasExceededPinAttempts()).thenReturn(true)

        val testState = model.state.test()
        model.process(PinIntent.CheckNumPinAttempts)

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                error = PinError.NUM_ATTEMPTS_EXCEEDED
            )
        }
    }

    @Test
    fun `CheckNumPinAttempts not to show error !hasExceededPinAttempts`() {
        whenever(interactor.hasExceededPinAttempts()).thenReturn(false)

        val testState = model.state.test()
        model.process(PinIntent.CheckNumPinAttempts)

        testState.assertValueAt(0) {
            it == PinState()
        }
    }

    @Ignore
    @Test
    fun `CreatePIN is succeeded`() {
        val tempPassword = "Test1234!"
        val pin = "1234"
        whenever(interactor.getTempPassword()).thenReturn(tempPassword)
        whenever(interactor.createPin(tempPassword, pin)).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(PinIntent.CreatePIN(pin))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                biometricStatus = BiometricStatus(
                    shouldShowFingerprint = it.biometricStatus.shouldShowFingerprint,
                    canShowFingerprint = it.biometricStatus.canShowFingerprint,
                    isBiometricsEnabled = false
                )
            )
        }.assertValueAt(2) {
            it == PinState(
                payloadStatus = PayloadStatus(
                    isPayloadCompleted = true,
                    payloadError = PayloadError.NONE
                ),
                pinStatus = PinStatus(
                    isFromPinCreation = true,
                    currentPin = it.pinStatus.currentPin,
                    isPinValidated = it.pinStatus.isPinValidated
                )
            )
        }
    }

    @Ignore
    @Test
    fun `CreatePIN is failing`() {
        val tempPassword = "Test1234!"
        val pin = "1234"
        whenever(interactor.getTempPassword()).thenReturn(tempPassword)
        whenever(interactor.createPin(tempPassword, pin)).thenReturn(Completable.error(Throwable()))

        val testState = model.state.test()
        model.process(PinIntent.CreatePIN(pin))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                error = PinError.CREATE_PIN_FAILED
            )
        }
    }

    @Ignore
    @Test
    fun `ValidatePassword succeeded`() {
        val password = "Test1234!"
        whenever(interactor.validatePassword(password)).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(PinIntent.ValidatePassword(password))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                passwordStatus = PasswordStatus(
                    isPasswordValid = true,
                    passwordError = it.passwordStatus?.passwordError ?: PasswordError.NONE
                )
            )
        }
    }
}
