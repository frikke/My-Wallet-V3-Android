package com.blockchain.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Orange000
import com.blockchain.componentlib.theme.Orange600
import com.blockchain.presentation.R

@Composable
fun BackupStatus() {
    Row(
        modifier = Modifier
            .background(
                color = Orange000,
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.tiny_margin))
            )
            .padding(
                horizontal = dimensionResource(id = R.dimen.very_small_margin),
                vertical = dimensionResource(id = R.dimen.tiny_margin)
            )
    ) {
        Image(imageResource = ImageResource.Local(R.drawable.ic_alert))

        Spacer(modifier = Modifier.size(dimensionResource(R.dimen.tiny_margin)))

        Text(
            text = stringResource(id = R.string.back_up_splash_status_negative),
            style = AppTheme.typography.caption2,
            color = Orange600,
        )
    }
}

@Preview
@Composable
fun PreviewBackupStatus() {
    BackupStatus()
}
