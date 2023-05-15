package piuk.blockchain.android.ui.launcher

import com.blockchain.android.testutils.rxInit
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.preferences.AuthPrefs
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import piuk.blockchain.android.ui.launcher.loader.LoaderActivity
import piuk.blockchain.android.ui.launcher.loader.LoaderIntents
import piuk.blockchain.android.ui.launcher.loader.LoaderInteractor
import piuk.blockchain.android.ui.launcher.loader.LoaderModel
import piuk.blockchain.android.ui.launcher.loader.LoaderState
import piuk.blockchain.android.ui.launcher.loader.LoadingStep
import piuk.blockchain.android.ui.launcher.loader.LoginMethod
import piuk.blockchain.android.ui.launcher.loader.ProgressStep
import piuk.blockchain.android.ui.launcher.loader.ToastType
import piuk.blockchain.android.util.AppUtil

@RunWith(MockitoJUnitRunner.Silent::class)
class LoaderModelTest {
    private lateinit var model: LoaderModel

    private val interactor: LoaderInteractor = mock()
    private val appUtil: AppUtil = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val prerequisites: Prerequisites = mock()
    private val authPrefs: AuthPrefs = mock() {
        on { walletGuid }.thenReturn(WALLET_GUID)
        on { pinId }.thenReturn(PIN_ID)
    }

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = LoaderModel(
            initialState = LoaderState(),
            mainScheduler = Schedulers.io(),
            environmentConfig = mock(),
            remoteLogger = mock(),
            interactor = interactor,
            appUtil = appUtil,
            payloadDataManager = payloadDataManager,
            prerequisites = prerequisites,
            authPrefs = authPrefs
        )
    }

    @Test
    fun `initSettings if accessState is logged in`() {
        // Arrange
        val isPinValidated = true
        val isAfterWalletCreation = false
        whenever(interactor.loaderIntents).thenReturn(
            Observable.just(LoaderIntents.UpdateProgressStep(ProgressStep.START))
        )

        model.process(LoaderIntents.CheckIsLoggedIn(isPinValidated, LoginMethod.PIN, null))

        // Assert
        verify(interactor).initSettings(isAfterWalletCreation, null)
    }

    @Test
    fun `start LauncherActivity if not logged in and PIN not validated`() {
        // Arrange
        val isPinValidated = false
        val testState = model.state.test()

        whenever(authPrefs.walletGuid).thenReturn("")

        model.process(LoaderIntents.CheckIsLoggedIn(isPinValidated, LoginMethod.UNDEFINED, null))

        // Assert
        testState
            .assertValues(
                LoaderState(),
                LoaderState(nextLoadingStep = LoadingStep.Launcher)
            )
    }

    @Test
    fun `DecryptAndSetupMetadata with invalid second password loads invalid password toast and shows dialog again`() {
        // Arrange
        val secondPassword = "test"
        whenever(payloadDataManager.validateSecondPassword(secondPassword)).thenReturn(false)
        val testState = model.state.test()

        model.process(LoaderIntents.DecryptAndSetupMetadata(secondPassword))

        // Assert
        verify(payloadDataManager).validateSecondPassword(secondPassword)
        testState
            .assertValues(
                LoaderState(),
                LoaderState(toastType = ToastType.INVALID_PASSWORD),
                LoaderState(toastType = null),
                LoaderState(shouldShowSecondPasswordDialog = true),
                LoaderState(shouldShowSecondPasswordDialog = false)
            )
    }

    @Test
    fun `DecryptAndSetupMetadata with valid second password calls decrypt and setup metadata successfully`() {
        // Arrange
        val secondPassword = "test"
        whenever(payloadDataManager.validateSecondPassword(secondPassword)).thenReturn(true)
        whenever(prerequisites.decryptAndSetupMetadata(secondPassword)).thenReturn(Completable.complete())
        val testState = model.state.test()

        model.process(LoaderIntents.DecryptAndSetupMetadata(secondPassword))

        // Assert
        verify(payloadDataManager).validateSecondPassword(secondPassword)
        verify(prerequisites).decryptAndSetupMetadata(secondPassword)
        verify(appUtil).loadAppWithVerifiedPin(LoaderActivity::class.java)

        testState
            .assertValues(
                LoaderState(),
                LoaderState(nextProgressStep = ProgressStep.DECRYPTING_WALLET),
                LoaderState(nextProgressStep = ProgressStep.FINISH)
            )
    }

    @Test
    fun `DecryptAndSetupMetadata with valid second password calls decrypt and setup metadata with error`() {
        // Arrange
        val secondPassword = "test"
        whenever(payloadDataManager.validateSecondPassword(secondPassword)).thenReturn(true)
        whenever(prerequisites.decryptAndSetupMetadata(secondPassword)).thenReturn(Completable.error(Throwable()))
        val testState = model.state.test()

        model.process(LoaderIntents.DecryptAndSetupMetadata(secondPassword))

        // Assert
        verify(payloadDataManager).validateSecondPassword(secondPassword)
        verify(prerequisites).decryptAndSetupMetadata(secondPassword)
        verify(appUtil, never()).loadAppWithVerifiedPin(LoaderActivity::class.java)

        testState
            .assertValues(
                LoaderState(),
                LoaderState(nextProgressStep = ProgressStep.DECRYPTING_WALLET),
                LoaderState(nextProgressStep = ProgressStep.FINISH)
            )
    }

    companion object {
        private const val WALLET_GUID = "0000-0000-0000-0000-0000"
        private const val PIN_ID = "1234"
    }
}
