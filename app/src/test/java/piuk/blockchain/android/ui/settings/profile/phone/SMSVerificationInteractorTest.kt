package piuk.blockchain.android.ui.settings.profile.phone

import com.blockchain.nabu.NabuUserSync
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.settings.SettingsManager
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.settings.v2.sheets.sms.SMSVerificationInteractor
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class SMSVerificationInteractorTest {
    private lateinit var interactor: SMSVerificationInteractor
    private val settingsDataManager = mock<SettingsDataManager>()
    private val nabuUserSync = mock<NabuUserSync>()
    private val payloadDataManager = mock<PayloadDataManager>()

    @Before
    fun setup() {
        interactor = SMSVerificationInteractor(
            settingsDataManager = settingsDataManager,
            nabuUserSync = nabuUserSync,
            payloadDataManager = payloadDataManager
        )
    }

    @Test
    fun `When resendCodeSMS success then updateSms, syncUser and disableNotification methods should get called`() {
        val phoneNumber = "+34655784930"
        val notifications: List<Int> = listOf(SettingsManager.NOTIFICATION_TYPE_SMS)
        val settings = Settings().copy(notificationsType = notifications)

        whenever(settingsDataManager.updateSms(phoneNumber)).thenReturn(Observable.just(settings))
        whenever(nabuUserSync.syncUser()).thenReturn(Completable.complete())
        whenever(settingsDataManager.disableNotification(Settings.NOTIFICATION_TYPE_SMS, notifications))
            .thenReturn(Observable.just(settings))
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))

        interactor.resendCodeSMS(phoneNumber).test()

        verify(settingsDataManager).updateSms(phoneNumber)
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
