package piuk.blockchain.android.ui.settings

import com.blockchain.api.services.WalletSettingsService
import com.blockchain.nabu.NabuUserSync
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
import piuk.blockchain.androidcore.data.settings.Email
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs

class ProfileInteractorTest {
    private lateinit var interactor: ProfileInteractor
    private val emailSyncUpdater = mock<EmailSyncUpdater>()
    private val prefs = mock<PersistentPrefs>()
    private val settingsDataManager = mock<SettingsDataManager>()
    private val nabuUserSync = mock<NabuUserSync>()

    @Before
    fun setup() {
        whenever(prefs.sharedKey).thenReturn("1234")
        whenever(prefs.walletGuid).thenReturn("4321")

        interactor = ProfileInteractor(
            emailUpdater = emailSyncUpdater,
            prefs = prefs,
            settingsDataManager = settingsDataManager,
            nabuUserSync = nabuUserSync
        )
    }

    @Test
    fun `Save email and phone`() {
        val settings = mock<Settings>()
        val email = mock<Email>()

        val emailAddress = "paco@gmail.com"
        val phone = "+34655819515"

        whenever(emailSyncUpdater.updateEmailAndSync(emailAddress)).thenReturn(Single.just(email))
        whenever(settingsDataManager.updateSms(phone)).thenReturn(Observable.just(settings))
        val observer = interactor.saveProfile(emailAddress, phone).test()
        observer.assertValueAt(0) {
            it.first == email && it.second == settings
        }

        verify(emailSyncUpdater).updateEmailAndSync(emailAddress)
        verify(settingsDataManager).updateSms(phone)

        verifyNoMoreInteractions(emailSyncUpdater)
        verifyNoMoreInteractions(settingsDataManager)
    }

    @Test
    fun `Load Profile`() {
        val settings = mock<WalletSettingsService.UserInfoSettings>()

        whenever(settingsDataManager.fetchWalletSettings(prefs.walletGuid, prefs.sharedKey))
            .thenReturn(Single.just(settings))

        val observer = interactor.fetchProfileSettings().test()
        observer.assertValueAt(0) {
            it == settings
        }

        verify(settingsDataManager).fetchWalletSettings(prefs.walletGuid, prefs.sharedKey)

        verifyNoMoreInteractions(settingsDataManager)
    }

    @Test
    fun `Save and send email to verify`() {
        val email = mock<Email>()
        val emailAddress = "paco@gmail.com"

        whenever(emailSyncUpdater.updateEmailAndSync(emailAddress)).thenReturn(Single.just(email))

        interactor.saveAndSendEmail(emailAddress).test()

        verify(emailSyncUpdater).updateEmailAndSync(emailAddress)

        verifyNoMoreInteractions(emailSyncUpdater)
    }

    @Test
    fun `Save and send sms code`() {
        val phoneNumber = "+34655784930"
        val notifications: List<Int> = listOf(SettingsManager.NOTIFICATION_TYPE_SMS)
        val settings = Settings().copy(notificationsType = notifications)

        whenever(settingsDataManager.updateSms(phoneNumber)).thenReturn(Observable.just(settings))
        whenever(nabuUserSync.syncUser()).thenReturn(Completable.complete())

        interactor.saveAndSendSMS(phoneNumber).test()

        verify(settingsDataManager).updateSms(phoneNumber)

        verifyNoMoreInteractions(emailSyncUpdater)
    }

    @Test
    fun `Verify phone number`() {
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
