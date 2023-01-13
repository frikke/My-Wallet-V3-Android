package piuk.blockchain.android.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import com.blockchain.commonarch.presentation.base.ComposeModalBottomDialog
import com.blockchain.commonarch.presentation.base.HostedBottomSheet
import com.blockchain.componentlib.sheets.BottomSheetButton
import com.blockchain.componentlib.sheets.BottomSheetOneButton
import com.blockchain.componentlib.sheets.ButtonType
import piuk.blockchain.android.R

class BuyDefiBottomSheet : ComposeModalBottomDialog() {
    interface Host : HostedBottomSheet.Host {
        fun goToTrading()
    }

    companion object {
        fun newInstance() = BuyDefiBottomSheet()
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a ActionBottomSheetHost.Host"
        )
    }

    @Composable
    override fun Sheet() {
        BottomSheetOneButton(
            title = getString(R.string.buy_crypto),
            showTitleInHeader = true,
            subtitle = "",
            shouldShowHeaderDivider = false,
            onCloseClick = { dismiss() },
            subtitleAlign = TextAlign.Start,
            headerImageResource = null,
            button = BottomSheetButton(
                type = ButtonType.PRIMARY,
                text = "",
                onClick = {
                    host.goToTrading()
                    dismiss()
                }
            )
        )
    }
}
