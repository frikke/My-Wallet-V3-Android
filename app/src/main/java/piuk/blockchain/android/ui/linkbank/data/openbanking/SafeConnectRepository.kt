package piuk.blockchain.android.ui.linkbank.data.openbanking

import com.blockchain.domain.experiments.RemoteConfigService
import kotlinx.coroutines.rx3.await
import piuk.blockchain.android.ui.linkbank.domain.openbanking.service.SafeConnectService

internal class SafeConnectRepository(
    private val remoteConfigService: RemoteConfigService
) : SafeConnectService {
    companion object {
        private const val SAFECONNECT_TOS_KEY = "android_safeconnect_tos"
    }

    override suspend fun getTosLink(): String {
        return remoteConfigService.getRawJson(SAFECONNECT_TOS_KEY).await()
    }
}
