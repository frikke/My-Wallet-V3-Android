package piuk.blockchain.android.ui.settings.profile

import com.blockchain.api.services.WalletSettingsService
import com.blockchain.preferences.AuthPrefs
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class ProfileInteractorTest {
    private lateinit var interactor: ProfileInteractor
    private val authPrefs = mock<AuthPrefs>()
    private val settingsDataManager = mock<SettingsDataManager>()

    @Before
    fun setup() {
        whenever(authPrefs.sharedKey).thenReturn("1234")
        whenever(authPrefs.walletGuid).thenReturn("4321")

        interactor = ProfileInteractor(
            authPrefs = authPrefs,
            settingsDataManager = settingsDataManager
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
}
