package com.blockchain.presentation.screens

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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.presentation.BackUpStatus
import com.blockchain.presentation.R

@Composable
fun BackupStatus(backupStatus: BackUpStatus) {
    Row(
        modifier = Modifier
            .background(
                color = backupStatus.bgColor,
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.tiny_margin))
            )
            .padding(
                horizontal = dimensionResource(id = R.dimen.very_small_margin),
                vertical = dimensionResource(id = R.dimen.tiny_margin)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            imageResource = ImageResource.Local(
                id = backupStatus.icon,
                size = dimensionResource(R.dimen.size_standard)
            )
        )

        Spacer(modifier = Modifier.size(dimensionResource(R.dimen.tiny_margin)))

        Text(
            text = stringResource(backupStatus.text),
            style = AppTheme.typography.caption2,
            color = backupStatus.textColor,
        )
    }
}

// ///////////////
// PREVIEWS
// ///////////////

@Preview(name = "Backup Status No Backup")
@Composable
fun PreviewBackupStatusNoBackup() {
    BackupStatus(BackUpStatus.NO_BACKUP)
}

@Preview(name = "Backup Status Backed up")
@Composable
fun PreviewBackupStatusBackup() {
    BackupStatus(BackUpStatus.BACKED_UP)
}
