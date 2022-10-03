package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.blockchain.core.kyc.domain.KycService
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder

class KycMoreInfoAnnouncementTest {
    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()

    private val kycService: KycService = mock()

    private lateinit var subject: KycMoreInfoAnnouncement

    @Before
    fun setUp() {
        whenever(dismissRecorder[KycMoreInfoAnnouncement.DISMISS_KEY])
            .thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey)
            .thenReturn(KycMoreInfoAnnouncement.DISMISS_KEY)

        subject = KycMoreInfoAnnouncement(
            kycService = kycService,
            dismissRecorder = dismissRecorder
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
}
