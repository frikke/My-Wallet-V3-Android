package piuk.blockchain.android.ui.settings.profile.phone

import com.blockchain.api.services.WalletSettingsService
import com.blockchain.nabu.NabuUserSync
import com.blockchain.nabu.api.getuser.data.GetUserStore
import com.blockchain.preferences.AuthPrefs
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.settings.v2.profile.phone.PhoneInteractor
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class PhoneInteractorTest {
    private lateinit var interactor: PhoneInteractor
    private val authPrefs = mock<AuthPrefs>()
    private val settingsDataManager = mock<SettingsDataManager>()
    private val nabuUserSync = mock<NabuUserSync>()
    private val getUserStore = mock<GetUserStore>()

    @Before
    fun setup() {
        whenever(authPrefs.sharedKey).thenReturn("1234")
        whenever(authPrefs.walletGuid).thenReturn("4321")

        interactor = PhoneInteractor(
            authPrefs = authPrefs,
            settingsDataManager = settingsDataManager,
            nabuUserSync = nabuUserSync,
            getUserStore = getUserStore
        )
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
        val settings = Settings()

        whenever(settingsDataManager.updateSms(phoneNumber, true)).thenReturn(Single.just(settings))
        whenever(nabuUserSync.syncUser()).thenReturn(Completable.complete())

        interactor.savePhoneNumber(phoneNumber).test()

        verify(userDataSource).invalidate()
        verify(settingsDataManager).updateSms(phoneNumber, true)
        verify(nabuUserSync).syncUser()
    }

    @Test
    fun `When resendCodeSMS success then updateSms, syncUser and disableNotification methods should get called`() {
        val phoneNumber = "+34655784930"
        val settings = Settings()

        whenever(settingsDataManager.updateSms(phoneNumber, true)).thenReturn(Single.just(settings))
        whenever(nabuUserSync.syncUser()).thenReturn(Completable.complete())

        interactor.resendCodeSMS(phoneNumber).test()

        verify(settingsDataManager).updateSms(phoneNumber, true)
        verify(nabuUserSync).syncUser()
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
