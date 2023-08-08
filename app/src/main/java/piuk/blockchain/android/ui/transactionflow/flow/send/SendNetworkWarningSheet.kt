package piuk.blockchain.android.ui.transactionflow.flow.send

import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.commonarch.presentation.base.ThemedBottomSheetFragment
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.sheets.BottomSheetButton
import com.blockchain.componentlib.sheets.BottomSheetOneButton
import com.blockchain.componentlib.sheets.ButtonType
import java.lang.IllegalStateException
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.R

@Parcelize
data class SendNetworkWarningInfo(
    val currencyName: String,
    val network: String
) : Parcelable

class SendNetworkWarningSheet : ThemedBottomSheetFragment() {

    private val info: SendNetworkWarningInfo by lazy {
        arguments?.getParcelable<SendNetworkWarningInfo>(INFO) ?: throw IllegalStateException(
            "Missing Required Info"
        )
    }

    private lateinit var composeView: ComposeView

    interface Host {
        fun onSheetClosed()
    }

    val host: Host by lazy {
        parentFragment as? Host
            ?: throw IllegalStateException("Host fragment is not a SendNetworkWarningSheet.Host")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).also { composeView = it }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        host.onSheetClosed()
    }

    override fun dismiss() {
        super.dismiss()
        host.onSheetClosed()
    }

    private fun setupViews() {
        composeView.apply {
            setContent {
                SheetContent()
            }
        }
    }

    @Composable
    private fun SheetContent() {
        BottomSheetOneButton(
            onCloseClick = { dismiss() },
            title = stringResource(id = com.blockchain.stringResources.R.string.common_did_you_know),
            subtitle = stringResource(
                id = com.blockchain.stringResources.R.string.send_select_wallet_warning_sheet_desc,
                info.currencyName,
                info.network
            ),
            headerImageResource = ImageResource.None,
            button = BottomSheetButton(
                type = ButtonType.PRIMARY,
                onClick = { dismiss() },
                text = stringResource(id = com.blockchain.stringResources.R.string.common_ok)
            ),
        )
    }

    companion object {
        private const val INFO = "INFO"
        fun newInstance(currencyName: String, network: String): SendNetworkWarningSheet =
            SendNetworkWarningSheet().apply {
                arguments =
                    Bundle().apply {
                        putParcelable(INFO, SendNetworkWarningInfo(currencyName, network))
                    }
            }
    }

    @Preview
    @Composable
    private fun SheetContentPreview() {
        SheetContent()
    }
}
