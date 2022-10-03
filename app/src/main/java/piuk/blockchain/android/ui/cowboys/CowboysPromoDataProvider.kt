package piuk.blockchain.android.ui.cowboys

import com.blockchain.api.ActionData
import com.blockchain.api.referral.data.MediaInfo
import com.blockchain.api.referral.data.StyleInfo
import com.blockchain.api.referral.data.UrlInfo
import com.blockchain.domain.common.model.PromotionStyleInfo
import com.blockchain.domain.common.model.ServerErrorAction
import com.blockchain.remoteconfig.RemoteConfig
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class CowboysPromoDataProvider(
    private val config: RemoteConfig,
    private val json: Json
) {

    fun getWelcomeInterstitial(): Single<PromotionStyleInfo> =
        config.getRawJson(KEY_INTERSTITIAL_WELCOME).map { data ->
            json.decodeFromString<CowboysData>(data).toDomain()
        }

    fun getRaffleInterstitial(): Single<PromotionStyleInfo> =
        config.getRawJson(KEY_INTERSTITIAL_RAFFLE).map { data ->
            json.decodeFromString<CowboysData>(data).toDomain()
        }

    fun getIdentityInterstitial(): Single<PromotionStyleInfo> =
        config.getRawJson(KEY_INTERSTITIAL_IDENTITY).map { data ->
            json.decodeFromString<CowboysData>(data).toDomain()
        }

    fun getWelcomeAnnouncement(): Single<PromotionStyleInfo> =
        config.getRawJson(KEY_ANNOUNCEMENT_WELCOME).map { data ->
            json.decodeFromString<CowboysData>(data).toDomain()
        }

    fun getRaffleAnnouncement(): Single<PromotionStyleInfo> =
        config.getRawJson(KEY_ANNOUNCEMENT_RAFFLE).map { data ->
            json.decodeFromString<CowboysData>(data).toDomain()
        }

    fun getKycInProgressAnnouncement(): Single<PromotionStyleInfo> =
        config.getRawJson(KEY_ANNOUNCEMENT_KYC_IN_PROGRESS).map { data ->
            json.decodeFromString<CowboysData>(data).toDomain()
        }

    fun getIdentityAnnouncement(): Single<PromotionStyleInfo> =
        config.getRawJson(KEY_ANNOUNCEMENT_IDENTITY).map { data ->
            json.decodeFromString<CowboysData>(data).toDomain()
        }

    fun getReferFriendsAnnouncement(): Single<PromotionStyleInfo> =
        config.getRawJson(KEY_ANNOUNCEMENT_REFER).map { data ->
            json.decodeFromString<CowboysData>(data).toDomain()
        }

    companion object {
        private const val BASE_PATH = "blockchain_ux_onboarding_promotion_cowboys"
        private const val KEY_INTERSTITIAL_WELCOME = "${BASE_PATH}_welcome_story"
        private const val KEY_ANNOUNCEMENT_WELCOME = "${BASE_PATH}_welcome_announcement"
        private const val KEY_INTERSTITIAL_RAFFLE = "${BASE_PATH}_raffle_story"
        private const val KEY_ANNOUNCEMENT_RAFFLE = "${BASE_PATH}_raffle_announcement"
        private const val KEY_INTERSTITIAL_IDENTITY = "${BASE_PATH}_verify_identity_story"
        private const val KEY_ANNOUNCEMENT_IDENTITY = "${BASE_PATH}_verify_identity_announcement"
        private const val KEY_ANNOUNCEMENT_KYC_IN_PROGRESS = "${BASE_PATH}_user_kyc_is_under_review_announcement"
        private const val KEY_ANNOUNCEMENT_REFER = "${BASE_PATH}_refer_friends_announcement"
    }
}

enum class FlowStep {
    EmailVerification,
    Welcome,
    Raffle,
    Verify
}

@Serializable
private data class CowboysData(
    val title: String,
    val message: String,
    val header: MediaInfo?,
    val icon: UrlInfo?,
    val style: StyleInfo?,
    val actions: List<ActionData>,
) {
    fun toDomain(): PromotionStyleInfo =
        PromotionStyleInfo(
            title = title,
            message = message,
            iconUrl = icon?.url.orEmpty(),
            headerUrl = header?.media?.url.orEmpty(),
            backgroundUrl = style?.background?.media?.url.orEmpty(),
            foregroundColorScheme = style?.foreground?.color?.hsb ?: emptyList(),
            actions = actions.map {
                ServerErrorAction(
                    it.title,
                    it.url.orEmpty()
                )
            }
        )
}
