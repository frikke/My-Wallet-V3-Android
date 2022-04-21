package piuk.blockchain.android.ui.interest.domain.model

import com.blockchain.nabu.datamanagers.repositories.interest.IneligibilityReason
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money

data class AssetInterestInfo(
    val assetInfo: AssetInfo,
    val assetInterestDetail: AssetInterestDetail?
)

data class AssetInterestDetail(
    val totalInterest: Money,
    val totalBalance: Money,
    val rate: Double,
    val eligible: Boolean,
    val ineligibilityReason: IneligibilityReason,
    val totalBalanceFiat: Money
)
