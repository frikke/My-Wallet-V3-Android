package piuk.blockchain.android.ui.linkbank.yapily.permission.data

import com.blockchain.remoteconfig.RemoteConfig
import kotlinx.coroutines.rx3.await
import piuk.blockchain.android.ui.linkbank.yapily.permission.domain.SafeConnectRemoteConfig

class SafeConnectRemoteConfigImpl(
    private val remoteConfig: RemoteConfig
) : SafeConnectRemoteConfig {
    companion object {
        private const val SAFECONNECT_TOS_PDF_KEY = "android_safeconnect_tos_pdf"
    }

    override suspend fun getTosPdfLink(): String {
        return remoteConfig.getRawJson(SAFECONNECT_TOS_PDF_KEY).await()
    }
}
