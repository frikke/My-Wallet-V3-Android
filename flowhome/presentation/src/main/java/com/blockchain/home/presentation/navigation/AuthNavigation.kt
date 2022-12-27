package com.blockchain.home.presentation.navigation

import android.os.Bundle
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

interface AuthNavigation {
    fun launchAuth(bundle: Bundle)
    fun logout()
}

interface AuthNavigationHost : SlidingModalBottomDialog.Host {
    fun navigateToBottomSheet(bottomSheet: BottomSheetDialogFragment)
}

interface AccountWalletLinkAlertSheetHost : SlidingModalBottomDialog.Host {
    fun logout()
}
