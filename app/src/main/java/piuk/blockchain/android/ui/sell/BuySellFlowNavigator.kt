package piuk.blockchain.android.ui.sell

import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.OrderState
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.androidcore.utils.extensions.thenSingle

class BuySellFlowNavigator(
    private val simpleBuySyncFactory: SimpleBuySyncFactory,
    private val custodialWalletManager: CustodialWalletManager,
    private val userIdentity: UserIdentity
) {
    fun navigateTo(selectedAsset: AssetInfo? = null): Single<BuySellIntroAction> {
        val state = simpleBuySyncFactory.currentState() ?: SimpleBuyState()

        val cancel: Completable = if (state.orderState == OrderState.PENDING_CONFIRMATION)
            custodialWalletManager.deleteBuyOrder(
                state.id
                    ?: throw IllegalStateException("Pending order should always have an id")
            ).onErrorComplete()
        else Completable.complete()

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
            userIdentity.userAccessForFeature(Feature.SimpleBuy).map { access ->
                when (access) {
                    FeatureAccess.NotRequested,
                    FeatureAccess.Unknown,
                    is FeatureAccess.Granted -> BuySellIntroAction.DisplayBuySellIntro
                    is FeatureAccess.Blocked -> {
                        if (access.isBlockedDueToEligibility())
                            BuySellIntroAction.UserNotEligible
                        else BuySellIntroAction.DisplayBuySellIntro
                    }
                }
            }
        }
    }
}

sealed class BuySellIntroAction {
    object DisplayBuySellIntro : BuySellIntroAction()
    object UserNotEligible : BuySellIntroAction()
    data class StartBuyWithSelectedAsset(val selectedAsset: AssetInfo) :
        BuySellIntroAction()
}
