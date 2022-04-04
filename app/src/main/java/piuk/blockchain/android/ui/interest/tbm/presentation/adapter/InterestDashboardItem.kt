package piuk.blockchain.android.ui.interest.tbm.presentation.adapter

import piuk.blockchain.android.ui.interest.tbm.domain.model.AssetInterestInfo

sealed interface InterestDashboardItem {
    object InterestIdentityVerificationItem : InterestDashboardItem

    data class InterestAssetInfoItem(
        val isKycGold: Boolean,
        val assetInterestInfo: AssetInterestInfo
    ) : InterestDashboardItem
}
