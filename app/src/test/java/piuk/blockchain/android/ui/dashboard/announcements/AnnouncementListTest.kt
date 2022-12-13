package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlin.test.assertEquals
import org.junit.Test

class AnnouncementListTest {

    private val host: AnnouncementHost = mock()
    private val orderAdapter: AnnouncementConfigAdapter = mock()
    private val dismissRecorder: DismissRecorder = mock()
    private val walletModeService: WalletModeService = mock {
        on { walletModeSingle }.thenReturn(Single.just(WalletMode.UNIVERSAL))
    }

    private fun createAnnouncementList(
        availableAnnouncements: List<AnnouncementRule>,
        scheduler: Scheduler = Schedulers.trampoline()
    ) =
        AnnouncementList(
            mainScheduler = scheduler,
            orderAdapter = orderAdapter,
            availableAnnouncements = availableAnnouncements,
            dismissRecorder = dismissRecorder,
            walletModeService = walletModeService,
        )

    @Test
    fun `announcement list is correctly mapped`() {

        val order = listOf("three", "two", "one")

        val available = listOf(
            announcement("one"),
            announcement("two"),
            announcement("three")
        )

        val result = createAnnouncementList(available)
            .buildAnnouncementList(order)

        assertEquals(3, result.size)
        assertEquals("three", result[0].name)
        assertEquals("two", result[1].name)
        assertEquals("one", result[2].name)
    }

    @Test
    fun `announcement list is correctly mapped, when order contains unknown announcements`() {

        val order = listOf("three", "two_point_five", "two", "one")

        val available = listOf(
            announcement("one"),
            announcement("two"),
            announcement("three")
        )

        val result = createAnnouncementList(available)
            .buildAnnouncementList(order)

        assertEquals(3, result.size)
        assertEquals("three", result[0].name)
        assertEquals("two", result[1].name)
        assertEquals("one", result[2].name)
    }

    @Test
    fun `announcement list is correctly mapped, when order skips announcement`() {

        val order = listOf("three", "two", "one")

        val available = listOf(
            announcement("one"),
            announcement("two"),
            announcement("three"),
            announcement("four")
        )

        val result = createAnnouncementList(available)
            .buildAnnouncementList(order)

        assertEquals(3, result.size)
        assertEquals("three", result[0].name)
        assertEquals("two", result[1].name)
        assertEquals("one", result[2].name)
    }

    @Test
    fun `calls no announcements until subscribed`() {

        val order = listOf("three", "two", "one")
        whenever(orderAdapter.announcementConfig).thenReturn(Single.just(AnnounceConfig(order, INTERVAL)))

        val available = listOf(
            announcement("one"),
            announcement("two"),
            announcement("three"),
            announcement("four")
        )

        createAnnouncementList(available)
            .nextAnnouncement()

        verifyZeroInteractions(host)
    }

    @Test
    fun `calls first announcement that says it should show`() {

        val order = listOf("one", "two", "three")
        whenever(orderAdapter.announcementConfig).thenReturn(Single.just(AnnounceConfig(order, INTERVAL)))

        val available = listOf(
            dontShowAnnouncement("one"),
            announcement("two"),
            announcement("three"),
            announcement("four")
        )

        createAnnouncementList(available)
            .nextAnnouncement()
            .test()
            .assertValue(available[1])
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `nothing available`() {
        val order = listOf("one", "two", "three")
        whenever(orderAdapter.announcementConfig).thenReturn(Single.just(AnnounceConfig(order, INTERVAL)))

        val available = listOf(
            dontShowAnnouncement("one"),
            dontShowAnnouncement("two"),
            dontShowAnnouncement("three"),
            dontShowAnnouncement("four")
        )

        createAnnouncementList(available)
            .nextAnnouncement()
            .test()
            .assertValues()
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `does not check announcements beyond one that says it should show`() {

        val order = listOf("one", "two", "three")
        whenever(orderAdapter.announcementConfig).thenReturn(Single.just(AnnounceConfig(order, INTERVAL)))

        val available = listOf(
            announcement("one"),
            dontCheckAnnouncement("two"),
            dontCheckAnnouncement("three"),
            dontCheckAnnouncement("four")
        )

        createAnnouncementList(available)
            .nextAnnouncement()
            .test()
            .assertValue(available[0])
            .assertComplete()
            .assertNoErrors()
    }

    private fun announcement(announcementName: String): AnnouncementRule =
        mock {
            on { shouldShow() }.thenReturn(Single.just(true))
            on { associatedWalletModes }.thenReturn(WalletMode.values().toList())
            on { name }.thenReturn(announcementName)
        }

    private fun dontShowAnnouncement(announcementName: String): AnnouncementRule =
        mock {
            on { shouldShow() }.thenReturn(Single.just(false))
            on { show(host) }.thenThrow(RuntimeException("Not expected"))
            on { associatedWalletModes }.thenReturn(WalletMode.values().toList())
            on { name }.thenReturn(announcementName)
        }

    private fun dontCheckAnnouncement(announcementName: String): AnnouncementRule =
        mock {
            on { shouldShow() }.thenThrow(RuntimeException("Not expected"))
            on { show(host) }.thenThrow(RuntimeException("Not expected"))
            on { associatedWalletModes }.thenReturn(WalletMode.values().toList())
            on { name }.thenReturn(announcementName)
        }

    companion object {
        private const val INTERVAL = 7L
    }
}
