package com.blockchain.home.presentation.onboarding.introduction

import androidx.lifecycle.ViewModel
import com.blockchain.preferences.SuperAppMvpPrefs

class IntroScreensViewModel(
    private val educationalScreensPrefs: SuperAppMvpPrefs
) : ViewModel() {
    fun markAsSeen() {
        educationalScreensPrefs.hasSeenEducationalWalletMode = true
    }
}
