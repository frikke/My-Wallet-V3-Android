package com.blockchain.earn.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import com.blockchain.commonarch.presentation.base.ThemedBottomSheetFragment
import com.blockchain.componentlib.sheets.BottomSheetButton
import com.blockchain.componentlib.sheets.BottomSheetOneButton
import com.blockchain.componentlib.sheets.ButtonType

class EarnFieldExplainerBottomSheet(private val earnField: EarnFieldExplainer) : ThemedBottomSheetFragment(
    cancelableOnTouchOutside = false
) {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BottomSheetOneButton(
                    onCloseClick = { dismiss() },
                    title = stringResource(id = earnField.titleResId),
                    subtitle = stringResource(id = earnField.descriptionResId),
                    button = BottomSheetButton(
                        type = ButtonType.PRIMARY,
                        text = stringResource(com.blockchain.stringResources.R.string.common_got_it),
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
