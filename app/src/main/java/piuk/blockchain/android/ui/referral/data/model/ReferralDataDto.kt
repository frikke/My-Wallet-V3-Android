package piuk.blockchain.android.ui.referral.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import piuk.blockchain.android.ui.referral.domain.model.ReferralData

@Serializable
data class ReferralDataDto(
    @SerialName("rewardTitle") val rewardTitle: String,
    @SerialName("rewardSubtitle") val rewardSubtitle: String,
    @SerialName("code") val code: String,
    @SerialName("criteria") val criteria: List<String>
)

fun ReferralData.mapDomain(): ReferralData = this.run {
    ReferralData(
        rewardTitle = rewardTitle,
        rewardSubtitle = rewardSubtitle,
        code = code,
        criteria = criteria
    )
}