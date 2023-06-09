package com.blockchain.earn.dashboard

import androidx.compose.runtime.Composable
import com.blockchain.commonarch.presentation.base.ComposeModalBottomDialog
import com.blockchain.componentlib.utils.openUrl
import com.blockchain.earn.dashboard.viewmodel.EarnType
import com.blockchain.presentation.urllinks.EARN_LEARN_MORE_URL

class EarnProductComparatorBottomSheet(private val earnProducts: List<EarnType>) : ComposeModalBottomDialog() {

    @Composable
    override fun Sheet() {
        EarnProductComparator(
            products = earnProducts.map { it.toUiElement() },
            onLearnMore = { context?.openUrl(EARN_LEARN_MORE_URL) },
            onClose = { dismiss() }
        )
    }

    private fun EarnType.toUiElement() =
        when (this) {
            EarnType.Passive -> EarnProductUiElement.PassiveRewardsUiElement
            EarnType.Staking -> EarnProductUiElement.StakingRewardsUiElement
            EarnType.Active -> EarnProductUiElement.ActiveRewardsUiElement
        }

    companion object {
        fun newInstance(earnProducts: List<EarnType>) =
            EarnProductComparatorBottomSheet(earnProducts)
    }
}
