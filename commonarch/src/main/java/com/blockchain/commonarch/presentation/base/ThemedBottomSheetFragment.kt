package com.blockchain.commonarch.presentation.base

import android.app.Dialog
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import androidx.core.content.res.ResourcesCompat
import com.blockchain.commonarch.presentation.mvi_v2.forceExpanded
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

abstract class ThemedBottomSheetFragment(
    private val cancelableOnTouchOutside: Boolean = true,
    private val forceExpanded: Boolean = true
) : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireActivity()).apply {

            // animate background screen scrim color because otherwise it looks like
            // a solid sheet going up with the content
            // needed for dark mode especially since sheet and screen bg are both black so now bg is tinted
            val transition: TransitionDrawable = ResourcesCompat.getDrawable(
                resources, com.blockchain.componentlib.R.drawable.scrimanim, null
            ) as TransitionDrawable
            window?.setBackgroundDrawable(transition)
            transition.startTransition(250)

            setCanceledOnTouchOutside(cancelableOnTouchOutside)
            if (forceExpanded) forceExpanded()
        }
    }
}
