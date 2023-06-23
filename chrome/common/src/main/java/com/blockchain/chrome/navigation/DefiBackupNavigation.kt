package com.blockchain.chrome.navigation

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf

@Stable
interface DefiBackupNavigation {
    fun startPhraseRecovery(
        launcher: ActivityResultLauncher<Intent>
    )
}

val LocalDefiBackupNavigationProvider = staticCompositionLocalOf<DefiBackupNavigation> {
    error("not provided")
}
