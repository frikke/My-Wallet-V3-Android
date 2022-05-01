package piuk.blockchain.android.ui.linkbank.presentation.yapily.permission

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.core.payments.model.YapilyInstitution

data class YapilyPermissionModelState(
    val termsOfServiceLink: String = "",
    val institution: YapilyInstitution? = null
) : ModelState
