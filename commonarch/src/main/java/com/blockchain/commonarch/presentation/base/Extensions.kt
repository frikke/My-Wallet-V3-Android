package com.blockchain.commonarch.presentation.base

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.blockchain.commonarch.R
import com.blockchain.componentlib.navigation.NavigationBarButton

fun Fragment.updateToolbar(
    toolbarTitle: String = "",
    menuItems: List<NavigationBarButton>? = null,
    backAction: (() -> Unit)? = null
) {
    (activity as? BlockchainActivity)?.updateToolbar(toolbarTitle, menuItems, backAction)
}

fun Fragment.updateToolbarBackground(applyModeBackground: Boolean, mutedBackground: Boolean) {
    (activity as? BlockchainActivity)?.updateToolbarBackground(
        applyModeBackground = applyModeBackground,
        mutedBackground = mutedBackground
    )
}

fun Fragment.updateTitleToolbar(titleToolbar: String = "") {
    (activity as? BlockchainActivity)?.updateToolbarTitle(title = titleToolbar)
}

fun Fragment.updateToolbarBackAction(backAction: () -> Unit) {
    (activity as? BlockchainActivity)?.updateToolbarBackAction(backAction)
}

fun Fragment.updateToolbarMenuItems(menuItems: List<NavigationBarButton>) {
    (activity as? BlockchainActivity)?.updateToolbarMenuItems(menuItems)
}

fun FragmentTransaction.addTransactionAnimation(): FragmentTransaction =
    this.setCustomAnimations(
        R.anim.fragment_slide_left_enter,
        R.anim.fragment_slide_left_exit,
        R.anim.fragment_slide_right_enter,
        R.anim.fragment_slide_right_exit
    )
