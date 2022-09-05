package piuk.blockchain.android.ui.dashboard.announcements

import androidx.annotation.VisibleForTesting
import com.blockchain.nabu.UserIdentity
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
    private val json: Json,
    private val userIdentity: UserIdentity
) : AnnouncementConfigAdapter {

    override val announcementConfig: Single<AnnounceConfig>
        get() = userIdentity.isCowboysUser().flatMap { isCowboysUser ->
            if (isCowboysUser) {
                config.getIfFeatureEnabled(COWBOYS_SEE_ANNOUNCEMENTS_KEY).flatMap { shouldSeeAnnouncements ->
                    if (shouldSeeAnnouncements) {
                        getAnnouncements()
                    } else {
                        Single.just(AnnounceConfig())
                    }
                }
            } else {
                getAnnouncements()
            }
        }

    private fun getAnnouncements(): Single<AnnounceConfig> =
        config.getRawJson(ANNOUNCE_KEY)
            .map { announcementsJson -> json.decodeFromString<AnnounceConfig>(announcementsJson) }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val ANNOUNCE_KEY = "announcements"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val COWBOYS_SEE_ANNOUNCEMENTS_KEY = "cowboys_users_should_see_announcements"
    }
}
