package piuk.blockchain.android.ui.linkbank.domain.yapily.service

interface SafeConnectService {
    suspend fun getTosLink(): String
}
