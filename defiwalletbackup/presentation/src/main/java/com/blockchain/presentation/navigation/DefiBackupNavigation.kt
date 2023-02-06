package com.blockchain.presentation.navigation

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Stable

@Stable
interface DefiBackupNavigation {
    fun startBackup(
        launcher: ActivityResultLauncher<Intent>,
        onboardingRequired: Boolean
    )
}
