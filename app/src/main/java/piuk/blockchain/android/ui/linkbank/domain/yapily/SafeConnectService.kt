package piuk.blockchain.android.ui.linkbank.domain.yapily

interface SafeConnectService {
    suspend fun getTosLink(): String
}
