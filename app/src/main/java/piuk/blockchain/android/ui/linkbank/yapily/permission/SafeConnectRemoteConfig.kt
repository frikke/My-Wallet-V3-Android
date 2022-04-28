package piuk.blockchain.android.ui.linkbank.yapily.permission

import com.blockchain.remoteconfig.RemoteConfig
import kotlinx.coroutines.rx3.await

// TODO (othman): move to data module when refactoring linkbank
class SafeConnectRemoteConfig(
    private val remoteConfig: RemoteConfig
) {
    companion object {
        private const val SAFECONNECT_TOS_PDF_KEY = "android_safeconnect_tos_pdf"
    }

    suspend fun getTosPdfLink(): String {
        return remoteConfig.getRawJson(SAFECONNECT_TOS_PDF_KEY).await()
    }
}
