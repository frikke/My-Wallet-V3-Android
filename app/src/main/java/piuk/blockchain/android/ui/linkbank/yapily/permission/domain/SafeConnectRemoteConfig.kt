package piuk.blockchain.android.ui.linkbank.yapily.permission.domain

// TODO (othman): move to domain module when refactoring linkbank
interface SafeConnectRemoteConfig {
    suspend fun getTosPdfLink(): String
}
