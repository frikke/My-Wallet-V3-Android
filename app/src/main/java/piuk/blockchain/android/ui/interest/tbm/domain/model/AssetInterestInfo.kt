package piuk.blockchain.android.ui.interest.tbm.domain.model

import com.blockchain.nabu.datamanagers.repositories.interest.IneligibilityReason
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money

data class AssetInterestInfo(
    val assetInfo: AssetInfo,
    val assetInterestDetail: AssetInterestDetail?
) {
    fun getAssetName() = assetInfo.name
    fun getAssetLogo() = assetInfo.logo
    fun getAssetDisplayTicker() = assetInfo.displayTicker

    fun hasDetail() = assetInterestDetail != null

    fun getInterestRate() = assetInterestDetail?.rate
    fun getBalance() = assetInterestDetail?.totalBalance
    fun getInligibilityReason() = assetInterestDetail?.ineligibilityReason
}

data class AssetInterestDetail(
    val totalInterest: Money,
    val totalBalance: Money,
    val rate: Double,
    val eligible: Boolean,
    val ineligibilityReason: IneligibilityReason,
    val totalBalanceFiat: Money
)