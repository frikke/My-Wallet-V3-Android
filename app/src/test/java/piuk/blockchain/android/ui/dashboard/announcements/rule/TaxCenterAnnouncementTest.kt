package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.blockchain.nabu.UserIdentity
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Maybe
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder

class TaxCenterAnnouncementTest {
    private val dismissRecorder: DismissRecorder = mock()
    private val userIdentity: UserIdentity = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()
    private lateinit var subject: TaxCenterAnnouncement

    @Before
    fun setUp() {
        whenever(dismissRecorder[TaxCenterAnnouncement.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(TaxCenterAnnouncement.DISMISS_KEY)
        subject =
            TaxCenterAnnouncement(
                dismissRecorder = dismissRecorder,
                userIdentity = userIdentity
            )
    }

    @Test
    fun `when announcement was already shown then it shouldnt show again`() {
        whenever(dismissEntry.isDismissed).thenReturn(true)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `when user is not in US then announcement shouldnt shown`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(userIdentity.getUserCountry()).thenReturn(Maybe.just("GB"))

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }
    @Test
    fun `when user is in US and announcement not dismissed then announcement should shown`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(userIdentity.getUserCountry()).thenReturn(Maybe.just("US"))

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }
}
