package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.domain.usecases.ShouldShowExchangeCampaignUseCase
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder

class ExchangeCampaignAnnouncementTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()
    private val shouldShowExchangeCampaignUseCase: ShouldShowExchangeCampaignUseCase = mock()

    private lateinit var subject: ExchangeCampaignAnnouncement

    @Before
    fun setUp() {
        whenever(dismissRecorder[ExchangeCampaignAnnouncement.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(ExchangeCampaignAnnouncement.DISMISS_KEY)

        subject = ExchangeCampaignAnnouncement(
            dismissRecorder = dismissRecorder,
            shouldShowExchangeCampaignUseCase = shouldShowExchangeCampaignUseCase
        )
    }

    @Test
    fun `given announcement already shown, then do not show it again`() {
        whenever(dismissEntry.isDismissed).thenReturn(true)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `given announcement never shown, when it should not be shown, then do not show it`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(shouldShowExchangeCampaignUseCase.invoke()).thenReturn(Single.just(false))

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `given announcement never shown, when it should be shown, then do show it`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(shouldShowExchangeCampaignUseCase.invoke()).thenReturn(Single.just(true))

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }
}
