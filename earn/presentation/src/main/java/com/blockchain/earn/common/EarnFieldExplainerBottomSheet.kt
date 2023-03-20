package com.blockchain.earn.common

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import com.blockchain.commonarch.presentation.mvi_v2.forceExpanded
import com.blockchain.componentlib.sheets.BottomSheetButton
import com.blockchain.componentlib.sheets.BottomSheetOneButton
import com.blockchain.componentlib.sheets.ButtonType
import com.blockchain.earn.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class EarnFieldExplainerBottomSheet(private val earnField: EarnFieldExplainer) : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            setCanceledOnTouchOutside(false)
            forceExpanded()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BottomSheetOneButton(
                    onCloseClick = { dismiss() },
                    title = stringResource(id = earnField.titleResId),
                    subtitle = stringResource(id = earnField.descriptionResId),
                    shouldShowHeaderDivider = false,
                    button = BottomSheetButton(
                        type = ButtonType.PRIMARY,
                        text = stringResource(R.string.common_got_it),
                        onClick = { dismiss() }
                    ),
                    headerImageResource = null
                )
            }
        }
    }

    companion object {
        fun newInstance(earnField: EarnFieldExplainer) = EarnFieldExplainerBottomSheet(earnField)
    }
}
