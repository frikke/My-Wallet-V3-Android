package com.blockchain.home.presentation.onboarding.custodial

import androidx.lifecycle.ViewModel
import com.blockchain.preferences.WalletStatusPrefs

class CustodialIntroViewModel(
    private val walletStatusPrefs: WalletStatusPrefs
) : ViewModel() {
    fun markAsSeen() {
        walletStatusPrefs.hasSeenCustodialOnboarding = true
    }
}
