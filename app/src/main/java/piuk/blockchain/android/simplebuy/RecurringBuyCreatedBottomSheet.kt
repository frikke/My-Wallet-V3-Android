package piuk.blockchain.android.simplebuy

import android.os.Bundle
import androidx.compose.runtime.Composable
import com.blockchain.commonarch.presentation.base.ComposeModalBottomDialog
import com.blockchain.commonarch.presentation.base.HostedBottomSheet
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.sheets.BottomSheetButton
import com.blockchain.componentlib.sheets.BottomSheetTwoButtons
import com.blockchain.componentlib.sheets.ButtonType
import piuk.blockchain.android.R

class RecurringBuyCreatedBottomSheet : ComposeModalBottomDialog() {

    interface Host : HostedBottomSheet.Host {
        fun viewRecurringBuy(recurringBuyId: String)
        fun skip()
    }

    override val host: Host by lazy {
        activity as? Host ?: parentFragment as? Host ?: throw IllegalStateException(
            "Host is not a RecurringBuyCreatedBottomSheet.Host"
        )
    }

    private val title: String by lazy {
        arguments?.getString(RB_TITLE).orEmpty()
    }

    private val subtitle: String by lazy {
        arguments?.getString(RB_SUBTITLE).orEmpty()
    }

    private val recurringBuyId: String by lazy {
        arguments?.getString(RECURRING_BUY_ID).orEmpty()
    }

    override val makeSheetNonCollapsible: Boolean
        get() = true

    @Composable
    override fun Sheet() {
        BottomSheetTwoButtons(
            headerImageResource = ImageResource.Local(com.blockchain.common.R.drawable.ic_tx_recurring_buy),
            title = title,
            subtitle = subtitle,
            onCloseClick = {},
            button1 = BottomSheetButton(
                type = ButtonType.MINIMAL,
                text = getString(com.blockchain.stringResources.R.string.recurring_buy_view_rb),
                onClick = { host.viewRecurringBuy(recurringBuyId) }
            ),
            button2 = BottomSheetButton(
                type = ButtonType.PRIMARY,
                text = getString(com.blockchain.stringResources.R.string.common_continue),
                onClick = { host.skip() }
            )
        )
    }

    companion object {
        private const val RB_TITLE = "RB_TITLE"
        private const val RB_SUBTITLE = "RB_SUBTITLE"
        private const val RECURRING_BUY_ID = "RECURRING_BUY_ID"

        fun newInstance(
            title: String,
            subtitle: String,
            recurringBuyId: String
        ): RecurringBuyCreatedBottomSheet =
            RecurringBuyCreatedBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(RB_TITLE, title)
                    putString(RB_SUBTITLE, subtitle)
                    putString(RECURRING_BUY_ID, recurringBuyId)
                }
            }
    }
}
