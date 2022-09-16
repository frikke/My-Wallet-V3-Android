package piuk.blockchain.android.ui.auth

import com.blockchain.domain.experiments.RemoteConfigService
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class FirebaseMobileNoticeRemoteConfig(
    private val remoteConfig: RemoteConfigService,
    private val json: Json
) : MobileNoticeRemoteConfig {

    override fun mobileNoticeDialog(): Single<MobileNoticeDialog> =
        remoteConfig.getRawJson(key)
            .filter { it.isNotEmpty() }
            .map {
                try {
                    json.decodeFromString<MobileNoticeDialog>(it)
                } catch (e: Exception) {
                    MobileNoticeDialog()
                }
            }
            .toSingle()

    companion object {
        private const val key = "mobile_notice"
    }
}
