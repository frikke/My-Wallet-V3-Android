package com.blockchain.home.presentation.onboarding.defi

import androidx.lifecycle.ViewModel
import com.blockchain.preferences.WalletStatusPrefs

class DefiIntroViewModel(
    private val walletStatusPrefs: WalletStatusPrefs
) : ViewModel() {
    fun markAsSeen() {
        walletStatusPrefs.hasSeenDefiOnboarding = true
    }
}
