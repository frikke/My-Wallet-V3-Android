package piuk.blockchain.android.ui.linkbank.presentation.openbanking.permission

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.domain.paymentmethods.model.YapilyInstitution
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.ui.linkbank.BankAuthSource

@Parcelize
data class OpenBankingPermissionArgs(
    val institution: YapilyInstitution,
    val entity: String,
    val authSource: BankAuthSource
) : ModelConfigArgs.ParcelableArgs {

    companion object {
        const val ARGS_KEY = "OpenBankingPermissionArgs"
    }
}
