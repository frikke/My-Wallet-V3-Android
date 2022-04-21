package piuk.blockchain.android.ui.interest.domain.model

import com.blockchain.nabu.models.responses.nabu.KycTiers
import info.blockchain.balance.AssetInfo

data class InterestDashboard(
    val tiers: KycTiers,
    val enabledAssets: List<AssetInfo>
)
