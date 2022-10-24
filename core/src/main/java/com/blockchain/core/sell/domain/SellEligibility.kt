package com.blockchain.core.sell.domain

import com.blockchain.nabu.BlockedReason
import info.blockchain.balance.AssetInfo

sealed class SellEligibility {
    data class NotEligible(val reason: BlockedReason) : SellEligibility()
    data class KycBlocked(val reason: SellUserEligibility) : SellEligibility()
    data class Eligible(val sellAssets: List<AssetInfo>) : SellEligibility()
}

sealed class SellUserEligibility {
    object KycdUser : SellUserEligibility()
    object KycRejectedUser : SellUserEligibility()
    object NonKycdUser : SellUserEligibility()
}
