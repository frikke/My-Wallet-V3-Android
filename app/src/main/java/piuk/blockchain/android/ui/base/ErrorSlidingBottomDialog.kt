package piuk.blockchain.android.ui.base

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.api.NabuApiException
import com.blockchain.api.ServerErrorAction
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.databinding.ErrorSlidingBottomDialogBinding
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.ACTION_UNKNOWN
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class ErrorSlidingBottomDialog : SlidingModalBottomDialog<ErrorSlidingBottomDialogBinding>() {

    interface Host : SlidingModalBottomDialog.Host {
        fun onErrorPrimaryCta()
        fun onErrorSecondaryCta()
        fun onErrorTertiaryCta()
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a ErrorSlidingBottomDialog.Host"
        )
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): ErrorSlidingBottomDialogBinding =
        ErrorSlidingBottomDialogBinding.inflate(inflater, container, false)

    private val errorDialogData: ErrorDialogData by unsafeLazy {
        arguments?.getParcelable(ERROR_DIALOG_DATA_KEY) as? ErrorDialogData
            ?: throw IllegalStateException("No Dialog date provided")
    }

    override fun initControls(binding: ErrorSlidingBottomDialogBinding) {
        with(binding) {
            title.text = errorDialogData.title
            description.text = errorDialogData.description
            errorDialogData.errorButtonCopies?.primaryButtonText?.let { primaryButtonText ->
                primaryCtaButton.apply {
                    text = primaryButtonText
                    onClick = {
                        dismiss()
                        host.onErrorPrimaryCta()
                    }
                    visible()
                }
            } ?: primaryCtaButton.gone()
            errorDialogData.errorButtonCopies?.secondaryButtonText?.let { secondaryButtonText ->
                secondaryCtaButton.apply {
                    text = secondaryButtonText
                    onClick = {
                        dismiss()
                        host.onErrorSecondaryCta()
                    }
                    visible()
                }
            } ?: secondaryCtaButton.gone()
            errorDialogData.errorButtonCopies?.tertiaryButtonText?.let { tertiaryButtonText ->
                tertiaryCtaButton.apply {
                    text = tertiaryButtonText
                    onClick = {
                        dismiss()
                        host.onErrorTertiaryCta()
                    }
                    visible()
                }
            } ?: tertiaryCtaButton.gone()
        }
        logClientError(
            title = errorDialogData.title,
            error = errorDialogData.error.orEmpty(),
            nabuApiException = errorDialogData.nabuApiException,
            action = errorDialogData.action,
            description = errorDialogData.description
        )
    }

    private fun logClientError(
        title: String,
        error: String,
        nabuApiException: NabuApiException?,
        description: String,
        action: String?,
    ) {
        analytics.logEvent(
            ClientErrorAnalytics.ClientLogError(
                nabuApiException = nabuApiException,
                errorDescription = description,
                error = error,
                source = nabuApiException?.getErrorCode()?.let { ClientErrorAnalytics.Companion.Source.NABU }
                    ?: ClientErrorAnalytics.Companion.Source.CLIENT,
                title = title,
                action = action,
            )
        )
    }

    companion object {
        private const val ERROR_DIALOG_DATA_KEY = "ERROR_DIALOG_DATA_KEY"
        fun newInstance(errorDialogData: ErrorDialogData): ErrorSlidingBottomDialog =
            ErrorSlidingBottomDialog().apply {
                arguments = Bundle().apply { putParcelable(ERROR_DIALOG_DATA_KEY, errorDialogData) }
            }
    }
}

@Parcelize
data class ErrorDialogData(
    val title: String,
    val description: String,
    val error: String? = null,
    val nabuApiException: NabuApiException? = null,
    val errorDescription: String? = null,
    val action: String? = ACTION_UNKNOWN,
    val errorButtonCopies: ErrorButtonCopies?,
) : Parcelable

fun List<ServerErrorAction>.mapToErrorCopies(): ErrorButtonCopies {
    var buttonCopies = ErrorButtonCopies()
    mapIndexed { index, info ->
        when (index) {
            0 -> buttonCopies = buttonCopies.copy(primaryButtonText = info.title)
            1 -> buttonCopies = buttonCopies.copy(secondaryButtonText = info.title)
            2 -> buttonCopies = buttonCopies.copy(tertiaryButtonText = info.title)
            else -> {
                // do nothing, only support 3 error types
            }
        }
    }
    return buttonCopies
}

@Parcelize
data class ErrorButtonCopies(
    val primaryButtonText: String? = null,
    val secondaryButtonText: String? = null,
    val tertiaryButtonText: String? = null,
) : Parcelable
