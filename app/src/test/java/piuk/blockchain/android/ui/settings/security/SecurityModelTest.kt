package piuk.blockchain.android.ui.settings.security

import com.blockchain.android.testutils.rxInit
import com.blockchain.enviroment.EnvironmentConfig
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.settings.v2.security.SecurityError
import piuk.blockchain.android.ui.settings.v2.security.SecurityInfo
import piuk.blockchain.android.ui.settings.v2.security.SecurityIntent
import piuk.blockchain.android.ui.settings.v2.security.SecurityInteractor
import piuk.blockchain.android.ui.settings.v2.security.SecurityModel
import piuk.blockchain.android.ui.settings.v2.security.SecurityState
import piuk.blockchain.android.ui.settings.v2.security.SecurityViewState

class SecurityModelTest {

    private lateinit var model: SecurityModel
    private val defaultState = spy(SecurityState())

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    private val interactor: SecurityInteractor = mock()

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = SecurityModel(
            initialState = defaultState,
            mainScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            crashLogger = mock(),
            interactor = interactor
        )
    }

    @Test
    fun `load initial information succeeds`() {
        val initialInfo: SecurityInfo = mock()

        whenever(interactor.loadInitialInformation()).thenReturn(Single.just(initialInfo))
        val testState = model.state.test()
        model.process(SecurityIntent.LoadInitialInformation)

        testState.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.securityInfo == initialInfo
        }
    }

    @Test
    fun `load initial information fails`() {
        val exception: Exception = mock()

        whenever(interactor.loadInitialInformation()).thenReturn(Single.error(exception))
        val testState = model.state.test()
        model.process(SecurityIntent.LoadInitialInformation)

        testState.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.errorState == SecurityError.LOAD_INITIAL_INFO_FAIL
        }
    }

    @Test
    fun `toggle biometrics when already enabled should disable`() {
        val initialInfo: SecurityInfo = mock {
            on { isBiometricsEnabled }.thenReturn(true)
        }
        whenever(defaultState.securityInfo).thenReturn(initialInfo)

        val testState = model.state.test()
        model.process(SecurityIntent.ToggleBiometrics)

        testState.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.securityViewState == SecurityViewState.ConfirmBiometricsDisabling
        }
    }

    @Test
    fun `toggle biometrics when disabled should check state`() {
        val initialInfo: SecurityInfo = mock {
            on { isBiometricsEnabled }.thenReturn(false)
        }
        whenever(defaultState.securityInfo).thenReturn(initialInfo)
        val securityIntent = SecurityIntent.UpdateViewState(SecurityViewState.ShowEnrollBiometrics)
        whenever(interactor.checkBiometricsState()).thenReturn(securityIntent)

        val testState = model.state.test()
        model.process(SecurityIntent.ToggleBiometrics)

        testState.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.securityViewState == SecurityViewState.ShowEnrollBiometrics
        }

        verify(interactor).checkBiometricsState()
    }

    @Test
    fun `disabling biometrics succeeds`() {
        val initialInfo = SecurityInfo(
            isBiometricsVisible = true,
            isBiometricsEnabled = true,
            isTorFilteringEnabled = true,
            areScreenshotsEnabled = true,
            isTwoFaEnabled = true
        )

        whenever(defaultState.securityInfo).thenReturn(initialInfo)
        whenever(interactor.disableBiometricLogin()).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(SecurityIntent.DisableBiometrics)

        testState.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.securityInfo?.isBiometricsEnabled == false
        }
    }

    @Test
    fun `disabling biometrics fails`() {
        whenever(interactor.disableBiometricLogin()).thenReturn(Completable.error(Exception()))

        val testState = model.state.test()
        model.process(SecurityIntent.DisableBiometrics)

        testState.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.errorState == SecurityError.BIOMETRICS_DISABLING_FAIL
        }
    }

    @Test
    fun `toggle two fa succeeds`() {
        // get an intent returned by this interactor function and test the state updates
        whenever(interactor.checkTwoFaState()).thenReturn(
            Single.just(
                SecurityIntent.UpdateViewState(
                    SecurityViewState.ShowVerifyPhoneNumberRequired
                )
            )
        )

        val testState = model.state.test()
        model.process(SecurityIntent.ToggleTwoFa)

        testState.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.securityViewState is SecurityViewState.ShowVerifyPhoneNumberRequired
        }
    }

    @Test
    fun `toggle two fa fails`() {
        // get an intent returned by this interactor function and test the state updates
        whenever(interactor.checkTwoFaState()).thenReturn(Single.error(Exception()))

        val testState = model.state.test()
        model.process(SecurityIntent.ToggleTwoFa)

        testState.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.errorState == SecurityError.TWO_FA_TOGGLE_FAIL
        }
    }

    @Test
    fun `toggle screenshots on succeeds`() {
        val initialInfo = SecurityInfo(
            isBiometricsVisible = true,
            isBiometricsEnabled = true,
            isTorFilteringEnabled = true,
            areScreenshotsEnabled = false,
            isTwoFaEnabled = true
        )

        whenever(defaultState.securityInfo).thenReturn(initialInfo)
        whenever(interactor.updateScreenshotsEnabled(true)).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(SecurityIntent.ToggleScreenshots)

        testState.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.securityInfo?.areScreenshotsEnabled == true
        }
    }

    @Test
    fun `toggle screenshots off succeeds`() {
        val initialInfo = SecurityInfo(
            isBiometricsVisible = true,
            isBiometricsEnabled = true,
            isTorFilteringEnabled = true,
            areScreenshotsEnabled = true,
            isTwoFaEnabled = true
        )

        whenever(defaultState.securityInfo).thenReturn(initialInfo)
        whenever(interactor.updateScreenshotsEnabled(false)).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(SecurityIntent.ToggleScreenshots)

        testState.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.securityInfo?.areScreenshotsEnabled == false
        }
    }

    @Test
    fun `toggle screenshots fails`() {
        val initialInfo: SecurityInfo = mock {
            on { areScreenshotsEnabled }.thenReturn(true)
        }
        whenever(defaultState.securityInfo).thenReturn(initialInfo)
        whenever(interactor.updateScreenshotsEnabled(false)).thenReturn(Completable.error(Exception()))

        val testState = model.state.test()
        model.process(SecurityIntent.ToggleScreenshots)

        testState.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.errorState == SecurityError.SCREENSHOT_UPDATE_FAIL
        }
    }

    @Test
    fun `toggle tor succeeds`() {
        val initialInfo = SecurityInfo(
            isBiometricsVisible = true,
            isBiometricsEnabled = true,
            isTorFilteringEnabled = true,
            areScreenshotsEnabled = true,
            isTwoFaEnabled = true
        )

        whenever(defaultState.securityInfo).thenReturn(initialInfo)
        whenever(interactor.updateTor(false)).thenReturn(Single.just(mock()))

        val testState = model.state.test()
        model.process(SecurityIntent.ToggleTor)

        testState.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.securityInfo?.isTorFilteringEnabled == false
        }
    }

    @Test
    fun `toggle tor fails`() {
        val initialInfo = SecurityInfo(
            isBiometricsVisible = true,
            isBiometricsEnabled = true,
            isTorFilteringEnabled = true,
            areScreenshotsEnabled = true,
            isTwoFaEnabled = true
        )

        whenever(defaultState.securityInfo).thenReturn(initialInfo)
        whenever(interactor.updateTor(false)).thenReturn(Single.error(Exception()))

        val testState = model.state.test()
        model.process(SecurityIntent.ToggleTor)

        testState.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.errorState == SecurityError.TOR_FILTER_UPDATE_FAIL
        }
    }
}
