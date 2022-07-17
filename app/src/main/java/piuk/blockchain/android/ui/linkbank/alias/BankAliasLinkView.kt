package piuk.blockchain.android.ui.linkbank.alias

import com.blockchain.api.NabuApiException
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.domain.common.model.ServerSideUxErrorInfo
import com.blockchain.domain.paymentmethods.model.AliasInfo

data class BankAliasLinkViewState(
    val showAliasInput: Boolean = true,
    val ctaState: ButtonState = ButtonState.Disabled,
    val alias: String? = null,
    val aliasInfo: AliasInfo? = null,
    val error: AliasError? = null
) : ViewState

sealed class AliasError {
    object InternetConnectionError : AliasError()
    class UnhandledHttpError(val nabuApiException: NabuApiException) : AliasError()
    class ServerSideUxError(val serverSideUxErrorInfo: ServerSideUxErrorInfo) : AliasError()
    class GeneralError(val exception: Exception) : AliasError()
}
