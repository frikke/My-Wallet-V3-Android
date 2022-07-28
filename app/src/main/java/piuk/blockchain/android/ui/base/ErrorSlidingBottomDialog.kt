package piuk.blockchain.android.ui.base

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import com.blockchain.api.NabuApiException
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.domain.common.model.ServerErrorAction
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ErrorSlidingBottomDialogBinding
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.ACTION_UNKNOWN
import piuk.blockchain.android.util.loadRemoteErrorAndStatusIcons
import piuk.blockchain.android.util.loadRemoteErrorIcon
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

            errorSheetIndicator.image = ImageResource.Local(R.drawable.vector_sheet_indicator_small)

            loadRemoteIcons(
                title = errorDialogData.title,
                description = errorDialogData.description,
                iconUrl = errorDialogData.iconUrl.orEmpty(),
                statusIconUrl = errorDialogData.statusIconUrl.orEmpty()
            )

            primaryCtaButton.apply {
                text = errorDialogData.errorButtonCopies?.primaryButtonText?.ifEmpty { getString(R.string.common_ok) }
                    ?: getString(R.string.common_ok)
                onClick = {
                    dismiss()
                    host.onErrorPrimaryCta()
                }
                visible()
            }

            errorDialogData.errorButtonCopies?.secondaryButtonText?.let { secondaryButtonText ->
                if (secondaryButtonText.isNotEmpty()) {
                    secondaryCtaButton.apply {
                        text = secondaryButtonText
                        onClick = {
                            dismiss()
                            host.onErrorSecondaryCta()
                        }
                        visible()
                    }
                } else {
                    secondaryCtaButton.gone()
                }
            } ?: secondaryCtaButton.gone()

            errorDialogData.errorButtonCopies?.tertiaryButtonText?.let { tertiaryButtonText ->
                if (tertiaryButtonText.isNotEmpty()) {
                    tertiaryCtaButton.apply {
                        text = tertiaryButtonText
                        onClick = {
                            dismiss()
                            host.onErrorTertiaryCta()
                        }
                        visible()
                    }
                } else {
                    tertiaryCtaButton.gone()
                }
            } ?: tertiaryCtaButton.gone()
        }
        logClientError(
            title = errorDialogData.title,
            error = errorDialogData.error.orEmpty(),
            nabuApiException = errorDialogData.nabuApiException,
            action = errorDialogData.action,
            description = errorDialogData.description,
            analyticsCategories = errorDialogData.analyticsCategories
        )
    }

    private fun loadRemoteIcons(
        iconUrl: String,
        statusIconUrl: String = "", // not all server side errors will have a status icon
        title: String,
        description: String,
        @DrawableRes defaultErrorIcon: Int = R.drawable.ic_alert_white_bkgd,
    ) {
        when {
            // we have been provided both icon and status
            iconUrl.isNotEmpty() && statusIconUrl.isNotEmpty() -> {
                loadRemoteErrorAndStatusIcons(
                    iconUrl,
                    statusIconUrl,
                    title,
                    description,
                    defaultErrorIcon
                )
            }
            // we only have one icon
            iconUrl.isNotEmpty() && statusIconUrl.isEmpty() -> {
                loadRemoteErrorIcon(
                    iconUrl,
                    title,
                    description,
                    defaultErrorIcon
                )
            }
            // no icons provided
            else -> showDefaultErrorIcon(
                title = title,
                description = description,
                errorIcon = defaultErrorIcon
            )
        }
    }

    private fun loadRemoteErrorIcon(
        iconUrl: String,
        title: String,
        description: String,
        @DrawableRes defaultErrorIcon: Int,
    ) {
        requireContext().loadRemoteErrorIcon(
            iconUrl,
            onIconLoadSuccess = { drawable ->
                updateErrorIcon(
                    title = title,
                    subtitle = description,
                    icon = drawable
                )
            },
            onIconLoadError = {
                showDefaultErrorIcon(
                    title = title,
                    description = description,
                    errorIcon = defaultErrorIcon
                )
            }
        )
    }

    private fun updateErrorIcon(
        title: String,
        subtitle: CharSequence,
        icon: Drawable,
    ) {
        with(binding) {
            errorSheetIcon.image = ImageResource.LocalWithResolvedBitmap(icon.toBitmap())
            this.title.text = title
            description.text = subtitle
        }
    }

    private fun showDefaultErrorIcon(title: String, description: String, @DrawableRes errorIcon: Int) {
        with(binding) {
            this.title.text = title
            this.description.text = description
            this.errorSheetIcon.image = ImageResource.Local(errorIcon)
        }
    }

    private fun loadRemoteErrorAndStatusIcons(
        iconUrl: String,
        statusIconUrl: String,
        title: String,
        description: String,
        @DrawableRes defaultStatusIcon: Int,
    ) {
        requireContext().loadRemoteErrorAndStatusIcons(
            iconUrl,
            statusIconUrl,
            onIconLoadSuccess = { drawable ->
                updateErrorIcon(
                    title = title,
                    subtitle = description,
                    icon = drawable
                )
            },
            onIconLoadError = {
                showDefaultErrorIcon(
                    title = title,
                    description = description,
                    errorIcon = defaultStatusIcon
                )
            },
            onStatusIconLoadSuccess = { drawable ->
                updateStatusIcon(
                    title = title,
                    subtitle = description,
                    statusIcon = drawable
                )
            },
            onStatusIconLoadError = {
                binding.errorSheetStatus.gone()
                showDefaultErrorIcon(
                    title = title,
                    description = description,
                    errorIcon = defaultStatusIcon
                )
            }
        )
    }

    private fun updateStatusIcon(
        title: String,
        subtitle: CharSequence,
        statusIcon: Drawable,
    ) {
        with(binding) {
            errorSheetStatus.image = ImageResource.LocalWithResolvedBitmap(statusIcon.toBitmap())
            errorSheetStatus.visible()
            this.title.text = title
            description.text = subtitle
        }
    }

    private fun Drawable.toBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(
            intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)

        return bitmap
    }

    private fun logClientError(
        title: String,
        error: String,
        nabuApiException: NabuApiException?,
        description: String,
        action: String?,
        analyticsCategories: List<String>
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
                categories = analyticsCategories
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
    val analyticsCategories: List<String>,
    val iconUrl: String? = null,
    val statusIconUrl: String? = null,
    val errorId: String? = null
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
