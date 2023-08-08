package com.blockchain.transactions.receive.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import com.blockchain.commonarch.presentation.base.ThemedBottomSheetFragment
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.topOnly
import com.blockchain.transactions.receive.detail.composable.ReceiveAccountDetail

class ReceiveAccountDetailFragment private constructor() : ThemedBottomSheetFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                Surface(
                    shape = AppTheme.shapes.veryLarge.topOnly()
                ) {
                    ReceiveAccountDetail(
                        onBackPressed = ::dismiss
                    )
                }
            }
        }
    }

    companion object {
        fun newInstance() = ReceiveAccountDetailFragment()
    }
}
