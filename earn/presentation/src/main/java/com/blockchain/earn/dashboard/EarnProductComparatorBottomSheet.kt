package com.blockchain.earn.dashboard

import androidx.compose.runtime.Composable
import com.blockchain.commonarch.presentation.base.ComposeModalBottomDialog
import com.blockchain.componentlib.utils.openUrl
import com.blockchain.earn.dashboard.viewmodel.EarnType
import com.blockchain.presentation.urllinks.EARN_LEARN_MORE_URL

class EarnProductComparatorBottomSheet(private val earnProducts: Map<EarnType, Double>) : ComposeModalBottomDialog() {

    @Composable
    override fun Sheet() {
        EarnProductComparator(
            products = earnProducts.map { it.toPair().toUiElement() },
            onLearnMore = { context?.openUrl(EARN_LEARN_MORE_URL) },
            onClose = { dismiss() }
        )
    }

    private fun Pair<EarnType, Double>.toUiElement() = this.let { (earnType, rate) ->
        when (earnType) {
            EarnType.Passive -> EarnProductUiElement.PassiveRewardsUiElement(rate = rate)
            EarnType.Staking -> EarnProductUiElement.StakingRewardsUiElement(rate = rate)
            EarnType.Active -> EarnProductUiElement.ActiveRewardsUiElement(rate = rate)
        }
    }

    companion object {
        fun newInstance(earnProducts: Map<EarnType, Double>) =
            EarnProductComparatorBottomSheet(earnProducts)
    }
}
