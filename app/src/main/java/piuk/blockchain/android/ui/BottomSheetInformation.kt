package piuk.blockchain.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.compose.ui.platform.ComposeView
import com.blockchain.commonarch.presentation.base.HostedBottomSheet
import com.blockchain.commonarch.presentation.base.ThemedBottomSheetFragment
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.sheets.BottomSheetButton
import com.blockchain.componentlib.sheets.BottomSheetOneButton
import com.blockchain.componentlib.sheets.BottomSheetTwoButtons
import com.blockchain.componentlib.sheets.ButtonType
import piuk.blockchain.android.R

class BottomSheetInformation : ThemedBottomSheetFragment() {

    interface Host : HostedBottomSheet.Host {
        fun primaryButtonClicked()
        fun secondButtonClicked()
    }

    val host: Host by lazy {
        activity as? Host ?: parentFragment as? Host ?: throw IllegalStateException(
            "Host is not a BottomSheetInformation.Host"
        )
    }

    private val title by lazy { arguments?.getString(TITLE).orEmpty() }
    private val description by lazy { arguments?.getString(DESCRIPTION).orEmpty() }
    private val primaryCtaText by lazy { arguments?.getString(CTA_TEXT_PRIMARY).orEmpty() }
    private val secondaryCtaText by lazy { arguments?.getString(CTA_TEXT_SECONDARY) }
    private val icon by lazy { arguments?.getInt(SHEET_ICON, -1)?.takeIf { it != -1 } }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                val secondaryCtaText = secondaryCtaText
                if (secondaryCtaText == null) {
                    BottomSheetOneButton(
                        title = title,
                        subtitle = description,
                        onCloseClick = { dismiss() },
                        headerImageResource = icon?.let {
                            ImageResource.Local(it)
                        } ?: ImageResource.None,
                        button = BottomSheetButton(
                            type = ButtonType.PRIMARY,
                            text = primaryCtaText,
                            onClick = {
                                host.primaryButtonClicked()
                                super.dismiss()
                            }
                        )
                    )
                } else {
                    BottomSheetTwoButtons(
                        title = title,
                        subtitle = description,
                        onCloseClick = { dismiss() },
                        headerImageResource = icon?.let {
                            ImageResource.Local(it)
                        } ?: ImageResource.None,
                        button1 = BottomSheetButton(
                            type = ButtonType.PRIMARY,
                            text = primaryCtaText,
                            onClick = {
                                host.primaryButtonClicked()
                                super.dismiss()
                            }
                        ),
                        button2 = BottomSheetButton(
                            type = ButtonType.MINIMAL,
                            text = secondaryCtaText,
                            onClick = {
                                host.secondButtonClicked()
                                super.dismiss()
                            }
                        )
                    )
                }
            }
        }
    }

    companion object {
        private const val TITLE = "title"
        private const val DESCRIPTION = "description"
        private const val CTA_TEXT_PRIMARY = "primary_cta_text"
        private const val CTA_TEXT_SECONDARY = "secondary_cta_text"
        private const val SHEET_ICON = "sheet_icon"

        fun newInstance(
            title: String,
            description: String,
            primaryCtaText: String,
            secondaryCtaText: String? = null,
            @DrawableRes icon: Int? = R.drawable.ic_phone
        ): BottomSheetInformation {
            return BottomSheetInformation().apply {
                arguments = Bundle().apply {
                    putString(TITLE, title)
                    putString(DESCRIPTION, description)
                    putString(CTA_TEXT_PRIMARY, primaryCtaText)
                    putString(CTA_TEXT_SECONDARY, secondaryCtaText)
                    icon?.let {
                        putInt(SHEET_ICON, it)
                    }
                }
            }
        }
    }
}
