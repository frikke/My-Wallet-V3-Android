package piuk.blockchain.android.ui.linkbank.domain.openbanking.service

interface SafeConnectService {
    suspend fun getTosLink(): String
}
