package piuk.blockchain.android.ui.home

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.presentation.backup.BackupPhraseActivity
import com.blockchain.presentation.navigation.DefiBackupNavigation
import com.blockchain.presentation.onboarding.DeFiOnboardingActivity

class DefiBackupNavigationImpl(
    private val activity: BlockchainActivity?,
) : DefiBackupNavigation {
    override fun startBackup(
        launcher: ActivityResultLauncher<Intent>,
        onboardingRequired: Boolean
    ) {
        activity?.let {
            launcher.launch(
                if (onboardingRequired) {
                    DeFiOnboardingActivity.newIntent(context = activity)
                } else {
                    BackupPhraseActivity.newIntent(context = activity)
                }
            )
        }
    }
}
