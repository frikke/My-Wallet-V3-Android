package piuk.blockchain.android.ui.linkbank.domain.yapily.usecase

import piuk.blockchain.android.ui.linkbank.domain.yapily.service.SafeConnectService

class GetSafeConnectTosLinkUseCase(private val service: SafeConnectService) {
    suspend operator fun invoke(): String = service.getTosLink()
}
