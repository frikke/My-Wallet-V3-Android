package piuk.blockchain.android.ui.base

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.api.NabuApiException
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import java.lang.IllegalStateException
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.databinding.ErrorSlidingBottomDialogBinding
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.ACTION_UNKNOWN
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class ErrorSlidingBottomDialog : SlidingModalBottomDialog<ErrorSlidingBottomDialogBinding>() {

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): ErrorSlidingBottomDialogBinding =
        ErrorSlidingBottomDialogBinding.inflate(inflater, container, false)

    private val errorDialogData: ErrorDialogData by unsafeLazy {
        arguments?.getParcelable(ERROR_DIALOG_DATA_KEY) as? ErrorDialogData
            ?: throw IllegalStateException("No Dialog date provided")
    }

    override fun initControls(binding: ErrorSlidingBottomDialogBinding) {
        binding.title.text = errorDialogData.title
        binding.description.text = errorDialogData.description
        binding.ctaButton.text = errorDialogData.buttonText

        binding.ctaButton.setOnClickListener {
            dismiss()
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
        action: String?
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
    val buttonText: String,
    val error: String? = null,
    val nabuApiException: NabuApiException? = null,
    val errorDescription: String? = null,
    val action: String? = ACTION_UNKNOWN
) : Parcelable
