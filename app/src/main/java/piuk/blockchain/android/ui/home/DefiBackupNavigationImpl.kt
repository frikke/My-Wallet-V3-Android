package piuk.blockchain.android.ui.home

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.presentation.backup.BackupPhraseActivity
import com.blockchain.presentation.navigation.DefiBackupNavigation

class DefiBackupNavigationImpl(
    private val activity: BlockchainActivity?,
) : DefiBackupNavigation {
    override fun startPhraseRecovery(
        launcher: ActivityResultLauncher<Intent>
    ) {
        activity?.let {
            launcher.launch(
                BackupPhraseActivity.newIntent(context = activity)
            )
        }
    }
}
