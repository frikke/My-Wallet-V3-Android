package piuk.blockchain.android.ui.linkbank.presentation.openbanking.permission

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.extensions.exhaustive
import kotlinx.coroutines.launch
import piuk.blockchain.android.ui.linkbank.domain.openbanking.usecase.GetSafeConnectTosLinkUseCase

class OpenBankingPermissionViewModel(
    private val getSafeConnectTosLinkUseCase: GetSafeConnectTosLinkUseCase
) : MviViewModel<OpenBankingPermissionIntents,
    OpenBankingPermissionViewState,
    OpenBankingPermissionModelState,
    OpenBankingPermissionNavEvent,
    OpenBankingPermissionArgs>(
    initialState = OpenBankingPermissionModelState()
) {
    override fun viewCreated(args: OpenBankingPermissionArgs) {
        updateState { it.copy(institution = args.institution) }
    }

    override suspend fun handleIntent(
        modelState: OpenBankingPermissionModelState,
        intent: OpenBankingPermissionIntents
    ) {
        when (intent) {
            is OpenBankingPermissionIntents.GetTermsOfServiceLink -> {
                getTermsOfServiceLink()
            }

            is OpenBankingPermissionIntents.ApproveClicked -> {
                modelState.institution?.let { navigate(OpenBankingPermissionNavEvent.AgreementAccepted(it)) }
            }

            OpenBankingPermissionIntents.DenyClicked -> {
                navigate(OpenBankingPermissionNavEvent.AgreementDenied)
            }
        }.exhaustive
    }

    override fun reduce(state: OpenBankingPermissionModelState): OpenBankingPermissionViewState {
        return OpenBankingPermissionViewState(
            termsOfServiceLink = state.termsOfServiceLink
        )
    }

    private fun getTermsOfServiceLink() {
        viewModelScope.launch {
            getSafeConnectTosLinkUseCase()
                .also { termsOfServiceLink ->
                    updateState { it.copy(termsOfServiceLink = termsOfServiceLink) }
                }
        }
    }
}
