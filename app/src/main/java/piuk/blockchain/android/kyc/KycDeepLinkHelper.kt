package piuk.blockchain.android.kyc

import android.content.Intent
import android.net.Uri
import com.blockchain.deeplinking.processor.KycLinkState
import com.blockchain.nabu.models.responses.nabu.CampaignData
import com.blockchain.notifications.links.PendingLink
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

fun Uri.ignoreFragment(): Uri {
    val cleanedUri = this.toString().replace("/#", "")
    return Uri.parse(cleanedUri)
}

class KycDeepLinkHelper(
    private val linkHandler: PendingLink
) {

    fun getLink(intent: Intent): Single<KycLinkState> =
        linkHandler.getPendingLinks(intent)
            .map(this::mapUri)
            .switchIfEmpty(Maybe.just(KycLinkState.NoUri))
            .toSingle()
            .onErrorResumeNext { Single.just(KycLinkState.NoUri) }

    fun mapUri(uri: Uri): KycLinkState {
        val uriWithoutFragment = uri.ignoreFragment()
        val name = uriWithoutFragment.getQueryParameter("deep_link_path")
        return when (name) {
            "verification" -> KycLinkState.Resubmit
            "email_verified" -> {
                val ctx = uriWithoutFragment.getQueryParameter("context")?.toLowerCase()
                return if (KYC_CONTEXT == ctx) KycLinkState.EmailVerified else KycLinkState.NoUri
            }
            "kyc" -> {
                val campaign = uriWithoutFragment.getQueryParameter("campaign")
                val campaignData = if (!campaign.isNullOrEmpty()) CampaignData(campaign, false) else null
                return KycLinkState.General(campaignData)
            }
            else -> KycLinkState.NoUri
        }
    }

    companion object {
        const val KYC_CONTEXT = "kyc"
    }
}
