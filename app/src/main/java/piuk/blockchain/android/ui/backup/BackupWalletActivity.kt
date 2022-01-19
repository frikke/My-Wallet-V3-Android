package piuk.blockchain.android.ui.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.blockchain.commonarch.databinding.ToolbarGeneralBinding
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import org.koin.android.ext.android.get
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityBackupWalletBinding
import piuk.blockchain.android.ui.backup.completed.BackupWalletCompletedFragment
import piuk.blockchain.android.ui.backup.start.BackupWalletStartingFragment
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.helperfunctions.consume

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

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount <= 1) {
            setResult(if (isBackedUp()) RESULT_OK else Activity.RESULT_CANCELED)
            finish()
        } else {
            supportFragmentManager.popBackStack()
        }
    }

    override fun onSupportNavigateUp() =
        consume { onBackPressed() }

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
    }
}
