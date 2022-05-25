package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.model.BankState
import com.blockchain.domain.paymentmethods.model.LinkedPaymentMethod
import com.blockchain.nabu.Feature
import com.blockchain.nabu.UserIdentity
import com.blockchain.testutils.USD
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder

class FiatFundsKycAnnouncementTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()
    private val userIdentity: UserIdentity = mock()
    private val bankService: BankService = mock()

    private lateinit var subject: FiatFundsKycAnnouncement

    @Before
    fun setUp() {
        whenever(dismissRecorder[FiatFundsKycAnnouncement.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(FiatFundsKycAnnouncement.DISMISS_KEY)

        subject =
            FiatFundsKycAnnouncement(
                dismissRecorder = dismissRecorder,
                userIdentity = userIdentity,
                bankService = bankService
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
    fun `should show, when not already shown and user is kyc gold without linked banks`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(userIdentity.isEligibleFor(Feature.SimpleBuy))
            .thenReturn(Single.just(true))

        whenever(bankService.getLinkedBanks()).thenReturn(Single.just(emptyList()))

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when not already shown and user is kyc gold but has linked banks`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(userIdentity.isEligibleFor(Feature.SimpleBuy))
            .thenReturn(Single.just(true))

        whenever(bankService.getLinkedBanks()).thenReturn(
            Single.just(
                listOf(
                    LinkedPaymentMethod.Bank("", "", "", "", "", false, BankState.ACTIVE, USD)
                )
            )
        )

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when not already shown and user is not kyc gold and has no linked banks`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(userIdentity.isEligibleFor(Feature.SimpleBuy))
            .thenReturn(Single.just(false))

        whenever(bankService.getLinkedBanks()).thenReturn(Single.just(emptyList()))

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }
}
