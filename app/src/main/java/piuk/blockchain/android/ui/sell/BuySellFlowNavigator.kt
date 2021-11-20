package piuk.blockchain.android.ui.sell

import com.blockchain.nabu.Feature
import com.blockchain.nabu.Tier
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
        val isGoldButNotEligible = Single.zip(
            userIdentity.isVerifiedFor(Feature.TierLevel(Tier.GOLD)),
            userIdentity.isEligibleFor(Feature.SimpleBuy)
        ) { gold, eligible ->
            gold && !eligible
        }
        return cancel.thenSingle {
            isGoldButNotEligible.map { isGoldButNotEligible ->
                decideNavigationStep(
                    selectedAsset, isGoldButNotEligible, state
                )
            }
        }
    }

    private fun decideNavigationStep(
        selectedAsset: AssetInfo?,
        isGoldButNotEligible: Boolean,
        state: SimpleBuyState
    ) =
        selectedAsset?.let {
            BuySellIntroAction.StartBuyWithSelectedAsset(it, state.hasPendingBuy())
        } ?: kotlin.run {
            BuySellIntroAction.DisplayBuySellIntro(
                isGoldButNotEligible = isGoldButNotEligible,
                hasPendingBuy = state.hasPendingBuy()
            )
        }
}

private fun SimpleBuyState.hasPendingBuy(): Boolean =
    orderState > OrderState.PENDING_CONFIRMATION && orderState < OrderState.FINISHED

sealed class BuySellIntroAction {
    data class DisplayBuySellIntro(val isGoldButNotEligible: Boolean, val hasPendingBuy: Boolean) : BuySellIntroAction()
    data class StartBuyWithSelectedAsset(val selectedAsset: AssetInfo, val hasPendingBuy: Boolean) :
        BuySellIntroAction()
}
