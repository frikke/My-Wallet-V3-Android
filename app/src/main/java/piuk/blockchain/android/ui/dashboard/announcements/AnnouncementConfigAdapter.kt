package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.remoteconfig.RemoteConfig
import io.reactivex.rxjava3.core.Single
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
    private val json: Json
) : AnnouncementConfigAdapter {

    override val announcementConfig: Single<AnnounceConfig>
        get() {
            return config.getRawJson(ANNOUNCE_KEY)
                .map { announcementsJson -> json.decodeFromString(announcementsJson) }
        }

    companion object {
        const val ANNOUNCE_KEY = "announcements"
    }
}
