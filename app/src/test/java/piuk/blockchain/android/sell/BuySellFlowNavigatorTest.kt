package piuk.blockchain.android.sell

import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.OrderState
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.ui.brokerage.BuySellFlowNavigator
import piuk.blockchain.android.ui.brokerage.BuySellIntroAction

class BuySellFlowNavigatorTest {

    private val simpleBuySyncFactory: SimpleBuySyncFactory = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val userIdentity: UserIdentity = mock {
        on { isVerifiedFor(Feature.TierLevel(KycTier.GOLD)) }.thenReturn(Single.just(true))
        on { isEligibleFor(Feature.Buy) }.thenReturn(Single.just(true))
        on { isEligibleFor(Feature.Sell) }.thenReturn(Single.just(true))
    }
    private lateinit var subject: BuySellFlowNavigator

    @Before
    fun setUp() {
        subject = BuySellFlowNavigator(
            simpleBuySyncFactory,
            custodialWalletManager,
            userIdentity
        )

        whenever(simpleBuySyncFactory.currentState()).thenReturn(
            SimpleBuyState()
        )
    }

    @Test
    fun `when user is not eligible to neither buy nor sell, corresponding state should be propagated to the UI`() {
        whenever(userIdentity.userAccessForFeature(Feature.Buy))
            .thenReturn(Single.just(FeatureAccess.Blocked(BlockedReason.NotEligible(null))))
        whenever(userIdentity.userAccessForFeature(Feature.Sell))
            .thenReturn(Single.just(FeatureAccess.Blocked(BlockedReason.NotEligible(null))))

        val test = subject.navigateTo().test()

        test.assertValue(BuySellIntroAction.UserNotEligible)
    }

    @Test
    fun `whenBuyStateIsNotPendingCurrencyIsSupportedAndSellIsEnableNormalBuySellUiIsDisplayed`() {
        whenever(userIdentity.userAccessForFeature(Feature.Buy))
            .thenReturn(Single.just(FeatureAccess.Granted()))
        whenever(userIdentity.userAccessForFeature(Feature.Sell))
            .thenReturn(Single.just(FeatureAccess.Granted()))

        val test = subject.navigateTo().test()

        test.assertValue(BuySellIntroAction.DisplayBuySellIntro)
    }

    @Test
    fun `whenBuyStateIsPendingConfirmationOrderIsCancelledAndBuySellUiIsDisplayed`() {
        whenever(simpleBuySyncFactory.currentState()).thenReturn(
            SimpleBuyState(
                id = "ORDERID",
                orderState = OrderState.PENDING_CONFIRMATION
            )
        )

        whenever(userIdentity.userAccessForFeature(Feature.Buy))
            .thenReturn(Single.just(FeatureAccess.Granted()))
        whenever(userIdentity.userAccessForFeature(Feature.Sell))
            .thenReturn(Single.just(FeatureAccess.Granted()))

        whenever(custodialWalletManager.deleteBuyOrder("ORDERID"))
            .thenReturn(Completable.complete())

        val test = subject.navigateTo().test()

        test.assertValue(BuySellIntroAction.DisplayBuySellIntro)
        verify(custodialWalletManager).deleteBuyOrder("ORDERID")
    }
}
