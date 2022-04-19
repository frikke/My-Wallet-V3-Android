package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.featureflag.FeatureFlag
import com.blockchain.remoteconfig.RemoteConfig
import com.google.gson.Gson
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class AnnounceConfig(
    val order: List<String> = emptyList(), // Announcement card display order
    val interval: Long = 7 // Period, in days, between re-displaying dismissed periodic cards
)

interface AnnouncementConfigAdapter {
    val announcementConfig: Single<AnnounceConfig> // Priority display order of announcements
}

class AnnouncementConfigAdapterImpl(
    private val config: RemoteConfig,
    private val json: Json,
    private val replaceGsonKtxFF: FeatureFlag
) : AnnouncementConfigAdapter {

    private val gson = Gson()

    override val announcementConfig: Single<AnnounceConfig>
        get() {
            return Singles.zip(replaceGsonKtxFF.enabled.onErrorReturn { false }, config.getRawJson(ANNOUNCE_KEY))
                .map { (replaceGsonKtx, announcementsJson) ->
                    if (replaceGsonKtx) json.decodeFromString(announcementsJson)
                    else gson.fromJson(announcementsJson, AnnounceConfig::class.java)
                }
        }

    companion object {
        const val ANNOUNCE_KEY = "announcements"
    }
}
