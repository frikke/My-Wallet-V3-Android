package piuk.blockchain.android.ui.linkbank.yapily.permission

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface YapilyPermissionIntents : Intent<YapilyPermissionModelState> {
    data class DownloadTermsOfService(val absolutePath: String) : YapilyPermissionIntents
}
