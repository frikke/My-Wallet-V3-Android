package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.blockchain.preferences.WalletStatusPrefs
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder

class BitpayAnnouncementTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()
    private val walletStatusPrefs: WalletStatusPrefs = mock()

    private lateinit var subject: BitpayAnnouncement

    @Before
    fun setUp() {
        whenever(dismissRecorder[BitpayAnnouncement.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(BitpayAnnouncement.DISMISS_KEY)

        subject =
            BitpayAnnouncement(
                dismissRecorder = dismissRecorder,
                walletStatusPrefs = walletStatusPrefs
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
    fun `should show, when not already shown and no bitpay transactions have been made`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(walletStatusPrefs.isWalletFunded).thenReturn(true)

        whenever(walletStatusPrefs.hasMadeBitPayTransaction).thenReturn(false)

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when not already shown and a bitpay transactions has been made`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(walletStatusPrefs.isWalletFunded).thenReturn(true)

        whenever(walletStatusPrefs.hasMadeBitPayTransaction).thenReturn(true)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }
}
