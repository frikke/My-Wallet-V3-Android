package piuk.blockchain.android.ui.linkbank.domain.yapily

// TODO (othman): move to domain module when refactoring linkbank
interface SafeConnectService {
    suspend fun getTosLink(): String
}
