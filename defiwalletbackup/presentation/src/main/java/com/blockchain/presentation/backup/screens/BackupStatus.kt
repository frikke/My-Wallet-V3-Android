package com.blockchain.presentation.backup.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Alert
import com.blockchain.componentlib.icons.Check
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.tag.SuccessTag
import com.blockchain.componentlib.tag.WarningTag
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.presentation.R
import com.blockchain.presentation.backup.BackUpStatus

@Composable
fun BackupStatus(backupStatus: BackUpStatus) {
    when (backupStatus) {
        BackUpStatus.NO_BACKUP -> {
            WarningTag(
                text = stringResource(com.blockchain.stringResources.R.string.back_up_status_negative),
                startImageResource = Icons.Filled.Alert
            )
        }

        BackUpStatus.BACKED_UP -> {
            SuccessTag(
                text = stringResource(com.blockchain.stringResources.R.string.back_up_status_positive),
                startImageResource = Icons.Filled.Check
            )
        }
    }
}

// ///////////////
// PREVIEWS
// ///////////////

@Preview(name = "Backup Status No Backup")
@Composable
private fun PreviewBackupStatusNoBackup() {
    BackupStatus(BackUpStatus.NO_BACKUP)
}

@Preview(name = "Backup Status Backed up")
@Composable
private fun PreviewBackupStatusBackup() {
    BackupStatus(BackUpStatus.BACKED_UP)
}
