package piuk.blockchain.android.ui.linkbank.yapily.permission

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.core.payments.model.YapilyInstitution
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.ui.linkbank.BankAuthSource

@Parcelize
data class YapilyPermissionArgs(
    val institution: YapilyInstitution,
    val entity: String,
    val authSource: BankAuthSource
) : ModelConfigArgs.ParcelableArgs {

    companion object {
        const val ARGS_KEY = "YapilyPermissionArgs"
    }
}
