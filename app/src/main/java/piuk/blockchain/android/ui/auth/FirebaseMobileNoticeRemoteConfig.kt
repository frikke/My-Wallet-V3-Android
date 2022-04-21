package piuk.blockchain.android.ui.auth

import com.blockchain.featureflag.FeatureFlag
import com.blockchain.remoteconfig.RemoteConfig
import com.squareup.moshi.Moshi
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class FirebaseMobileNoticeRemoteConfig(
    private val remoteConfig: RemoteConfig,
    private val json: Json,
    private val disableMoshiFeatureFlag: FeatureFlag
) : MobileNoticeRemoteConfig {

    private val moshi = Moshi.Builder().build()

    override fun mobileNoticeDialog(): Single<MobileNoticeDialog> =
        disableMoshiFeatureFlag.enabled.flatMapMaybe { isMoshiDisabled ->
            remoteConfig.getRawJson(key)
                .filter { it.isNotEmpty() }
                .map {
                    if (isMoshiDisabled) {
                        try {
                            json.decodeFromString<MobileNoticeDialog>(it)
                        } catch (e: Exception) {
                            MobileNoticeDialog()
                        }
                    } else {
                        moshi.adapter(MobileNoticeDialog::class.java).fromJson(it) ?: MobileNoticeDialog()
                    }
                }
        }
            .toSingle()

    companion object {
        private const val key = "mobile_notice"
    }
}
