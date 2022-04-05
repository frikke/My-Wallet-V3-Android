package piuk.blockchain.android.ui.interest.tbm.presentation

import piuk.blockchain.android.ui.interest.tbm.domain.model.AssetInterestInfo

sealed interface InterestDashboardItem {
    object InterestIdentityVerificationItem : InterestDashboardItem

    data class InterestAssetInfoItem(
        val assetInterestInfo: AssetInterestInfo
    ) : InterestDashboardItem
}
