package piuk.blockchain.android.ui.dashboard.coinview.recurringbuy

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.commonarch.presentation.mvi_v2.forceExpanded
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.home.presentation.recurringbuy.detail.composable.RecurringBuyDetail
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class RecurringBuyDetailsSheet : BottomSheetDialogFragment() {

    interface Host : SlidingModalBottomDialog.Host {
        fun onRecurringBuyDeleted()
    }

    private val host: Host by lazy {
        activity as? Host ?: throw IllegalStateException("Host fragment is not a RecurringBuyDetailsSheet.Host")
    }

    private val recurringBuyId: String by lazy {
        arguments?.getString(RECURRING_BUY_ID, "").orEmpty()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            forceExpanded()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = AppTheme.dimensions.smallSpacing, topEnd = AppTheme.dimensions.smallSpacing
                    )
                ) {
                    RecurringBuyDetail(
                        recurringBuyId = recurringBuyId,
                        onCloseClick = {
                            host.onRecurringBuyDeleted()
                            dismiss()
                        }
                    )
                }
            }
        }
    }

    companion object {
        private const val RECURRING_BUY_ID = "RECURRING_BUY_ID"
        fun newInstance(recurringBuyId: String): RecurringBuyDetailsSheet = RecurringBuyDetailsSheet().apply {
            arguments = Bundle().apply {
                putString(RECURRING_BUY_ID, recurringBuyId)
            }
        }
    }
}
