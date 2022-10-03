package piuk.blockchain.android.ui.referral.presentation

import android.os.Parcelable
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.domain.referral.model.ReferralInfo
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReferralArgs(
    val code: String,
    val criteria: List<String>,
    val campaignId: String,
    val rewardSubtitle: String,
    val rewardTitle: String,
    val promotionData: ReferralPromotionStyleInfo?
) : ModelConfigArgs.ParcelableArgs {

    companion object {
        const val ARGS_KEY = "ReferralArgs"
    }
}

@Parcelize
class ReferralPromotionStyleInfo(
    val title: String,
    val message: String,
    val iconUrl: String,
    val backgroundUrl: String,
) : Parcelable

fun ReferralInfo.Data.mapArgs() = ReferralArgs(
    code = code,
    criteria = criteria,
    campaignId = campaignId,
    rewardSubtitle = rewardSubtitle,
    rewardTitle = rewardTitle,
    promotionData = promotionInfo?.run {
        ReferralPromotionStyleInfo(
            title = title,
            message = message,
            iconUrl = iconUrl,
            backgroundUrl = backgroundUrl
        )
    }
)
