package piuk.blockchain.android.ui.linkbank.yapily.permission

import android.os.Parcelable
import com.blockchain.core.payments.model.YapilyInstitution
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.ui.linkbank.BankAuthSource

@Parcelize
data class YapilyPermissionArgs(
    val institution: YapilyInstitution,
    val entity: String,
    val authSource: BankAuthSource
) : Parcelable {

    companion object {
        const val ARGS_KEY = "YapilyPermissionArgs"
    }
}