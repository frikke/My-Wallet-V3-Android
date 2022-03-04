package piuk.blockchain.android.ui.dashboard.sheets

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.SingleAccount
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.preferences.DashboardPrefs
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogBackupForSendBinding
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics

class ForceBackupForSendSheet : SlidingModalBottomDialog<DialogBackupForSendBinding>() {

    interface Host : SlidingModalBottomDialog.Host {
        fun startBackupForTransfer()
        fun startTransferFunds(account: SingleAccount, action: AssetAction)
    }

    private lateinit var account: SingleAccount
    private lateinit var action: AssetAction

    private val dashboardPrefs: DashboardPrefs by inject()

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException("Host fragment is not a ForceBackupForSendSheet.Host")
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogBackupForSendBinding =
        DialogBackupForSendBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogBackupForSendBinding) {
        analytics.logEvent(SimpleBuyAnalytics.BACK_UP_YOUR_WALLET_SHOWN)

        binding.ctaButton.setOnClickListener { onCtaClick() }

        binding.ctaLater.setOnClickListener {
            checkAttemptsAndNavigate()
        }

        val remainingSendsWithoutBackup = dashboardPrefs.remainingSendsWithoutBackup

        if (remainingSendsWithoutBackup == 0) {
            binding.ctaLater.isEnabled = false
            binding.backupSendsLabel.text = getString(R.string.backup_before_send_now_label)
        } else {
            binding.ctaLater.isEnabled = true
            binding.backupSendsLabel.text =
                resources.getQuantityString(
                    R.plurals.backup_before_send_later_label, remainingSendsWithoutBackup,
                    remainingSendsWithoutBackup
                )
        }
    }

    private fun checkAttemptsAndNavigate() {
        if (dashboardPrefs.remainingSendsWithoutBackup > 0) {
            host.startTransferFunds(account, action)
        }
        dismiss()
    }

    override fun onSheetHidden() {
        super.onSheetHidden()
        dialog?.let {
            onCancel(it)
        }
    }

    private fun onCtaClick() {
        analytics.logEvent(SimpleBuyAnalytics.BACK_UP_YOUR_WALLET_CLICKED)
        dismiss()
        host.startBackupForTransfer()
    }

    companion object {
        fun newInstance(backupDetails: BackupDetails): ForceBackupForSendSheet =
            ForceBackupForSendSheet().apply {
                account = backupDetails.account
                action = backupDetails.action
            }
    }
}

data class BackupDetails(
    val account: SingleAccount,
    val action: AssetAction
)
