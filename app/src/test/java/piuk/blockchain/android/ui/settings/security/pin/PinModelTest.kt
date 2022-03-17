package piuk.blockchain.android.ui.settings.security.pin

import com.blockchain.android.testutils.rxInit
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.ProviderSpecificAnalytics
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.tasks.Task
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.api.data.UpdateType
import info.blockchain.wallet.exceptions.AccountLockedException
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import info.blockchain.wallet.exceptions.ServerConnectionException
import info.blockchain.wallet.exceptions.UnsupportedVersionException
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.net.SocketTimeoutException
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.spongycastle.crypto.InvalidCipherTextException
import piuk.blockchain.android.ui.auth.MobileNoticeDialog
import piuk.blockchain.android.ui.settings.v2.security.pin.AppUpgradeStatus
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
import piuk.blockchain.android.ui.settings.v2.security.pin.ProgressDialogStatus
import piuk.blockchain.android.ui.settings.v2.security.pin.UpgradeAppMethod
import piuk.blockchain.android.ui.settings.v2.security.pin.UpgradeWalletStatus

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
    fun `CheckAppUpgradeStatus success and type RECOMMENDED then UpgradeAppMethod FLEXIBLE`() {
        val versionName = "202202.1.0"
        val updateType = UpdateType.RECOMMENDED
        val appUpdateManager = mock<AppUpdateManager>()
        val appUpdateInfoTask = mock<Task<AppUpdateInfo>>()
        val appUpdateInfo = mock<AppUpdateInfo>()

        whenever(interactor.checkForceUpgradeStatus(versionName))
            .thenReturn(Observable.just(updateType))
        whenever(interactor.updateInfo(appUpdateManager))
            .thenReturn(Observable.just(appUpdateInfoTask))

        whenever(appUpdateInfoTask.result).thenReturn(appUpdateInfo)
        whenever(appUpdateInfoTask.result.updateAvailability())
            .thenReturn(UpdateAvailability.UPDATE_AVAILABLE)
        whenever(appUpdateInfoTask.result.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE))
            .thenReturn(true)

        val testState = model.state.test()
        model.process(PinIntent.CheckAppUpgradeStatus(versionName, appUpdateManager))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                appUpgradeStatus = AppUpgradeStatus(
                    appNeedsToUpgrade = UpgradeAppMethod.FLEXIBLE,
                    appUpdateInfo = appUpdateInfo
                )
            )
        }
    }

    @Test
    fun `CheckAppUpgradeStatus success and type FORCE then UpgradeAppMethod FORCED_NATIVELY`() {
        val versionName = "202202.1.0"
        val updateType = UpdateType.FORCE
        val appUpdateManager = mock<AppUpdateManager>()
        val appUpdateInfoTask = mock<Task<AppUpdateInfo>>()
        val appUpdateInfo = mock<AppUpdateInfo>()

        whenever(interactor.checkForceUpgradeStatus(versionName))
            .thenReturn(Observable.just(updateType))
        whenever(interactor.updateInfo(appUpdateManager))
            .thenReturn(Observable.just(appUpdateInfoTask))

        whenever(appUpdateInfoTask.result).thenReturn(appUpdateInfo)
        whenever(appUpdateInfoTask.result.updateAvailability())
            .thenReturn(UpdateAvailability.UPDATE_AVAILABLE)
        whenever(appUpdateInfoTask.result.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE))
            .thenReturn(true)

        val testState = model.state.test()
        model.process(PinIntent.CheckAppUpgradeStatus(versionName, appUpdateManager))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                appUpgradeStatus = AppUpgradeStatus(
                    appNeedsToUpgrade = UpgradeAppMethod.FORCED_NATIVELY,
                    appUpdateInfo = appUpdateInfo
                )
            )
        }
    }

    @Test
    fun `CheckAppUpgradeStatus success and type FORCE and updateType not allowed then UpgradeAppMethod FORCED_STORE`() {
        val versionName = "202202.1.0"
        val updateType = UpdateType.FORCE
        val appUpdateManager = mock<AppUpdateManager>()
        val appUpdateInfoTask = mock<Task<AppUpdateInfo>>()
        val appUpdateInfo = mock<AppUpdateInfo>()

        whenever(interactor.checkForceUpgradeStatus(versionName))
            .thenReturn(Observable.just(updateType))
        whenever(interactor.updateInfo(appUpdateManager))
            .thenReturn(Observable.just(appUpdateInfoTask))

        whenever(appUpdateInfoTask.result).thenReturn(appUpdateInfo)
        whenever(appUpdateInfoTask.result.updateAvailability())
            .thenReturn(UpdateAvailability.UPDATE_AVAILABLE)
        whenever(appUpdateInfoTask.result.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE))
            .thenReturn(false)

        val testState = model.state.test()
        model.process(PinIntent.CheckAppUpgradeStatus(versionName, appUpdateManager))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                appUpgradeStatus = AppUpgradeStatus(
                    appNeedsToUpgrade = UpgradeAppMethod.FORCED_STORE,
                    appUpdateInfo = null
                )
            )
        }
    }

    @Test
    fun `CheckAppUpgradeStatus fails, do nothing, just print exception`() {
        val versionName = "202202.1.0"
        val updateType = UpdateType.FORCE
        val appUpdateManager = mock<AppUpdateManager>()

        whenever(interactor.checkForceUpgradeStatus(versionName))
            .thenReturn(Observable.just(updateType))
        whenever(interactor.updateInfo(appUpdateManager))
            .thenReturn(Observable.error(Throwable()))

        val testState = model.state.test()
        model.process(PinIntent.CheckAppUpgradeStatus(versionName, appUpdateManager))

        testState.assertValueAt(0) {
            it == PinState()
        }
    }

    @Test
    fun `UpdatePayload succeeded and isWalletUpgradeRequired then UpgradeRequired`() {
        val password = "Test1234!"
        val isFromPinCreation = false
        val SECOND_PASSWORD_ATTEMPTS = 5

        whenever(interactor.isWalletUpgradeRequired()).thenReturn(true)
        whenever(interactor.updatePayload(password)).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(PinIntent.UpdatePayload(password, isFromPinCreation))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                progressDialog = ProgressDialogStatus(
                    hasToShow = true,
                    messageToShow = it.progressDialog?.messageToShow ?: 0
                )
            )
        }.assertValueAt(2) {
            it == PinState(
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
                progressDialog = it.progressDialog
            )
        }.assertValueAt(3) {
            it == PinState(
                upgradeWalletStatus = it.upgradeWalletStatus,
                pinStatus = it.pinStatus,
                passwordStatus = it.passwordStatus,
                biometricStatus = it.biometricStatus,
                progressDialog = ProgressDialogStatus(
                    hasToShow = false,
                    messageToShow = 0
                )
            )
        }
    }

    @Test
    fun `UpdatePayload succeeded and isWalletUpgradeRequired is not required then PayloadSucceeded`() {
        val password = "Test1234!"
        val isFromPinCreation = false
        val SECOND_PASSWORD_ATTEMPTS = 5

        whenever(interactor.isWalletUpgradeRequired()).thenReturn(false)
        whenever(interactor.updatePayload(password)).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(PinIntent.UpdatePayload(password, isFromPinCreation))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                progressDialog = ProgressDialogStatus(
                    hasToShow = true,
                    messageToShow = it.progressDialog?.messageToShow ?: 0
                )
            )
        }.assertValueAt(2) {
            it == PinState(
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
                progressDialog = it.progressDialog
            )
        }.assertValueAt(3) {
            it == PinState(
                payloadStatus = it.payloadStatus,
                pinStatus = it.pinStatus,
                passwordStatus = it.passwordStatus,
                biometricStatus = it.biometricStatus,
                progressDialog = ProgressDialogStatus(
                    hasToShow = false,
                    messageToShow = 0
                )
            )
        }
    }

    @Test
    fun `UpdatePayload fails due to InvalidCredentialsException then update passwordError to CREDENTIALS_INVALID`() {
        val password = "Test1234!"
        val isFromPinCreation = false

        whenever(interactor.updatePayload(password)).thenReturn(Completable.error(InvalidCredentialsException()))

        val testState = model.state.test()
        model.process(PinIntent.UpdatePayload(password, isFromPinCreation))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                progressDialog = ProgressDialogStatus(
                    hasToShow = true,
                    messageToShow = it.progressDialog?.messageToShow ?: 0
                )
            )
        }.assertValueAt(2) {
            it == PinState(
                payloadStatus = PayloadStatus(
                    isPayloadCompleted = false,
                    payloadError = PayloadError.CREDENTIALS_INVALID
                ),
                progressDialog = it.progressDialog
            )
        }.assertValueAt(3) {
            it == PinState(
                payloadStatus = it.payloadStatus,
                progressDialog = ProgressDialogStatus(
                    hasToShow = false,
                    messageToShow = 0
                )
            )
        }
    }

    @Test
    fun `UpdatePayload fails due to ServerConnectionException then update passwordError to SERVER_CONNECTION_EXCEPTION`() {
        val password = "Test1234!"
        val isFromPinCreation = false

        whenever(interactor.updatePayload(password)).thenReturn(Completable.error(ServerConnectionException()))

        val testState = model.state.test()
        model.process(PinIntent.UpdatePayload(password, isFromPinCreation))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                progressDialog = ProgressDialogStatus(
                    hasToShow = true,
                    messageToShow = it.progressDialog?.messageToShow ?: 0
                )
            )
        }.assertValueAt(2) {
            it == PinState(
                payloadStatus = PayloadStatus(
                    isPayloadCompleted = false,
                    payloadError = PayloadError.SERVER_CONNECTION_EXCEPTION
                ),
                progressDialog = it.progressDialog
            )
        }.assertValueAt(3) {
            it == PinState(
                payloadStatus = it.payloadStatus,
                progressDialog = ProgressDialogStatus(
                    hasToShow = false,
                    messageToShow = 0
                )
            )
        }
    }

    @Test
    fun `UpdatePayload fails due to SocketTimeoutException then update passwordError to SERVER_TIMEOUT`() {
        val password = "Test1234!"
        val isFromPinCreation = false

        whenever(interactor.updatePayload(password)).thenReturn(Completable.error(SocketTimeoutException()))

        val testState = model.state.test()
        model.process(PinIntent.UpdatePayload(password, isFromPinCreation))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                progressDialog = ProgressDialogStatus(
                    hasToShow = true,
                    messageToShow = it.progressDialog?.messageToShow ?: 0
                )
            )
        }.assertValueAt(2) {
            it == PinState(
                payloadStatus = PayloadStatus(
                    isPayloadCompleted = false,
                    payloadError = PayloadError.SERVER_TIMEOUT
                ),
                progressDialog = it.progressDialog
            )
        }.assertValueAt(3) {
            it == PinState(
                payloadStatus = it.payloadStatus,
                progressDialog = ProgressDialogStatus(
                    hasToShow = false,
                    messageToShow = 0
                )
            )
        }
    }

    @Test
    fun `UpdatePayload fails due to UnsupportedVersionException then update passwordError to UNSUPORTTED_VERSION_EXCEPTION`() {
        val password = "Test1234!"
        val isFromPinCreation = false

        whenever(interactor.updatePayload(password)).thenReturn(Completable.error(UnsupportedVersionException()))

        val testState = model.state.test()
        model.process(PinIntent.UpdatePayload(password, isFromPinCreation))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                progressDialog = ProgressDialogStatus(
                    hasToShow = true,
                    messageToShow = it.progressDialog?.messageToShow ?: 0
                )
            )
        }.assertValueAt(2) {
            it == PinState(
                payloadStatus = PayloadStatus(
                    isPayloadCompleted = false,
                    payloadError = PayloadError.UNSUPPORTED_VERSION_EXCEPTION
                ),
                progressDialog = it.progressDialog
            )
        }.assertValueAt(3) {
            it == PinState(
                payloadStatus = it.payloadStatus,
                progressDialog = ProgressDialogStatus(
                    hasToShow = false,
                    messageToShow = 0
                )
            )
        }
    }

    @Test
    fun `UpdatePayload fails due to DecryptionException then update passwordError to DECRYPTION_EXCEPTION`() {
        val password = "Test1234!"
        val isFromPinCreation = false

        whenever(interactor.updatePayload(password)).thenReturn(Completable.error(DecryptionException()))

        val testState = model.state.test()
        model.process(PinIntent.UpdatePayload(password, isFromPinCreation))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                progressDialog = ProgressDialogStatus(
                    hasToShow = true,
                    messageToShow = it.progressDialog?.messageToShow ?: 0
                )
            )
        }.assertValueAt(2) {
            it == PinState(
                payloadStatus = PayloadStatus(
                    isPayloadCompleted = false,
                    payloadError = PayloadError.DECRYPTION_EXCEPTION
                ),
                progressDialog = it.progressDialog
            )
        }.assertValueAt(3) {
            it == PinState(
                payloadStatus = it.payloadStatus,
                progressDialog = ProgressDialogStatus(
                    hasToShow = false,
                    messageToShow = 0
                )
            )
        }
    }

    @Test
    fun `UpdatePayload fails due to HDWalletException then update passwordError to HD_WALLET_EXCEPTION`() {
        val password = "Test1234!"
        val isFromPinCreation = false

        whenever(interactor.updatePayload(password)).thenReturn(Completable.error(HDWalletException()))

        val testState = model.state.test()
        model.process(PinIntent.UpdatePayload(password, isFromPinCreation))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                progressDialog = ProgressDialogStatus(
                    hasToShow = true,
                    messageToShow = it.progressDialog?.messageToShow ?: 0
                )
            )
        }.assertValueAt(2) {
            it == PinState(
                payloadStatus = PayloadStatus(
                    isPayloadCompleted = false,
                    payloadError = PayloadError.HD_WALLET_EXCEPTION
                ),
                progressDialog = it.progressDialog
            )
        }.assertValueAt(3) {
            it == PinState(
                payloadStatus = it.payloadStatus,
                progressDialog = ProgressDialogStatus(
                    hasToShow = false,
                    messageToShow = 0
                )
            )
        }
    }

    @Test
    fun `UpdatePayload fails due to InvalidCipherTextException then update passwordError to INVALID_CIPHER_TEXT`() {
        val password = "Test1234!"
        val isFromPinCreation = false

        whenever(interactor.updatePayload(password)).thenReturn(Completable.error(InvalidCipherTextException()))

        val testState = model.state.test()
        model.process(PinIntent.UpdatePayload(password, isFromPinCreation))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                progressDialog = ProgressDialogStatus(
                    hasToShow = true,
                    messageToShow = it.progressDialog?.messageToShow ?: 0
                )
            )
        }.assertValueAt(2) {
            it == PinState(
                payloadStatus = PayloadStatus(
                    isPayloadCompleted = false,
                    payloadError = PayloadError.INVALID_CIPHER_TEXT
                ),
                progressDialog = it.progressDialog
            )
        }.assertValueAt(3) {
            it == PinState(
                payloadStatus = it.payloadStatus,
                progressDialog = ProgressDialogStatus(
                    hasToShow = false,
                    messageToShow = 0
                )
            )
        }
    }

    @Test
    fun `UpdatePayload fails due to AccountLockedException then update passwordError to ACCOUNT_LOCKED`() {
        val password = "Test1234!"
        val isFromPinCreation = false

        whenever(interactor.updatePayload(password)).thenReturn(Completable.error(AccountLockedException()))

        val testState = model.state.test()
        model.process(PinIntent.UpdatePayload(password, isFromPinCreation))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                progressDialog = ProgressDialogStatus(
                    hasToShow = true,
                    messageToShow = it.progressDialog?.messageToShow ?: 0
                )
            )
        }.assertValueAt(2) {
            it == PinState(
                payloadStatus = PayloadStatus(
                    isPayloadCompleted = false,
                    payloadError = PayloadError.ACCOUNT_LOCKED
                ),
                progressDialog = it.progressDialog
            )
        }.assertValueAt(3) {
            it == PinState(
                payloadStatus = it.payloadStatus,
                progressDialog = ProgressDialogStatus(
                    hasToShow = false,
                    messageToShow = 0
                )
            )
        }
    }

    @Test
    fun `UpdatePayload fails due to Unknown issue then update passwordError to UNKNOWN`() {
        val password = "Test1234!"
        val isFromPinCreation = false

        whenever(interactor.updatePayload(password)).thenReturn(Completable.error(Throwable()))

        val testState = model.state.test()
        model.process(PinIntent.UpdatePayload(password, isFromPinCreation))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                progressDialog = ProgressDialogStatus(
                    hasToShow = true,
                    messageToShow = it.progressDialog?.messageToShow ?: 0
                )
            )
        }.assertValueAt(2) {
            it == PinState(
                payloadStatus = PayloadStatus(
                    isPayloadCompleted = false,
                    payloadError = PayloadError.UNKNOWN
                ),
                progressDialog = it.progressDialog
            )
        }.assertValueAt(3) {
            it == PinState(
                payloadStatus = it.payloadStatus,
                progressDialog = ProgressDialogStatus(
                    hasToShow = false,
                    messageToShow = 0
                )
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
                isLoading = it.isLoading,
                progressDialog = ProgressDialogStatus(
                    hasToShow = true,
                    messageToShow = it.progressDialog?.messageToShow ?: 0
                )
            )
        }.assertValueAt(3) {
            it == PinState(
                isLoading = false,
                progressDialog = it.progressDialog
            )
        }.assertValueAt(4) {
            it == PinState(
                progressDialog = ProgressDialogStatus(
                    hasToShow = false,
                    messageToShow = 0
                )
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
                isLoading = it.isLoading,
                progressDialog = ProgressDialogStatus(
                    hasToShow = true,
                    messageToShow = it.progressDialog?.messageToShow ?: 0
                )
            )
        }.assertValueAt(3) {
            it == PinState(
                isLoading = false,
                error = PinError.CREATE_PIN_FAILED,
                progressDialog = it.progressDialog
            )
        }.assertValueAt(4) {
            it == PinState(
                error = it.error,
                progressDialog = ProgressDialogStatus(
                    hasToShow = false,
                    messageToShow = 0
                )
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
                progressDialog = ProgressDialogStatus(
                    hasToShow = true,
                    messageToShow = it.progressDialog?.messageToShow ?: 0
                )
            )
        }.assertValueAt(2) {
            it == PinState(
                progressDialog = it.progressDialog,
                passwordStatus = PasswordStatus(
                    isPasswordValid = true,
                    passwordError = PasswordError.NONE
                )
            )
        }.assertValueAt(3) {
            it == PinState(
                passwordStatus = it.passwordStatus,
                progressDialog = ProgressDialogStatus(
                    hasToShow = false,
                    messageToShow = 0
                )
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
                progressDialog = ProgressDialogStatus(
                    hasToShow = true,
                    messageToShow = it.progressDialog?.messageToShow ?: 0
                )
            )
        }.assertValueAt(2) {
            it == PinState(
                progressDialog = it.progressDialog,
                passwordStatus = PasswordStatus(
                    isPasswordValid = false,
                    passwordError = PasswordError.SERVER_CONNECTION_EXCEPTION
                )
            )
        }.assertValueAt(3) {
            it == PinState(
                passwordStatus = it.passwordStatus,
                progressDialog = ProgressDialogStatus(
                    hasToShow = false,
                    messageToShow = 0
                )
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
                progressDialog = ProgressDialogStatus(
                    hasToShow = true,
                    messageToShow = it.progressDialog?.messageToShow ?: 0
                )
            )
        }.assertValueAt(2) {
            it == PinState(
                progressDialog = it.progressDialog,
                passwordStatus = PasswordStatus(
                    isPasswordValid = false,
                    passwordError = PasswordError.SERVER_TIMEOUT
                )
            )
        }.assertValueAt(3) {
            it == PinState(
                passwordStatus = it.passwordStatus,
                progressDialog = ProgressDialogStatus(
                    hasToShow = false,
                    messageToShow = 0
                )
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
                progressDialog = ProgressDialogStatus(
                    hasToShow = true,
                    messageToShow = it.progressDialog?.messageToShow ?: 0
                )
            )
        }.assertValueAt(2) {
            it == PinState(
                progressDialog = it.progressDialog,
                passwordStatus = PasswordStatus(
                    isPasswordValid = false,
                    passwordError = PasswordError.HD_WALLET_EXCEPTION
                )
            )
        }.assertValueAt(3) {
            it == PinState(
                passwordStatus = it.passwordStatus,
                progressDialog = ProgressDialogStatus(
                    hasToShow = false,
                    messageToShow = 0
                )
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
                progressDialog = ProgressDialogStatus(
                    hasToShow = true,
                    messageToShow = it.progressDialog?.messageToShow ?: 0
                )
            )
        }.assertValueAt(2) {
            it == PinState(
                progressDialog = it.progressDialog,
                passwordStatus = PasswordStatus(
                    isPasswordValid = false,
                    passwordError = PasswordError.ACCOUNT_LOCKED
                )
            )
        }.assertValueAt(3) {
            it == PinState(
                passwordStatus = it.passwordStatus,
                progressDialog = ProgressDialogStatus(
                    hasToShow = false,
                    messageToShow = 0
                )
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
                progressDialog = ProgressDialogStatus(
                    hasToShow = true,
                    messageToShow = it.progressDialog?.messageToShow ?: 0
                )
            )
        }.assertValueAt(2) {
            it == PinState(
                progressDialog = it.progressDialog,
                passwordStatus = PasswordStatus(
                    isPasswordValid = false,
                    passwordError = PasswordError.UNKNOWN
                )
            )
        }.assertValueAt(3) {
            it == PinState(
                passwordStatus = it.passwordStatus,
                progressDialog = ProgressDialogStatus(
                    hasToShow = false,
                    messageToShow = 0
                )
            )
        }
    }

    @Test
    fun `UpgradeWallet succeeded then update upgradeAppSucceeded to true`() {
        val password = "Test1234!"
        val isFromPinCreation = true

        whenever(interactor.doUpgradeWallet(password, isFromPinCreation)).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(PinIntent.UpgradeWallet(password, isFromPinCreation))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                progressDialog = ProgressDialogStatus(
                    hasToShow = true,
                    messageToShow = it.progressDialog?.messageToShow ?: 0
                )
            )
        }.assertValueAt(2) {
            it == PinState(
                progressDialog = it.progressDialog,
                upgradeWalletStatus = UpgradeWalletStatus(
                    isWalletUpgradeRequired = it.upgradeWalletStatus?.isWalletUpgradeRequired ?: false,
                    upgradeAppSucceeded = true
                )
            )
        }.assertValueAt(3) {
            it == PinState(
                upgradeWalletStatus = it.upgradeWalletStatus,
                progressDialog = ProgressDialogStatus(
                    hasToShow = false,
                    messageToShow = 0
                )
            )
        }
    }

    @Test
    fun `UpgradeWallet failed then update upgradeAppSucceeded to false `() {
        val password = "Test1234!"
        val isFromPinCreation = true

        whenever(interactor.doUpgradeWallet(password, isFromPinCreation)).thenReturn(Completable.error(Throwable()))

        val testState = model.state.test()
        model.process(PinIntent.UpgradeWallet(password, isFromPinCreation))

        testState.assertValueAt(0) {
            it == PinState()
        }.assertValueAt(1) {
            it == PinState(
                progressDialog = ProgressDialogStatus(
                    hasToShow = true,
                    messageToShow = it.progressDialog?.messageToShow ?: 0
                )
            )
        }.assertValueAt(2) {
            it == PinState(
                progressDialog = it.progressDialog,
                upgradeWalletStatus = UpgradeWalletStatus(
                    isWalletUpgradeRequired = it.upgradeWalletStatus?.isWalletUpgradeRequired ?: false,
                    upgradeAppSucceeded = false
                )
            )
        }.assertValueAt(3) {
            it == PinState(
                upgradeWalletStatus = it.upgradeWalletStatus,
                progressDialog = ProgressDialogStatus(
                    hasToShow = false,
                    messageToShow = 0
                )
            )
        }
    }

    @Test
    fun `ValidatePIN is succeeded and isForValidatingPinForResult then update PinStatus`() {
        val isForValidatingPinForResult = true
        val pin = "1234"

        whenever(interactor.validatePIN(pin, isForValidatingPinForResult)).thenReturn(Single.just(pin))

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
                progressDialog = ProgressDialogStatus(
                    hasToShow = true,
                    messageToShow = it.progressDialog?.messageToShow ?: 0
                )
            )
        }.assertValueAt(3) {
            it == PinState(
                isLoading = false,
                biometricStatus = it.biometricStatus,
                progressDialog = it.progressDialog,
                pinStatus = PinStatus(
                    isPinValidated = true,
                    currentPin = it.pinStatus.currentPin,
                    isFromPinCreation = it.pinStatus.isFromPinCreation
                )
            )
        }.assertValueAt(4) {
            it == PinState(
                isLoading = false,
                biometricStatus = it.biometricStatus,
                pinStatus = it.pinStatus,
                progressDialog = ProgressDialogStatus(
                    hasToShow = false,
                    messageToShow = 0
                )
            )
        }
    }

    @Test
    fun `ValidatePIN is succeeded and isForValidatingPinForResult is false then update PinStatus and UpdatePayload`() {
        val isForValidatingPinForResult = false
        val pin = "1234"

        whenever(interactor.validatePIN(pin, isForValidatingPinForResult)).thenReturn(Single.just(pin))
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
                progressDialog = ProgressDialogStatus(
                    hasToShow = true,
                    messageToShow = it.progressDialog?.messageToShow ?: 0
                )
            )
        }.assertValueAt(3) {
            it == PinState(
                isLoading = true,
                biometricStatus = it.biometricStatus,
                progressDialog = ProgressDialogStatus(
                    hasToShow = false,
                    messageToShow = 0
                )
            )
        }
    }

    @Test
    fun `ValidatePIN failed due to InvalidCredentialsException then return pinError INVALID_CREDENTIALS`() {
        val isForValidatingPinForResult = false
        val pin = "1234"

        whenever(interactor.validatePIN(pin, isForValidatingPinForResult))
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
                progressDialog = ProgressDialogStatus(
                    hasToShow = true,
                    messageToShow = it.progressDialog?.messageToShow ?: 0
                )
            )
        }.assertValueAt(3) {
            it == PinState(
                isLoading = false,
                biometricStatus = it.biometricStatus,
                progressDialog = it.progressDialog,
                error = PinError.INVALID_CREDENTIALS
            )
        }.assertValueAt(4) {
            it == PinState(
                isLoading = false,
                error = it.error,
                biometricStatus = it.biometricStatus,
                progressDialog = ProgressDialogStatus(
                    hasToShow = false,
                    messageToShow = 0
                )
            )
        }
    }

    @Test
    fun `ValidatePIN failed due to Unknown then return pinError ERROR_CONNECTION`() {
        val isForValidatingPinForResult = false
        val pin = "1234"

        whenever(interactor.validatePIN(pin, isForValidatingPinForResult))
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
                    canShowFingerprint = it.biometricStatus.canShowFingerprint
                )
            )
        }.assertValueAt(2) {
            it == PinState(
                isLoading = true,
                biometricStatus = it.biometricStatus,
                progressDialog = ProgressDialogStatus(
                    hasToShow = true,
                    messageToShow = it.progressDialog?.messageToShow ?: 0
                )
            )
        }.assertValueAt(3) {
            it == PinState(
                isLoading = false,
                biometricStatus = it.biometricStatus,
                progressDialog = it.progressDialog,
                error = PinError.ERROR_CONNECTION
            )
        }.assertValueAt(4) {
            it == PinState(
                error = it.error,
                isLoading = false,
                biometricStatus = it.biometricStatus,
                progressDialog = ProgressDialogStatus(
                    hasToShow = false,
                    messageToShow = 0
                )
            )
        }
    }
}
