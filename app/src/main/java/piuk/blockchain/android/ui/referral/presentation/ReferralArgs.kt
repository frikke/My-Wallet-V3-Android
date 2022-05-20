package piuk.blockchain.android.ui.referral.presentation

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.domain.referral.model.ReferralInfo
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReferralArgs(
    val code: String,
    val criteria: List<String>,
    val rewardSubtitle: String,
    val rewardTitle: String
) : ModelConfigArgs.ParcelableArgs {

    companion object {
        const val ARGS_KEY = "ReferralArgs"
    }
}

fun ReferralInfo.Data.mapArgs() = ReferralArgs(
    code = code,
    criteria = criteria,
    rewardSubtitle = rewardSubtitle,
    rewardTitle = rewardTitle
)
