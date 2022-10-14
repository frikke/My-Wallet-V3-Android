package piuk.blockchain.android.ui.settings.sheets

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.unit.Dp
import com.blockchain.commonarch.presentation.base.HostedBottomSheet
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.ImageResource
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.BottomSheetBackupPhraseInfoBinding

class BackupPhraseInfoSheet : SlidingModalBottomDialog<BottomSheetBackupPhraseInfoBinding>() {

    interface Host : HostedBottomSheet.Host {
        fun onBackupNow()
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a BackupPhraseInfoSheet.Host"
        )
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetBackupPhraseInfoBinding =
        BottomSheetBackupPhraseInfoBinding.inflate(inflater, container, false)

    override fun initControls(binding: BottomSheetBackupPhraseInfoBinding) {
        with(binding) {
            sheetHeader.apply {
                title = getString(R.string.security_backup_sheet_title)
                onClosePress = {
                    this@BackupPhraseInfoSheet.dismiss()
                }
            }

            backupIcon.apply {
                image = ImageResource.Local(id = R.drawable.ic_lock, size = Dp(48f))
            }
            backupBlurb.apply {
                text = getString(R.string.security_backup_sheet_blurb)
                textColor = ComposeColors.Body
                style = ComposeTypographies.Paragraph1
                gravity = ComposeGravities.Centre
            }

            ctaEnable.apply {
                text = getString(R.string.security_backup_sheet_cta)
                onClick = {
                    dismiss()
                    host.onBackupNow()
                }
            }
        }
    }

    companion object {
        fun newInstance(): BackupPhraseInfoSheet = BackupPhraseInfoSheet()
    }
}
