package piuk.blockchain.android.ui.settings.security

import com.blockchain.preferences.SecurityPrefs
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Observable
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.data.biometrics.BiometricsController
import piuk.blockchain.android.ui.settings.v2.security.SecurityError
import piuk.blockchain.android.ui.settings.v2.security.SecurityIntent
import piuk.blockchain.android.ui.settings.v2.security.SecurityInteractor
import piuk.blockchain.android.ui.settings.v2.security.SecurityViewState
import piuk.blockchain.androidcore.data.access.PinRepository
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class SecurityInteractorTest {

    private lateinit var interactor: SecurityInteractor

    private val settingsDataManager: SettingsDataManager = mock()
    private val biometricsController: BiometricsController = mock()
    private val securityPrefs: SecurityPrefs = mock()
    private val pinRepository: PinRepository = mock()

    @Before
    fun setup() {
        interactor = SecurityInteractor(
            settingsDataManager = settingsDataManager,
            biometricsController = biometricsController,
            securityPrefs = securityPrefs,
            pinRepository = pinRepository
        )
    }

    @Test
    fun `load initial info succeeds`() {
        whenever(biometricsController.isHardwareDetected).thenReturn(true)
        whenever(biometricsController.isBiometricUnlockEnabled).thenReturn(true)
        whenever(securityPrefs.areScreenshotsEnabled).thenReturn(true)

        val settingsMock: Settings = mock {
            on { authType }.thenReturn(Settings.AUTH_TYPE_SMS)
            on { isBlockTorIps }.thenReturn(true)
        }
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settingsMock))

        val test = interactor.loadInitialInformation().test()
        test.assertValue {
            it.isBiometricsEnabled &&
                it.isBiometricsVisible &&
                it.isTorFilteringEnabled &&
                it.areScreenshotsEnabled &&
                it.isTwoFaEnabled
        }
    }

    @Test
    fun `check two fa state auth off and not sms verified should trigger view`() {
        val settingsMock: Settings = mock {
            on { authType }.thenReturn(Settings.AUTH_TYPE_OFF)
            on { isSmsVerified }.thenReturn(false)
        }
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settingsMock))

        val test = interactor.checkTwoFaState().test()
        test.assertValue {
            (it is SecurityIntent.UpdateViewState) &&
                (it.viewState == SecurityViewState.ShowVerifyPhoneNumberRequired)
        }

        verify(settingsDataManager).getSettings()
        verifyNoMoreInteractions(settingsDataManager)
    }

    @Test
    fun `check two fa state auth off and sms verified should update settings`() {
        val settingsMock: Settings = mock {
            on { authType }.thenReturn(Settings.AUTH_TYPE_OFF)
            on { isSmsVerified }.thenReturn(true)
        }
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settingsMock))
        whenever(settingsDataManager.updateTwoFactor(Settings.AUTH_TYPE_SMS)).thenReturn(Observable.just(mock()))

        val test = interactor.checkTwoFaState().test()
        test.assertValue {
            it is SecurityIntent.TwoFactorEnabled
        }

        verify(settingsDataManager).getSettings()
        verify(settingsDataManager).updateTwoFactor(Settings.AUTH_TYPE_SMS)
        verifyNoMoreInteractions(settingsDataManager)
    }

    @Test
    fun `check two fa state auth on and auth type yubikey should trigger do on web view`() {
        val settingsMock: Settings = mock {
            on { authType }.thenReturn(Settings.AUTH_TYPE_YUBI_KEY)
        }
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settingsMock))

        val test = interactor.checkTwoFaState().test()
        test.assertValue {
            (it is SecurityIntent.UpdateViewState) &&
                (it.viewState == SecurityViewState.ShowDisablingOnWebRequired)
        }

        verify(settingsDataManager).getSettings()
        verifyNoMoreInteractions(settingsDataManager)
    }

    @Test
    fun `check two fa state auth on and auth type mobile should update settings`() {
        val settingsMock: Settings = mock {
            on { authType }.thenReturn(Settings.AUTH_TYPE_SMS)
            on { isSmsVerified }.thenReturn(true)
        }
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settingsMock))
        whenever(settingsDataManager.updateTwoFactor(Settings.AUTH_TYPE_OFF)).thenReturn(Observable.just(mock()))

        val test = interactor.checkTwoFaState().test()
        test.assertValue {
            it is SecurityIntent.TwoFactorDisabled
        }

        verify(settingsDataManager).getSettings()
        verify(settingsDataManager).updateTwoFactor(Settings.AUTH_TYPE_OFF)
        verifyNoMoreInteractions(settingsDataManager)
    }

    @Test
    fun `update screenshots should update preference`() {
        doNothing().whenever(securityPrefs).setScreenshotsEnabled(any())
        val test = interactor.updateScreenshotsEnabled(true).test()
        test.assertComplete()

        verify(securityPrefs).setScreenshotsEnabled(true)
        verifyNoMoreInteractions(securityPrefs)
    }

    @Test
    fun `update Tor should update settings`() {
        val settingsMock: Settings = mock()
        whenever(settingsDataManager.updateTor(any())).thenReturn(Observable.just(settingsMock))
        val test = interactor.updateTor(true).test()
        test.assertValue {
            it == settingsMock
        }
        verify(settingsDataManager).updateTor(true)
        verifyNoMoreInteractions(settingsDataManager)
    }

    @Test
    fun `check biometrics state when none enrolled should trigger view state`() {
        whenever(biometricsController.areBiometricsEnrolled).thenReturn(false)
        val result = interactor.checkBiometricsState()
        Assert.assertTrue(
            result is SecurityIntent.UpdateViewState && result.viewState == SecurityViewState.ShowEnrollBiometrics
        )
        verify(biometricsController).areBiometricsEnrolled
        verifyNoMoreInteractions(biometricsController)
    }

    @Test
    fun `check biometrics state when some enrolled and pin missing should show error`() {
        whenever(biometricsController.areBiometricsEnrolled).thenReturn(true)
        whenever(pinRepository.pin).thenReturn("")
        val result = interactor.checkBiometricsState()

        Assert.assertTrue(
            result is SecurityIntent.UpdateErrorState && result.error == SecurityError.PIN_MISSING_EXCEPTION
        )
        verify(biometricsController).areBiometricsEnrolled
        verifyNoMoreInteractions(biometricsController)

        verify(pinRepository).pin
        verifyNoMoreInteractions(pinRepository)
    }

    @Test
    fun `check biometrics state when some enrolled, pin exists and disabled should update setting`() {
        whenever(biometricsController.areBiometricsEnrolled).thenReturn(true)
        whenever(biometricsController.isBiometricUnlockEnabled).thenReturn(false)
        whenever(pinRepository.pin).thenReturn("1234")

        val result = interactor.checkBiometricsState()

        Assert.assertTrue(
            result is SecurityIntent.UpdateViewState && result.viewState == SecurityViewState.ShowEnableBiometrics
        )
        verify(biometricsController).areBiometricsEnrolled
        verify(biometricsController).isBiometricUnlockEnabled
        verifyNoMoreInteractions(biometricsController)

        verify(pinRepository).pin
        verifyNoMoreInteractions(pinRepository)
    }

    @Test
    fun `check biometrics state when some enrolled, pin exists and already enabled should show enable`() {
        whenever(biometricsController.areBiometricsEnrolled).thenReturn(true)
        whenever(biometricsController.isBiometricUnlockEnabled).thenReturn(true)
        whenever(pinRepository.pin).thenReturn("1234")

        val result = interactor.checkBiometricsState()

        Assert.assertTrue(
            result is SecurityIntent.EnableBiometrics
        )
        verify(biometricsController).areBiometricsEnrolled
        verify(biometricsController).isBiometricUnlockEnabled
        verifyNoMoreInteractions(biometricsController)

        verify(pinRepository).pin
        verifyNoMoreInteractions(pinRepository)
    }

    @Test
    fun `disable biometrics should update preferences`() {
    }
}
