package piuk.blockchain.android.ui.linkbank.presentation.yapily.permission

import com.blockchain.commonarch.presentation.mvi_v2.ModelState

data class YapilyPermissionModelState(
    val termsOfServiceLink: String = ""
) : ModelState
