package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.blockchain.core.kyc.domain.KycService
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder

class KycRecoveryResubmissionAnnouncementTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()
    private val kycService: KycService = mock()

    private lateinit var subject: KycRecoveryResubmissionAnnouncement

    @Before
    fun setUp() {
        whenever(dismissRecorder[VerifyEmailAnnouncement.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(VerifyEmailAnnouncement.DISMISS_KEY)

        subject = KycRecoveryResubmissionAnnouncement(
            dismissRecorder = dismissRecorder,
            kycService = kycService
        )
    }

    @Test
    fun `should show, when not already shown, user  kyc requires resubmission`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)

        whenever(kycService.shouldResubmitAfterRecovery()).thenReturn(Single.just(true))

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when not already shown, user is verified`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)

        whenever(kycService.shouldResubmitAfterRecovery()).thenReturn(Single.just(false))

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }
}
