package piuk.blockchain.android.ui.settings.profile.email

import com.blockchain.api.services.WalletSettingsService
import com.blockchain.preferences.AuthPrefs
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.settings.v2.profile.email.EmailInteractor
import piuk.blockchain.androidcore.data.settings.Email
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class EmailInteractorTest {
    private lateinit var interactor: EmailInteractor
    private val emailSyncUpdater = mock<EmailSyncUpdater>()
    private val authPrefs = mock<AuthPrefs>()
    private val settingsDataManager = mock<SettingsDataManager>()

    @Before
    fun setup() {
        whenever(authPrefs.sharedKey).thenReturn("1234")
        whenever(authPrefs.walletGuid).thenReturn("4321")

        interactor = EmailInteractor(
            emailUpdater = emailSyncUpdater,
            authPrefs = authPrefs,
            settingsDataManager = settingsDataManager
        )
    }

    @Test
    fun `When saveEmail success then updateEmailAndSync and disableNotification methods should get called`() {
        val email = mock<Email>()
        val emailAddress = "paco@gmail.com"

        whenever(emailSyncUpdater.updateEmailAndSync(emailAddress)).thenReturn(Single.just(email))

        interactor.saveEmail(emailAddress).test()

        verify(emailSyncUpdater).updateEmailAndSync(emailAddress)
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
}
