package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.Prices24HrWithDelta
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementQueries
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.resources.AssetResources

class NewAssetAnnouncementTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()
    private val announcementQueries: AnnouncementQueries = mock()
    private val assetResources: AssetResources = mock()

    private lateinit var subject: NewAssetAnnouncement

    @Before
    fun setUp() {
        subject = NewAssetAnnouncement(
            dismissRecorder = dismissRecorder,
            announcementQueries = announcementQueries,
            assetResources = assetResources
        )
    }

    @Test
    fun `should not show, when already shown`() {
        whenever(dismissRecorder[NewAssetAnnouncement.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(NewAssetAnnouncement.DISMISS_KEY)
        whenever(dismissEntry.isDismissed).thenReturn(true)

        whenever(announcementQueries.getAssetFromCatalogue()).thenReturn(Maybe.empty())

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should show, when not already shown, and is a known asset`() {
        val assetDismissKey = "${NewAssetAnnouncement.DISMISS_KEY}${CryptoCurrency.BTC.networkTicker}"
        val prices24HrWithDelta = Prices24HrWithDelta(
            0.0,
            ExchangeRate.zeroRateExchangeRate(CryptoCurrency.BTC, FiatCurrency.Dollars),
            ExchangeRate.zeroRateExchangeRate(CryptoCurrency.BTC, FiatCurrency.Dollars)
        )

        whenever(dismissEntry.prefsKey).thenReturn(assetDismissKey)
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(dismissRecorder[assetDismissKey]).thenReturn(dismissEntry)

        whenever(announcementQueries.getAssetFromCatalogue()).thenReturn(Maybe.just(CryptoCurrency.BTC))
        whenever(announcementQueries.getAssetPrice(CryptoCurrency.BTC)).thenReturn(Observable.just(prices24HrWithDelta))

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when not already shown, and is an unknown asset`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(dismissRecorder[NewAssetAnnouncement.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(NewAssetAnnouncement.DISMISS_KEY)

        whenever(announcementQueries.getAssetFromCatalogue()).thenReturn(Maybe.empty())

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }
}
