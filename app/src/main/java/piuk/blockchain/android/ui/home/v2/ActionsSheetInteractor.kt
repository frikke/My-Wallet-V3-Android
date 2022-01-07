package piuk.blockchain.android.ui.home.v2

import com.blockchain.extensions.exhaustive
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import io.reactivex.rxjava3.core.Single

class ActionsSheetInteractor internal constructor(
    private val userIdentity: UserIdentity,
    private val fabSheetBuySellOrderingFeatureFlag: FabSheetBuySellOrderingFeatureFlag
) {
    fun getUserAccessToSimpleBuy(): Single<ActionsSheetIntent> =
        userIdentity.userAccessForFeature(Feature.SimpleBuy).map { accessState ->
            val blockedState = accessState as? FeatureAccess.Blocked
            blockedState?.let {
                when (val reason = it.reason) {
                    is BlockedReason.TooManyInFlightTransactions -> ActionsSheetIntent.UpdateFlowToLaunch(
                        FlowToLaunch.TooManyPendingBuys(reason.maxTransactions)
                    )
                    BlockedReason.NotEligible ->
                        // launch Buy anyways, because this is handled in that screen
                        ActionsSheetIntent.UpdateFlowToLaunch(FlowToLaunch.BuyFlow)
                }.exhaustive
            } ?: run {
                ActionsSheetIntent.UpdateFlowToLaunch(FlowToLaunch.BuyFlow)
            }
        }

    fun getFabCtaOrdering(): Single<SplitButtonCtaOrdering> =
        fabSheetBuySellOrderingFeatureFlag.enabled.map {
            if (it) {
                SplitButtonCtaOrdering.BUY_END
            } else {
                SplitButtonCtaOrdering.BUY_START
            }
        }
}
