package com.blockchain.commonarch.presentation.base

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.blockchain.commonarch.R
import com.blockchain.componentlib.navigation.ModeBackgroundColor
import com.blockchain.componentlib.navigation.NavigationBarButton

fun Fragment.updateToolbar(
    toolbarTitle: String = "",
    menuItems: List<NavigationBarButton>? = null,
    backAction: (() -> Unit)? = null
) {
    (activity as? BlockchainActivity)?.updateToolbar(
        toolbarTitle = toolbarTitle,
        menuItems = menuItems,
        backAction = backAction
    )
}

fun Fragment.updateToolbarBackground(
    modeColor: ModeBackgroundColor = ModeBackgroundColor.Current,
    mutedBackground: Boolean = true
) {
    (activity as? BlockchainActivity)?.updateToolbarBackground(
        modeColor = modeColor,
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
        com.blockchain.componentlib.R.anim.fragment_slide_left_enter,
        com.blockchain.componentlib.R.anim.fragment_slide_left_exit,
        com.blockchain.componentlib.R.anim.fragment_slide_right_enter,
        com.blockchain.componentlib.R.anim.fragment_slide_right_exit
    )
