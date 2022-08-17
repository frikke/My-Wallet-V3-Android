package piuk.blockchain.android.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.blockchain.commonarch.presentation.base.ComposeModalBottomDialog
import com.blockchain.componentlib.sheets.SheetHeader
import piuk.blockchain.android.R

class DefiActionsBottomSheet : ComposeModalBottomDialog() {
    companion object {
        fun newInstance() = DefiActionsBottomSheet()
    }

    override val host: ActionBottomSheetHost by lazy {
        super.host as? ActionBottomSheetHost ?: throw IllegalStateException(
            "Host fragment is not a ActionBottomSheetHost.Host"
        )
    }

    @Composable
    override fun Sheet() {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SheetHeader(
                title = stringResource(id = R.string.shortcuts),
                onClosePress = { dismiss() },
                shouldShowDivider = true
            )
            ActionRows(
                listOf(
                    DefiAction(
                        icon = R.drawable.ic_defi_swap,
                        title = stringResource(id = R.string.common_swap),
                        subtitle = stringResource(id = R.string.exchange_your_crypto),
                        onClick = { host.launchSwapScreen(); dismiss() }
                    ),
                    DefiAction(
                        icon = R.drawable.ic_defi_send,
                        title = stringResource(id = R.string.common_send),
                        subtitle = stringResource(id = R.string.transfer_to_another_wallet),
                        onClick = { host.launchSend(); dismiss() }
                    ),
                    DefiAction(
                        icon = R.drawable.ic_defi_receive,
                        title = stringResource(id = R.string.common_receive),
                        subtitle = stringResource(id = R.string.receive_to_your_wallet),
                        onClick = { host.launchReceive(); dismiss() }
                    )
                )
            )
            DefiBuyCrypto(onClick = { host.launchDefiBuy(); dismiss() })
        }
    }
}

data class DefiAction(
    val icon: Int,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)
