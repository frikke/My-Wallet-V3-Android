package piuk.blockchain.android.ui.linkbank.yapily.permission

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.extensions.exhaustive
import com.blockchain.outcome.doOnSuccess
import kotlinx.coroutines.launch
import piuk.blockchain.android.fileutils.domain.usecase.DownloadFileUseCase

class YapilyPermissionViewModel(
    private val safeConnectRemoteConfig: SafeConnectRemoteConfig,
    private val downloadFileUseCase: DownloadFileUseCase
) : MviViewModel<YapilyPermissionIntents,
    YapilyPermissionViewState,
    YapilyPermissionModelState,
    YapilyPermissionNavigationEvent,
    YapilyPermissionArgs>(
    initialState = YapilyPermissionModelState
) {

    override fun viewCreated(args: YapilyPermissionArgs) {
    }

    override suspend fun handleIntent(modelState: YapilyPermissionModelState, intent: YapilyPermissionIntents) {
        when (intent) {
            is YapilyPermissionIntents.DownloadTermsOfService -> {
                downloadTermsOfService(intent.absolutePath)
            }
        }.exhaustive
    }

    override fun reduce(state: YapilyPermissionModelState): YapilyPermissionViewState {
        return YapilyPermissionViewState
    }

    private fun downloadTermsOfService(absolutePath: String) {
        viewModelScope.launch {
            downloadFileUseCase(absolutePath = absolutePath, safeConnectRemoteConfig.getTosPdfLink()).doOnSuccess {
                navigate(YapilyPermissionNavigationEvent.OpenFile(it))
            }
        }
    }
}
