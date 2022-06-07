package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.blockchain.featureflag.FeatureFlag
import com.blockchain.preferences.NftAnnouncementPrefs
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder

class NftAnnouncementTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()
    private val nftAnnouncementPrefs: NftAnnouncementPrefs = mock()

    private lateinit var subject: NftAnnouncement

    @Before
    fun setUp() {
        whenever(dismissRecorder[NftAnnouncement.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(NftAnnouncement.DISMISS_KEY)

        subject = NftAnnouncement(
            dismissRecorder = dismissRecorder,
            nftAnnouncementPrefs = nftAnnouncementPrefs
        )
    }

    @Test
    fun `should show, when not not successful and not dismissed`() {
        whenever(nftAnnouncementPrefs.isJoinNftWaitlistSuccessful).thenReturn(false)
        whenever(nftAnnouncementPrefs.isNftAnnouncementDismissed).thenReturn(false)

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when successful and not dismissed`() {
        whenever(nftAnnouncementPrefs.isJoinNftWaitlistSuccessful).thenReturn(true)
        whenever(nftAnnouncementPrefs.isNftAnnouncementDismissed).thenReturn(false)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when not successful and dismissed`() {
        whenever(nftAnnouncementPrefs.isJoinNftWaitlistSuccessful).thenReturn(false)
        whenever(nftAnnouncementPrefs.isNftAnnouncementDismissed).thenReturn(true)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when successful and dismissed`() {
        whenever(nftAnnouncementPrefs.isJoinNftWaitlistSuccessful).thenReturn(true)
        whenever(nftAnnouncementPrefs.isNftAnnouncementDismissed).thenReturn(true)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }
}
