package piuk.blockchain.android.ui.home.models

import com.blockchain.extensions.exhaustive
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import io.reactivex.rxjava3.core.Single

class ActionsSheetInteractor internal constructor(
    private val userIdentity: UserIdentity,
) {
    fun getUserAccessToSimpleBuy(): Single<ActionsSheetIntent> =
        userIdentity.userAccessForFeature(Feature.SimpleBuy).map { accessState ->
            val blockedState = accessState as? FeatureAccess.Blocked
            blockedState?.let {
                when (val reason = it.reason) {
                    is BlockedReason.TooManyInFlightTransactions -> ActionsSheetIntent.UpdateFlowToLaunch(
                        FlowToLaunch.TooManyPendingBuys(reason.maxTransactions)
                    )
                    BlockedReason.NotEligible,
                    is BlockedReason.Sanctions,
                    is BlockedReason.InsufficientTier ->
                        // launch Buy anyways, because this is handled in that screen
                        ActionsSheetIntent.UpdateFlowToLaunch(FlowToLaunch.BuyFlow)
                }.exhaustive
            } ?: run {
                ActionsSheetIntent.UpdateFlowToLaunch(FlowToLaunch.BuyFlow)
            }
        }

    fun getFabCtaOrdering(): Single<SplitButtonCtaOrdering> =
        Single.just(SplitButtonCtaOrdering.BUY_END)
}
