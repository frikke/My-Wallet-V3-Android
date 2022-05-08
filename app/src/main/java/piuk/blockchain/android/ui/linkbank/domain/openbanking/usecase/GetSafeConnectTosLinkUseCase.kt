package piuk.blockchain.android.ui.linkbank.domain.openbanking.usecase

import piuk.blockchain.android.ui.linkbank.domain.openbanking.service.SafeConnectService

class GetSafeConnectTosLinkUseCase(private val service: SafeConnectService) {
    suspend operator fun invoke(): String = service.getTosLink()
}
