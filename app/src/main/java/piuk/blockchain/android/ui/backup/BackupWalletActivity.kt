package piuk.blockchain.android.ui.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.utils.consume
import org.koin.android.ext.android.get
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityBackupWalletBinding
import piuk.blockchain.android.ui.backup.completed.BackupWalletCompletedFragment
import piuk.blockchain.android.ui.backup.start.BackupWalletStartingFragment

// todo(othman) setup BackupPhraseActivity in portfolio announcement card
@Deprecated("this is a legacy class, replaced by BackupPhraseActivity")
class BackupWalletActivity : BlockchainActivity() {

    private val payloadManger: PayloadDataManager by scopedInject()

    override val alwaysDisableScreenshots: Boolean
        get() = true

    private val binding: ActivityBackupWalletBinding by lazy {
        ActivityBackupWalletBinding.inflate(layoutInflater)
    }

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupBackPress()

        get<Analytics>().logEvent(AnalyticsEvents.Backup)
        updateToolbar(
            toolbarTitle = getString(R.string.backup_wallet_title),
            backAction = { onSupportNavigateUp() }
        )
        if (isBackedUp()) {
            startFragment(
                BackupWalletCompletedFragment.newInstance(),
                BackupWalletCompletedFragment.TAG
            )
        } else {
            startFragment(BackupWalletStartingFragment(), BackupWalletStartingFragment.TAG)
        }
    }

    private fun startFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, fragment)
            .addToBackStack(tag)
            .commit()
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(owner = this) {
            if (supportFragmentManager.backStackEntryCount <= 1) {
                setResult(if (isBackedUp()) RESULT_OK else Activity.RESULT_CANCELED)
                finish()
            } else {
                supportFragmentManager.popBackStack()
            }
        }
    }

    override fun onSupportNavigateUp() =
        consume { onBackPressedDispatcher.onBackPressed() }

    private fun isBackedUp() = payloadManger.isBackedUp

    companion object {
        fun start(context: Context) {
            val starter = Intent(context, BackupWalletActivity::class.java)
            context.startActivity(starter)
        }

        fun startForResult(fragment: Fragment, requestCode: Int) {
            fragment.startActivityForResult(
                Intent(fragment.context, BackupWalletActivity::class.java),
                requestCode
            )
        }

        fun newIntent(context: Context): Intent = Intent(context, BackupWalletActivity::class.java)
    }
}
