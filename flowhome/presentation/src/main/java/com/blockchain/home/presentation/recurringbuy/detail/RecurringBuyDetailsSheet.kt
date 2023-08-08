package com.blockchain.home.presentation.recurringbuy.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.commonarch.presentation.base.ThemedBottomSheetFragment
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.home.presentation.recurringbuy.detail.composable.RecurringBuyDetail

class RecurringBuyDetailsSheet : ThemedBottomSheetFragment() {

    interface Host : SlidingModalBottomDialog.Host {
        fun onRecurringBuyDeleted()
    }

    private val host: Host by lazy {
        activity as? Host ?: throw IllegalStateException("Host fragment is not a RecurringBuyDetailsSheet.Host")
    }

    private val recurringBuyId: String by lazy {
        arguments?.getString(RECURRING_BUY_ID, "").orEmpty()
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
                        topStart = AppTheme.dimensions.smallSpacing,
                        topEnd = AppTheme.dimensions.smallSpacing
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
