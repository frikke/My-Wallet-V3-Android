package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.blockchain.core.settings.SettingsDataManager
import com.blockchain.preferences.WalletStatusPrefs
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Observable
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder

class TwoFAAnnouncementTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()
    private val walletStatusPrefs: WalletStatusPrefs = mock()
    private val walletSettings: SettingsDataManager = mock()
    private val settings: Settings = mock()

    private lateinit var subject: TwoFAAnnouncement

    @Before
    fun setUp() {
        whenever(dismissRecorder[TwoFAAnnouncement.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(TwoFAAnnouncement.DISMISS_KEY)

        whenever(walletSettings.getSettings()).thenReturn(Observable.just(settings))

        subject =
            TwoFAAnnouncement(
                dismissRecorder = dismissRecorder,
                walletStatusPrefs = walletStatusPrefs,
                walletSettings = walletSettings
            )
    }

    @Test
    fun `should not show, when already shown`() {
        whenever(dismissEntry.isDismissed).thenReturn(true)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should show, when not already shown, wallet is funded and 2 fa is not enabled`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(walletStatusPrefs.isWalletFunded).thenReturn(true)

        whenever(settings.isSmsVerified).thenReturn(false)
        whenever(settings.authType).thenReturn(Settings.AUTH_TYPE_OFF)

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when not already shown, wallet is not funded`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(walletStatusPrefs.isWalletFunded).thenReturn(false)

        whenever(settings.isSmsVerified).thenReturn(false)
        whenever(settings.authType).thenReturn(Settings.AUTH_TYPE_OFF)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when 2fa is enabled - yubi key`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(walletStatusPrefs.isWalletFunded).thenReturn(false)

        whenever(settings.isSmsVerified).thenReturn(false)
        whenever(settings.authType).thenReturn(Settings.AUTH_TYPE_YUBI_KEY)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when 2fa is enabled - SMS`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(walletStatusPrefs.isWalletFunded).thenReturn(false)

        whenever(settings.isSmsVerified).thenReturn(true)
        whenever(settings.authType).thenReturn(Settings.AUTH_TYPE_SMS)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }
}
