package piuk.blockchain.android.ui.linkbank.presentation.yapily.permission

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface YapilyPermissionIntents : Intent<YapilyPermissionModelState> {
    object GetTermsOfServiceLink : YapilyPermissionIntents
}
