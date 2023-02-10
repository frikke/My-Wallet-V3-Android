package com.blockchain.home.presentation.defionboarding

import androidx.lifecycle.ViewModel
import com.blockchain.preferences.WalletStatusPrefs

class DeFiOnboardingViewModel(
    private val walletStatusPrefs: WalletStatusPrefs
) : ViewModel() {
    fun markAsSeen() {
        walletStatusPrefs.hasSeenDefiOnboarding = true
    }
}
