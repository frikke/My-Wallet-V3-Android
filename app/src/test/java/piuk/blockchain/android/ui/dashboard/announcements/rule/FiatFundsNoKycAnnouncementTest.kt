package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder

class FiatFundsNoKycAnnouncementTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()
    private val userIdentity: UserIdentity = mock()

    private lateinit var subject: FiatFundsNoKycAnnouncement

    @Before
    fun setUp() {
        whenever(dismissRecorder[FiatFundsNoKycAnnouncement.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(FiatFundsNoKycAnnouncement.DISMISS_KEY)

        subject =
            FiatFundsNoKycAnnouncement(
                dismissRecorder = dismissRecorder,
                userIdentity = userIdentity
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
    fun `should show, when not already shown and user is not kyc gold`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(userIdentity.getHighestApprovedKycTier())
            .thenReturn(Single.just(Tier.BRONZE))

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when not already shown and user is kyc gold`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(userIdentity.getHighestApprovedKycTier())
            .thenReturn(Single.just(Tier.GOLD))

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }
}
