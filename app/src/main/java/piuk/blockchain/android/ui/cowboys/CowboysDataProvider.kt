package piuk.blockchain.android.ui.cowboys

import com.blockchain.api.ActionData
import com.blockchain.domain.common.model.ServerErrorAction
import com.blockchain.remoteconfig.RemoteConfig
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class CowboysDataProvider(
    private val config: RemoteConfig,
    private val json: Json
) {

    fun getWelcomeInterstitial(): Single<CowboysInterstitialInfo> =
        config.getRawJson(KEY_INTERSTITIAL_WELCOME).map { data ->
            json.decodeFromString<CowboysInterstitialData>(data).toDomain()
        }

    fun getRaffleInterstitial(): Single<CowboysInterstitialInfo> =
        config.getRawJson(KEY_INTERSTITIAL_RAFFLE).map { data ->
            json.decodeFromString<CowboysInterstitialData>(data).toDomain()
        }

    fun getIdentityInterstitial(): Single<CowboysInterstitialInfo> =
        config.getRawJson(KEY_INTERSTITIAL_IDENTITY).map { data ->
            json.decodeFromString<CowboysInterstitialData>(data).toDomain()
        }

    fun getWelcomeAnnouncement(): Single<CowboysAnnouncementInfo> =
        config.getRawJson(KEY_ANNOUNCEMENT_WELCOME).map { data ->
            json.decodeFromString<CowboysAnnouncementData>(data).toDomain()
        }

    fun getRaffleAnnouncement(): Single<CowboysAnnouncementInfo> =
        config.getRawJson(KEY_ANNOUNCEMENT_RAFFLE).map { data ->
            json.decodeFromString<CowboysAnnouncementData>(data).toDomain()
        }

    fun getIdentityAnnouncement(): Single<CowboysAnnouncementInfo> =
        config.getRawJson(KEY_ANNOUNCEMENT_IDENTITY).map { data ->
            json.decodeFromString<CowboysAnnouncementData>(data).toDomain()
        }

    companion object {
        private const val BASE_PATH = "blockchain_ux_onboarding_promotion_cowboys"
        private const val KEY_INTERSTITIAL_WELCOME = "${BASE_PATH}_welcome_story"
        private const val KEY_ANNOUNCEMENT_WELCOME = "${BASE_PATH}_welcome_announcement"
        private const val KEY_INTERSTITIAL_RAFFLE = "${BASE_PATH}_raffle_story"
        private const val KEY_ANNOUNCEMENT_RAFFLE = "${BASE_PATH}_raffle_announcement"
        private const val KEY_INTERSTITIAL_IDENTITY = "${BASE_PATH}_verify_identity_story"
        private const val KEY_ANNOUNCEMENT_IDENTITY = "${BASE_PATH}_verify_identity_announcement"
    }
}

enum class FlowStep {
    Welcome,
    Raffle,
    Verify
}

data class CowboysInterstitialInfo(
    val title: String,
    val message: String,
    val iconUrl: String,
    val backgroundUrl: String,
    val foregroundUrl: String,
    val actions: List<ServerErrorAction>
)

data class CowboysAnnouncementInfo(
    val title: String,
    val message: String,
    val iconUrl: String,
    val actions: List<ServerErrorAction>
)

@Serializable
private data class CowboysAnnouncementData(
    val title: String,
    val message: String,
    val icon: UrlInfo,
    val actions: List<ActionData>
) {
    fun toDomain(): CowboysAnnouncementInfo =
        CowboysAnnouncementInfo(
            title = title,
            message = message,
            iconUrl = icon.url,
            actions = actions.map {
                ServerErrorAction(
                    it.title,
                    it.url.orEmpty()
                )
            }
        )
}

@Serializable
private data class CowboysInterstitialData(
    val title: String,
    val message: String,
    val icon: UrlInfo,
    val style: StyleInfo,
    val actions: List<ActionData>
) {
    fun toDomain(): CowboysInterstitialInfo =
        CowboysInterstitialInfo(
            title = title,
            message = message,
            iconUrl = icon.url,
            backgroundUrl = style.background.media.url,
            foregroundUrl = style.foreground.media.url,
            actions = actions.map {
                ServerErrorAction(
                    it.title,
                    it.url.orEmpty()
                )
            }
        )
}

@Serializable
private data class StyleInfo(
    val background: MediaInfo,
    val foreground: MediaInfo
)

@Serializable
private data class MediaInfo(
    val media: UrlInfo
)

@Serializable
private data class UrlInfo(
    val url: String
)
