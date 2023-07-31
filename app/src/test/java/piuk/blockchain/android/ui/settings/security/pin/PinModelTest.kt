package piuk.blockchain.android.ui.settings.security.pin

import com.blockchain.analytics.Analytics
import com.blockchain.analytics.ProviderSpecificAnalytics
import com.blockchain.android.testutils.rxInit
import com.blockchain.enviroment.EnvironmentConfig
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.exceptions.AccountLockedException
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import info.blockchain.wallet.exceptions.ServerConnectionException
import info.blockchain.wallet.exceptions.UnsupportedVersionException
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.net.SocketTimeoutException
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.spongycastle.crypto.InvalidCipherTextException
import piuk.blockchain.android.ui.auth.MobileNoticeDialog

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
            remoteLogger = mock(),
            interactor = interactor,
            specificAnalytics = specificAnalytics,
            analytics = analytics,
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

    @Test
    fun `CheckApiStatus succeeded will update apiStatus to true or false`() {
        whenever(interactor.checkApiStatus()).thenReturn(Single.just(true))

        val testState = model.state.test()
        model.process(PinIntent.CheckApiStatus)

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(0) {
            it == PinState(
                isApiHealthyStatus = true
            )
        }
    }

    @Test
    fun `CheckApiStatus fails will do nothing, just print exception`() {
        whenever(interactor.checkApiStatus()).thenReturn(Single.error(Throwable()))

        val testState = model.state.test()
        model.process(PinIntent.CheckApiStatus)

        testState.assertValueAt(0) {
            it == PinState()
        }
    }

    @Test
    fun `CheckFingerprint update biometricStatus depending on shouldShowFingerprintLogin`() {
        val shouldShow = interactor.shouldShowFingerprintLogin()

        val testState = model.state.test()
        model.process(PinIntent.CheckFingerprint)

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(0) {
            it == PinState(
                biometricStatus = BiometricStatus(
                    shouldShowFingerprint = shouldShow,
                    canShowFingerprint = it.biometricStatus.canShowFingerprint
                )
            )
        }
    }

    @Test
    fun `FetchRemoteMobileNotice success then show dialog`() {
        val mobileNoticeDialog = MobileNoticeDialog(
            title = "Attention",
            body = "This is an important message",
            ctaText = "Understood",
            ctaLink = "www.blockchain.com"
        )
        whenever(interactor.fetchInfoMessage()).thenReturn(Single.just(mobileNoticeDialog))

        val testState = model.state.test()
        model.process(PinIntent.FetchRemoteMobileNotice)

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                showMobileNotice = mobileNoticeDialog
            )
        }
    }

    @Test
    fun `FetchRemoteMobileNotice fails then don't show anything, just print exception`() {
        whenever(interactor.fetchInfoMessage()).thenReturn(Single.error(Throwable()))

        val testState = model.state.test()
        model.process(PinIntent.FetchRemoteMobileNotice)

        testState.assertValueAt(0) {
            it == PinState()
        }
    }

    @Test
    fun `UpdatePayload succeeded and isWalletUpgradeRequired then UpgradeRequired`() {
        val password = "Test1234!"
        val pin = "1234"
        val isFromPinCreation = false
        val SECOND_PASSWORD_ATTEMPTS = 5

        whenever(interactor.isWalletUpgradeRequired()).thenReturn(true)
        whenever(interactor.validatePIN(pin, isFromPinCreation, false)).thenReturn(Single.just(password))
        whenever(interactor.updatePayload(password)).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(PinIntent.ValidatePIN(pin, isFromPinCreation))

        testState.assertValueAt(2) {
            it == PinState(
                isLoading = true,
                upgradeWalletStatus = UpgradeWalletStatus(
                    isWalletUpgradeRequired = true,
                    upgradeAppSucceeded = it.upgradeWalletStatus?.upgradeAppSucceeded ?: false
                ),
                pinStatus = PinStatus(
                    isFromPinCreation = isFromPinCreation,
                    currentPin = it.pinStatus.currentPin,
                    isPinValidated = it.pinStatus.isPinValidated
                ),
                passwordStatus = PasswordStatus(
                    passwordTriesRemaining = SECOND_PASSWORD_ATTEMPTS,
                    isPasswordValid = it.passwordStatus?.isPasswordValid ?: false,
                    passwordError = it.passwordStatus?.passwordError ?: PasswordError.NONE
                ),
                biometricStatus = BiometricStatus(
                    shouldShowFingerprint = it.biometricStatus.shouldShowFingerprint,
                    canShowFingerprint = true
                ),
            )
        }.assertValueAt(3) {
            it == PinState(
                upgradeWalletStatus = it.upgradeWalletStatus,
                pinStatus = it.pinStatus,
                passwordStatus = it.passwordStatus,
                biometricStatus = it.biometricStatus,
            )
        }
    }

    @Test
    fun `UpdatePayload succeeded and isWalletUpgradeRequired is not required then PayloadSucceeded`() {
        val password = "Test1234!"
        val pin = "1234"
        val isFromPinCreation = false

        whenever(interactor.isWalletUpgradeRequired()).thenReturn(false)
        whenever(interactor.validatePIN(pin, false, false)).thenReturn(Single.just(password))
        whenever(interactor.updatePayload(password)).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(PinIntent.ValidatePIN(pin, isFromPinCreation))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == PinState(
                isLoading = true,
                payloadStatus = PayloadStatus(
                    isPayloadCompleted = true,
                    payloadError = PayloadError.NONE
                ),
                pinStatus = PinStatus(
                    isFromPinCreation = isFromPinCreation,
                    currentPin = it.pinStatus.currentPin,
                    isPinValidated = it.pinStatus.isPinValidated
                ),
                biometricStatus = BiometricStatus(
                    shouldShowFingerprint = it.biometricStatus.shouldShowFingerprint,
                    canShowFingerprint = true
                ),
            )
        }.assertValueAt(3) {
            it == PinState(
                payloadStatus = it.payloadStatus,
                pinStatus = it.pinStatus,
                passwordStatus = it.passwordStatus,
                biometricStatus = it.biometricStatus,
            )
        }
    }

    @Test
    fun `UpdatePayload fails due to InvalidCredentialsException then update passwordError to CREDENTIALS_INVALID`() {
        val password = "Test1234!"
        val pin = "1234"
        val isFromPinCreation = false

        whenever(interactor.updatePayload(password)).thenReturn(Completable.error(InvalidCredentialsException()))
        whenever(interactor.validatePIN(pin, false, false)).thenReturn(Single.just(password))

        val testState = model.state.test()
        model.process(PinIntent.ValidatePIN(pin, isFromPinCreation))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == PinState(
                payloadStatus = PayloadStatus(
                    isPayloadCompleted = false,
                    payloadError = PayloadError.CREDENTIALS_INVALID
                ),
                isLoading = true
            )
        }.assertValueAt(3) {
            it == PinState(
                payloadStatus = it.payloadStatus,
            )
        }
    }

    @Test
    fun `UpdatePayload fails due to ServerConnectionException then update passwordError to SERVER_CONNECTION_EXCEPTION`() {
        val password = "Test1234!"
        val pin = "1234"
        val isFromPinCreation = false

        whenever(interactor.updatePayload(password)).thenReturn(Completable.error(ServerConnectionException()))
        whenever(interactor.validatePIN(pin, false, false)).thenReturn(Single.just(password))

        val testState = model.state.test()
        model.process(PinIntent.ValidatePIN(pin, isFromPinCreation))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == PinState(
                isLoading = true,
                payloadStatus = PayloadStatus(
                    isPayloadCompleted = false,
                    payloadError = PayloadError.SERVER_CONNECTION_EXCEPTION
                ),
            )
        }.assertValueAt(3) {
            it == PinState(
                payloadStatus = it.payloadStatus,
                isLoading = false
            )
        }
    }

    @Test
    fun `UpdatePayload fails due to SocketTimeoutException then update passwordError to SERVER_TIMEOUT`() {
        val password = "Test1234!"
        val pin = "1234"
        val isFromPinCreation = false
        whenever(interactor.validatePIN(pin, false, false)).thenReturn(Single.just(password))
        whenever(interactor.updatePayload(password)).thenReturn(Completable.error(SocketTimeoutException()))

        val testState = model.state.test()
        model.process(PinIntent.ValidatePIN(pin, isFromPinCreation))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == PinState(
                isLoading = true,
                payloadStatus = PayloadStatus(
                    isPayloadCompleted = false,
                    payloadError = PayloadError.SERVER_TIMEOUT
                ),
            )
        }.assertValueAt(3) {
            it == PinState(
                payloadStatus = PayloadStatus(
                    isPayloadCompleted = false,
                    payloadError = PayloadError.SERVER_TIMEOUT
                ),
            )
        }
    }

    @Test
    fun `UpdatePayload fails due to UnsupportedVersionException then update passwordError to UNSUPORTTED_VERSION_EXCEPTION`() {
        val password = "Test1234!"
        val pin = "1234"
        val isFromPinCreation = false
        whenever(interactor.updatePayload(password)).thenReturn(Completable.error(UnsupportedVersionException()))

        whenever(interactor.validatePIN(pin, false, false)).thenReturn(Single.just(password))

        val testState = model.state.test()
        model.process(PinIntent.ValidatePIN(pin, isFromPinCreation))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == PinState(
                isLoading = true,
                payloadStatus = PayloadStatus(
                    isPayloadCompleted = false,
                    payloadError = PayloadError.UNSUPPORTED_VERSION_EXCEPTION
                ),
            )
        }.assertValueAt(3) {
            it == PinState(
                payloadStatus = it.payloadStatus,
                isLoading = false
            )
        }
    }

    @Test
    fun `UpdatePayload fails due to DecryptionException then update passwordError to DECRYPTION_EXCEPTION`() {
        val password = "Test1234!"
        val pin = "1234"
        val isFromPinCreation = false
        whenever(interactor.updatePayload(password)).thenReturn(Completable.error(DecryptionException()))

        whenever(interactor.validatePIN(pin, isFromPinCreation, false)).thenReturn(Single.just(password))
        val testState = model.state.test()
        model.process(PinIntent.ValidatePIN(pin, isFromPinCreation))
        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == PinState(
                payloadStatus = PayloadStatus(
                    isPayloadCompleted = false,
                    payloadError = PayloadError.DECRYPTION_EXCEPTION
                ),
                isLoading = true
            )
        }.assertValueAt(3) {
            it == PinState(
                payloadStatus = it.payloadStatus,
                isLoading = false

            )
        }
    }

    @Test
    fun `UpdatePayload fails due to HDWalletException then update passwordError to HD_WALLET_EXCEPTION`() {
        val password = "Test1234!"
        val pin = "1234"
        val isFromPinCreation = false
        whenever(interactor.updatePayload(password)).thenReturn(Completable.error(HDWalletException()))

        whenever(interactor.validatePIN(pin, isFromPinCreation, false)).thenReturn(Single.just(password))
        val testState = model.state.test()
        model.process(PinIntent.ValidatePIN(pin, isFromPinCreation))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == PinState(
                payloadStatus = PayloadStatus(
                    isPayloadCompleted = false,
                    payloadError = PayloadError.HD_WALLET_EXCEPTION
                ),
                isLoading = true
            )
        }.assertValueAt(3) {
            it == PinState(
                payloadStatus = it.payloadStatus,
                isLoading = false
            )
        }
    }

    @Test
    fun `UpdatePayload fails due to InvalidCipherTextException then update passwordError to INVALID_CIPHER_TEXT`() {
        val password = "Test1234!"
        val pin = "1234"
        val isFromPinCreation = false
        whenever(interactor.updatePayload(password)).thenReturn(Completable.error(InvalidCipherTextException()))

        whenever(interactor.validatePIN(pin, isFromPinCreation, false)).thenReturn(Single.just(password))
        val testState = model.state.test()
        model.process(PinIntent.ValidatePIN(pin, isFromPinCreation))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == PinState(
                payloadStatus = PayloadStatus(
                    isPayloadCompleted = false,
                    payloadError = PayloadError.INVALID_CIPHER_TEXT
                ),
                isLoading = true
            )
        }.assertValueAt(3) {
            it == PinState(
                payloadStatus = it.payloadStatus,
            )
        }
    }

    @Test
    fun `UpdatePayload fails due to AccountLockedException then update passwordError to ACCOUNT_LOCKED`() {
        val password = "Test1234!"
        val pin = "1234"
        val isFromPinCreation = false
        whenever(interactor.updatePayload(password)).thenReturn(Completable.error(AccountLockedException()))

        whenever(interactor.validatePIN(pin, isFromPinCreation, false)).thenReturn(Single.just(password))
        val testState = model.state.test()
        model.process(PinIntent.ValidatePIN(pin, isFromPinCreation))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == PinState(
                payloadStatus = PayloadStatus(
                    isPayloadCompleted = false,
                    payloadError = PayloadError.ACCOUNT_LOCKED
                ),
                isLoading = true
            )
        }.assertValueAt(3) {
            it == PinState(
                payloadStatus = it.payloadStatus,
            )
        }
    }

    @Test
    fun `UpdatePayload fails due to Unknown issue then update passwordError to UNKNOWN`() {
        val password = "Test1234!"
        val pin = "1234"
        val isFromPinCreation = false

        whenever(interactor.updatePayload(password)).thenReturn(Completable.error(Throwable()))
        whenever(interactor.validatePIN(pin, isFromPinCreation, false)).thenReturn(Single.just(password))

        val testState = model.state.test()
        model.process(PinIntent.ValidatePIN(pin, isFromPinCreation))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == PinState(
                payloadStatus = PayloadStatus(
                    isPayloadCompleted = false,
                    payloadError = PayloadError.UNKNOWN
                ),
                isLoading = true
            )
        }.assertValueAt(3) {
            it == PinState(
                payloadStatus = it.payloadStatus,
                isLoading = false
            )
        }
    }

    @Test
    fun `CreatePIN is succeeded then call UpdatePayload`() {
        val tempPassword = "Test1234!"
        val pin = "1234"
        whenever(interactor.getTempPassword()).thenReturn(tempPassword)
        whenever(interactor.createPin(tempPassword, pin)).thenReturn(Completable.complete())
        whenever(interactor.isWalletUpgradeRequired()).thenReturn(false)
        whenever(interactor.updatePayload(any())).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(PinIntent.CreatePIN(pin))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == PinState(
                isLoading = true,
                payloadStatus = PayloadStatus(true),
                pinStatus = PinStatus(isFromPinCreation = true)
            )
        }.assertValueAt(3) {
            it == PinState(
                payloadStatus = PayloadStatus(true),
                pinStatus = PinStatus(isFromPinCreation = true)
            )
        }
    }

    @Test
    fun `CreatePIN is failing when password is not null`() {
        val tempPassword = "Test1234!"
        val pin = "1234"
        whenever(interactor.getTempPassword()).thenReturn(tempPassword)
        whenever(interactor.createPin(tempPassword, pin)).thenReturn(Completable.error(Throwable()))
        whenever(interactor.isWalletUpgradeRequired()).thenReturn(false)
        whenever(interactor.updatePayload(any())).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(PinIntent.CreatePIN(pin))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == PinState(
                isLoading = true,
                error = PinError.CREATE_PIN_FAILED,
            )
        }.assertValueAt(3) {
            it == PinState(
                error = it.error,
            )
        }
    }

    @Test
    fun `ValidatePassword succeeded then isPasswordValid is true`() {
        val password = "Test1234!"
        whenever(interactor.validatePassword(password)).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(PinIntent.ValidatePassword(password))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == PinState(
                isLoading = true,
                passwordStatus = PasswordStatus(
                    isPasswordValid = true,
                    passwordError = PasswordError.NONE
                )
            )
        }.assertValueAt(3) {
            it == PinState(
                passwordStatus = it.passwordStatus,
            )
        }
    }

    @Test
    fun `ValidatePassword fails due to ServerConnectionException then update passwordError to SERVER_CONNECTION_EXCEPTION`() {
        val password = "Test1234!"
        whenever(interactor.validatePassword(password)).thenReturn(Completable.error(ServerConnectionException()))

        val testState = model.state.test()
        model.process(PinIntent.ValidatePassword(password))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == PinState(
                isLoading = true,
                passwordStatus = PasswordStatus(
                    isPasswordValid = false,
                    passwordError = PasswordError.SERVER_CONNECTION_EXCEPTION
                )
            )
        }.assertValueAt(3) {
            it == PinState(
                passwordStatus = it.passwordStatus,
            )
        }
    }

    @Test
    fun `ValidatePassword fails due to SocketTimeoutException then update passwordError to SERVER_TIMEOUT`() {
        val password = "Test1234!"
        whenever(interactor.validatePassword(password)).thenReturn(Completable.error(SocketTimeoutException()))

        val testState = model.state.test()
        model.process(PinIntent.ValidatePassword(password))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == PinState(
                isLoading = true,
                passwordStatus = PasswordStatus(
                    isPasswordValid = false,
                    passwordError = PasswordError.SERVER_TIMEOUT
                )
            )
        }.assertValueAt(3) {
            it == PinState(
                passwordStatus = it.passwordStatus,
            )
        }
    }

    @Test
    fun `ValidatePassword fails due to HDWalletException then update passwordError to HD_WALLET_EXCEPTION`() {
        val password = "Test1234!"
        whenever(interactor.validatePassword(password)).thenReturn(Completable.error(HDWalletException()))

        val testState = model.state.test()
        model.process(PinIntent.ValidatePassword(password))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                isLoading = true,
            )
        }.assertValueAt(2) {
            it == PinState(
                isLoading = true,
                passwordStatus = PasswordStatus(
                    isPasswordValid = false,
                    passwordError = PasswordError.HD_WALLET_EXCEPTION
                )
            )
        }.assertValueAt(3) {
            it == PinState(
                passwordStatus = it.passwordStatus,
            )
        }
    }

    @Test
    fun `ValidatePassword fails due to AccountLockedException then update passwordError to ACCOUNT_LOCKED`() {
        val password = "Test1234!"
        whenever(interactor.validatePassword(password)).thenReturn(Completable.error(AccountLockedException()))

        val testState = model.state.test()
        model.process(PinIntent.ValidatePassword(password))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                isLoading = true,
            )
        }.assertValueAt(2) {
            it == PinState(
                isLoading = true,
                passwordStatus = PasswordStatus(
                    isPasswordValid = false,
                    passwordError = PasswordError.ACCOUNT_LOCKED
                )
            )
        }.assertValueAt(3) {
            it == PinState(
                passwordStatus = it.passwordStatus,
            )
        }
    }

    @Test
    fun `ValidatePassword fails due to unknown reason then update passwordError to UNKNOWN`() {
        val password = "Test1234!"
        whenever(interactor.validatePassword(password)).thenReturn(Completable.error(Throwable()))

        val testState = model.state.test()
        model.process(PinIntent.ValidatePassword(password))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                isLoading = true,
            )
        }.assertValueAt(2) {
            it == PinState(
                isLoading = true,
                passwordStatus = PasswordStatus(
                    isPasswordValid = false,
                    passwordError = PasswordError.UNKNOWN
                )
            )
        }.assertValueAt(3) {
            it == PinState(
                passwordStatus = it.passwordStatus,
            )
        }
    }

    @Test
    fun `UpgradeWallet succeeded then update upgradeAppSucceeded to true`() {
        val password = "Test1234!"
        val isFromPinCreation = true

        whenever(interactor.doUpgradeWallet(password)).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(PinIntent.UpgradeWallet(password, isFromPinCreation))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == PinState(
                isLoading = true,
                upgradeWalletStatus = UpgradeWalletStatus(
                    isWalletUpgradeRequired = it.upgradeWalletStatus?.isWalletUpgradeRequired ?: false,
                    upgradeAppSucceeded = true
                )
            )
        }.assertValueAt(3) {
            it == PinState(
                upgradeWalletStatus = it.upgradeWalletStatus,
            )
        }
    }

    @Test
    fun `UpgradeWallet failed then update upgradeAppSucceeded to false `() {
        val password = "Test1234!"
        val isFromPinCreation = true

        whenever(interactor.doUpgradeWallet(password)).thenReturn(Completable.error(Throwable()))

        val testState = model.state.test()
        model.process(PinIntent.UpgradeWallet(password, isFromPinCreation))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                isLoading = true
            )
        }.assertValueAt(2) {
            it == PinState(
                upgradeWalletStatus = UpgradeWalletStatus(
                    isWalletUpgradeRequired = it.upgradeWalletStatus?.isWalletUpgradeRequired ?: false,
                    upgradeAppSucceeded = false
                ),
                isLoading = true
            )
        }.assertValueAt(3) {
            it == PinState(
                upgradeWalletStatus = it.upgradeWalletStatus,
                isLoading = false
            )
        }
    }

    @Test
    fun `ValidatePIN is succeeded and isForValidatingPinForResult then update PinStatus`() {
        val isForValidatingPinForResult = true
        val pin = "1234"

        whenever(interactor.validatePIN(pin, isForValidatingPinForResult, false)).thenReturn(Single.just(pin))

        val testState = model.state.test()
        model.process(PinIntent.ValidatePIN(pin, isForValidatingPinForResult))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                isLoading = true,
                biometricStatus = BiometricStatus(
                    shouldShowFingerprint = false,
                    canShowFingerprint = it.biometricStatus.canShowFingerprint
                )
            )
        }.assertValueAt(2) {
            it == PinState(
                isLoading = true,
                biometricStatus = it.biometricStatus,
                pinStatus = PinStatus(
                    isPinValidated = true,
                    currentPin = it.pinStatus.currentPin,
                    isFromPinCreation = it.pinStatus.isFromPinCreation
                )
            )
        }.assertValueAt(3) {

            it == PinState(
                isLoading = false,
                biometricStatus = it.biometricStatus,
                pinStatus = PinStatus(
                    isPinValidated = true,
                    currentPin = it.pinStatus.currentPin,
                    isFromPinCreation = it.pinStatus.isFromPinCreation
                )
            )
        }
    }

    @Test
    fun `ValidatePIN is succeeded and isForValidatingPinForResult is false then update PinStatus and UpdatePayload`() {
        val isForValidatingPinForResult = false
        val pin = "1234"

        whenever(interactor.validatePIN(pin, isForValidatingPinForResult, false)).thenReturn(Single.just(pin))
        whenever(interactor.isWalletUpgradeRequired()).thenReturn(false)
        whenever(interactor.updatePayload(any())).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(PinIntent.ValidatePIN(pin, isForValidatingPinForResult))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                isLoading = true,
                biometricStatus = BiometricStatus(
                    shouldShowFingerprint = false,
                    canShowFingerprint = it.biometricStatus.canShowFingerprint
                )
            )
        }.assertValueAt(2) {
            it == PinState(
                isLoading = true,
                biometricStatus = it.biometricStatus,
                payloadStatus = PayloadStatus(isPayloadCompleted = true)
            )
        }.assertValueAt(3) {
            it == PinState(
                isLoading = false,
                biometricStatus = it.biometricStatus,
                payloadStatus = PayloadStatus(isPayloadCompleted = true)
            )
        }
    }

    @Test
    fun `ValidatePIN failed due to InvalidCredentialsException then return pinError INVALID_CREDENTIALS`() {
        val isForValidatingPinForResult = false
        val pin = "1234"

        whenever(interactor.validatePIN(pin, isForValidatingPinForResult, false))
            .thenReturn(Single.error(InvalidCredentialsException()))

        val testState = model.state.test()
        model.process(PinIntent.ValidatePIN(pin, isForValidatingPinForResult))
        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                isLoading = true,
                biometricStatus = BiometricStatus(
                    shouldShowFingerprint = false,
                    canShowFingerprint = it.biometricStatus.canShowFingerprint
                )
            )
        }.assertValueAt(2) {
            it == PinState(
                isLoading = true,
                biometricStatus = it.biometricStatus,
                error = PinError.INVALID_CREDENTIALS
            )
        }.assertValueAt(3) {
            it == PinState(
                isLoading = false,
                biometricStatus = it.biometricStatus,
                error = PinError.INVALID_CREDENTIALS
            )
        }
    }

    @Test
    fun `ValidatePIN failed due to Unknown then return pinError ERROR_CONNECTION`() {
        val isForValidatingPinForResult = false
        val pin = "1234"

        whenever(interactor.validatePIN(pin, isForValidatingPinForResult, false))
            .thenReturn(Single.error(Throwable()))

        val testState = model.state.test()
        model.process(PinIntent.ValidatePIN(pin, isForValidatingPinForResult))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                isLoading = true,
                biometricStatus = BiometricStatus(
                    shouldShowFingerprint = false,
                    canShowFingerprint = true
                )
            )
        }.assertValueAt(2) {
            it == PinState(
                isLoading = true,
                biometricStatus = BiometricStatus(
                    shouldShowFingerprint = false,
                    canShowFingerprint = true
                ),
                error = PinError.ERROR_CONNECTION
            )
        }.assertValueAt(3) {
            it == PinState(
                isLoading = false,
                biometricStatus = BiometricStatus(
                    shouldShowFingerprint = false,
                    canShowFingerprint = true
                ),
                error = PinError.ERROR_CONNECTION
            )
        }
    }
}
