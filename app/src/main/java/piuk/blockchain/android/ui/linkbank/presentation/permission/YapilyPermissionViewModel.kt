package piuk.blockchain.android.ui.linkbank.presentation.permission

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.extensions.exhaustive
import kotlinx.coroutines.launch
import piuk.blockchain.android.ui.linkbank.domain.yapily.SafeConnectService

class YapilyPermissionViewModel(
    private val safeConnectService: SafeConnectService
) : MviViewModel<YapilyPermissionIntents,
    YapilyPermissionViewState,
    YapilyPermissionModelState,
    YapilyPermissionNavigationEvent,
    YapilyPermissionArgs>(
    initialState = YapilyPermissionModelState()
) {
    override fun viewCreated(args: YapilyPermissionArgs) {
    }

    override suspend fun handleIntent(modelState: YapilyPermissionModelState, intent: YapilyPermissionIntents) {
        when (intent) {
            is YapilyPermissionIntents.GetTermsOfServiceLink -> {
                getTermsOfServiceLink()
            }
        }.exhaustive
    }

    override fun reduce(state: YapilyPermissionModelState): YapilyPermissionViewState {
        return YapilyPermissionViewState(
            termsOfServiceLink = state.termsOfServiceLink
        )
    }

    private fun getTermsOfServiceLink() {
        viewModelScope.launch {
            safeConnectService.getTosLink()
                .also { termsOfServiceLink ->
                    updateState { it.copy(termsOfServiceLink = termsOfServiceLink) }
                }
        }
    }
}
