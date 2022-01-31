package piuk.blockchain.android.ui.settings

import com.blockchain.api.services.WalletSettingsService
import com.blockchain.nabu.NabuUserSync
import com.blockchain.preferences.AuthPrefs
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.settings.SettingsManager
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.settings.v2.profile.ProfileInteractor
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.Email
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class ProfileInteractorTest {
    private lateinit var interactor: ProfileInteractor
    private val emailSyncUpdater = mock<EmailSyncUpdater>()
    private val authPrefs = mock<AuthPrefs>()
    private val settingsDataManager = mock<SettingsDataManager>()
    private val nabuUserSync = mock<NabuUserSync>()
    private val payloadDataManager = mock<PayloadDataManager>()

    @Before
    fun setup() {
        whenever(authPrefs.sharedKey).thenReturn("1234")
        whenever(authPrefs.walletGuid).thenReturn("4321")

        interactor = ProfileInteractor(
            emailUpdater = emailSyncUpdater,
            authPrefs = authPrefs,
            settingsDataManager = settingsDataManager,
            nabuUserSync = nabuUserSync,
            payloadDataManager = payloadDataManager
        )
    }

    @Test
    fun `When saveEmail success then updateEmailAndSync and disableNotification methods should get called`() {
        val email = mock<Email>()
        val emailAddress = "paco@gmail.com"
        val notifications: List<Int> = listOf(SettingsManager.NOTIFICATION_TYPE_EMAIL)
        val settings = Settings().copy(notificationsType = notifications)

        whenever(emailSyncUpdater.updateEmailAndSync(emailAddress)).thenReturn(Single.just(email))
        whenever(settingsDataManager.disableNotification(Settings.NOTIFICATION_TYPE_EMAIL, notifications))
            .thenReturn(Observable.just(settings))
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))

        interactor.saveEmail(emailAddress).test()

        verify(emailSyncUpdater).updateEmailAndSync(emailAddress)
        verify(settingsDataManager).disableNotification(Settings.NOTIFICATION_TYPE_EMAIL, notifications)
    }

    @Test
    fun `When resendEmail success then emailSyncUpdater method should get called`() {
        val email = mock<Email>()
        val emailAddress = "paco@gmail.com"

        whenever(emailSyncUpdater.resendEmail(emailAddress)).thenReturn(Single.just(email))

        interactor.resendEmail(emailAddress).test()

        verify(emailSyncUpdater).resendEmail(emailAddress)

        verifyNoMoreInteractions(settingsDataManager)
    }

    @Test
    fun `When loadProfile success then fetchWalletSettings method should get called`() {
        val settings = mock<WalletSettingsService.UserInfoSettings>()

        whenever(settingsDataManager.fetchWalletSettings(authPrefs.walletGuid, authPrefs.sharedKey))
            .thenReturn(Single.just(settings))

        val observer = interactor.fetchProfileSettings().test()
        observer.assertValueAt(0) {
            it == settings
        }

        verify(settingsDataManager).fetchWalletSettings(authPrefs.walletGuid, authPrefs.sharedKey)

        verifyNoMoreInteractions(settingsDataManager)
    }

    @Test
    fun `When savePhoneNumber success then updateSms, syncUser and disableNotification methods should get called`() {
        val phoneNumber = "+34655784930"
        val notifications: List<Int> = listOf(SettingsManager.NOTIFICATION_TYPE_SMS)
        val settings = Settings().copy(notificationsType = notifications)

        whenever(settingsDataManager.updateSms(phoneNumber, true)).thenReturn(Single.just(settings))
        whenever(nabuUserSync.syncUser()).thenReturn(Completable.complete())
        whenever(settingsDataManager.disableNotification(Settings.NOTIFICATION_TYPE_SMS, notifications))
            .thenReturn(Observable.just(settings))
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))

        interactor.savePhoneNumber(phoneNumber).test()

        verify(settingsDataManager).updateSms(phoneNumber, true)
        verify(nabuUserSync).syncUser()
        verify(settingsDataManager).disableNotification(Settings.NOTIFICATION_TYPE_SMS, notifications)
    }

    @Test
    fun `When resendCodeSMS success then updateSms, syncUser and disableNotification methods should get called`() {
        val phoneNumber = "+34655784930"
        val notifications: List<Int> = listOf(SettingsManager.NOTIFICATION_TYPE_SMS)
        val settings = Settings().copy(notificationsType = notifications)

        whenever(settingsDataManager.updateSms(phoneNumber, true)).thenReturn(Single.just(settings))
        whenever(nabuUserSync.syncUser()).thenReturn(Completable.complete())
        whenever(settingsDataManager.disableNotification(Settings.NOTIFICATION_TYPE_SMS, notifications))
            .thenReturn(Observable.just(settings))
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))

        interactor.resendCodeSMS(phoneNumber).test()

        verify(settingsDataManager).updateSms(phoneNumber, true)
        verify(nabuUserSync).syncUser()
        verify(settingsDataManager).disableNotification(Settings.NOTIFICATION_TYPE_SMS, notifications)
    }

    @Test
    fun `When verifyPhoneNumber success then verifySms and syncUser methods should get called`() {
        val settings = mock<Settings>()
        val code = "1234"

        whenever(settingsDataManager.verifySms(code)).thenReturn(Observable.just(settings))
        whenever(nabuUserSync.syncUser()).thenReturn(Completable.complete())

        interactor.verifyPhoneNumber(code).test()

        verify(settingsDataManager).verifySms(code)
        verify(nabuUserSync).syncUser()

        verifyNoMoreInteractions(nabuUserSync)
        verifyNoMoreInteractions(settingsDataManager)
    }
}
