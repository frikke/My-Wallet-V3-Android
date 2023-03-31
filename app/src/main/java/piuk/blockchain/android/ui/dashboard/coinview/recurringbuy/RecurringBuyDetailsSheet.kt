package piuk.blockchain.android.ui.dashboard.coinview.recurringbuy

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.commonarch.presentation.mvi.MviBottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.forceExpanded
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.core.recurringbuy.domain.model.RecurringBuy
import com.blockchain.core.recurringbuy.domain.model.RecurringBuyState
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.home.presentation.recurringbuy.detail.composable.RecurringBuyDetail
import com.blockchain.presentation.customviews.BlockchainListDividerDecor
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.utils.toFormattedDateWithoutYear
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import info.blockchain.balance.AssetInfo
import java.time.ZoneId
import java.time.ZonedDateTime
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogSheetRecurringBuyInfoBinding
import piuk.blockchain.android.databinding.MobileNoticeDialogBinding
import piuk.blockchain.android.simplebuy.CheckoutAdapterDelegate
import piuk.blockchain.android.simplebuy.SimpleBuyCheckoutItem
import piuk.blockchain.android.simplebuy.toHumanReadableRecurringBuy
import piuk.blockchain.android.simplebuy.toHumanReadableRecurringDate
import piuk.blockchain.android.ui.home.MobileNoticeDialogFragment

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
