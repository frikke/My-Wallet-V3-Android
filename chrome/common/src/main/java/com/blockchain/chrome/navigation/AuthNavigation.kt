package com.blockchain.chrome.navigation

import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.domain.auth.SecureChannelLoginData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

interface AuthNavigation {
    fun launchAuth(data: SecureChannelLoginData)
    fun logout()
}

interface AuthNavigationHost : SlidingModalBottomDialog.Host {
    fun navigateToBottomSheet(bottomSheet: BottomSheetDialogFragment)
}

interface AccountWalletLinkAlertSheetHost : SlidingModalBottomDialog.Host {
    fun logout()
}
