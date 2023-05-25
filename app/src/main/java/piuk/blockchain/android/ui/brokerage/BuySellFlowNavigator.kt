package piuk.blockchain.android.ui.brokerage

import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.Feature
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.utils.thenSingle
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory

class BuySellFlowNavigator(
    private val simpleBuySyncFactory: SimpleBuySyncFactory,
    private val custodialWalletManager: CustodialWalletManager,
    private val userIdentity: UserIdentity,
    private val newSellFlowFF: FeatureFlag
) {
    fun navigateTo(selectedAsset: AssetInfo? = null): Single<BuySellIntroAction> {
        val state = simpleBuySyncFactory.currentState() ?: SimpleBuyState()

        val cancel: Completable = if (state.orderState == OrderState.PENDING_CONFIRMATION) {
            custodialWalletManager.deleteBuyOrder(
                state.id
                    ?: throw IllegalStateException("Pending order should always have an id")
            ).onErrorComplete()
        } else Completable.complete()

        return cancel.doOnComplete {
            simpleBuySyncFactory.clear()
        }.thenSingle {
            decideNavigationStep(
                selectedAsset
            )
        }
    }

    private fun decideNavigationStep(
        selectedAsset: AssetInfo?
    ): Single<BuySellIntroAction> {
        return selectedAsset?.let {
            Single.just(BuySellIntroAction.StartBuyWithSelectedAsset(it))
        } ?: run {
            Single.zip(
                userIdentity.userAccessForFeature(Feature.Buy),
                userIdentity.userAccessForFeature(Feature.Sell),
                newSellFlowFF.enabled
            ) { buyAccess, sellAccess, newSellFlowFFEnabled ->
                if (buyAccess.isBlockedDueToEligibility() && sellAccess.isBlockedDueToEligibility()) {
                    BuySellIntroAction.UserNotEligible
                } else BuySellIntroAction.DisplayBuySellIntro(newSellFlowFFEnabled)
            }
        }
    }
}

sealed class BuySellIntroAction {
    data class DisplayBuySellIntro(val newSellFlowFFEnabled: Boolean) : BuySellIntroAction()
    object UserNotEligible : BuySellIntroAction()
    data class StartBuyWithSelectedAsset(val selectedAsset: AssetInfo) :
        BuySellIntroAction()
}
