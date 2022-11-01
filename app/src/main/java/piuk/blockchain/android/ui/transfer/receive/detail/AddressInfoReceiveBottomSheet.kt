package piuk.blockchain.android.ui.transfer.receive.detail

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import com.blockchain.commonarch.presentation.base.ComposeModalBottomDialog
import com.blockchain.componentlib.sheets.BottomSheetButton
import com.blockchain.componentlib.sheets.BottomSheetOneButton
import com.blockchain.componentlib.sheets.ButtonType
import piuk.blockchain.android.R

class AddressInfoReceiveBottomSheet : ComposeModalBottomDialog() {

    override val host: Host by lazy {
        activity as? Host ?: parentFragment as? Host ?: throw IllegalStateException(
            "Host is not a AddressInfoReceiveBottomSheet.Host"
        )
    }

    val displayTicker: String
        get() = arguments?.getString(TICKER).orEmpty()

    val label: String?
        get() = arguments?.getString(LABEL).orEmpty()

    override val makeSheetNonCollapsible: Boolean
        get() = false

    @Composable
    override fun Sheet() {
        BottomSheetOneButton(
            onCloseClick = { dismiss() },
            title = getString(R.string.common_did_you_know),
            headerImageResource = null,
            shouldShowHeaderDivider = false,
            subtitleAlign = TextAlign.Left,
            showTitleInHeader = true,
            subtitle = getString(R.string.receive_rotating_address_desc, displayTicker, label),
            button = BottomSheetButton(
                type = ButtonType.PRIMARY,
                onClick = {
                    dismiss()
                }, text = getString(R.string.common_ok)
            )
        )
    }

    companion object {
        private const val TICKER = "DISPLAY_TICKER"
        private const val LABEL = "LABEL"

        fun newInstance(
            displayTicker: String,
            label: String
        ): AddressInfoReceiveBottomSheet =
            AddressInfoReceiveBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(TICKER, displayTicker)
                    putString(LABEL, label)
                }
            }
    }
}
