package com.blockchain.presentation.backup.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.button.TertiaryButton
import com.blockchain.componentlib.icons.Check
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.CopyText
import com.blockchain.presentation.R
import com.blockchain.presentation.backup.CopyState

@Composable
fun CopyMnemonicCta(
    copyState: CopyState,
    mnemonic: List<String>,
    mnemonicCopied: () -> Unit
) {
    var copyMnemonic by remember { mutableStateOf(false) }

    if (copyMnemonic) {
        CopyText(
            label = stringResource(id = R.string.manual_backup_title),
            textToCopy = mnemonic.joinToString(separator = " ")
        )
        mnemonicCopied()
        copyMnemonic = false
    }

    when (copyState) {
        is CopyState.Idle -> {
            TertiaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.common_copy),
                onClick = { copyMnemonic = true }
            )
        }

        CopyState.Copied -> {
            MnemonicCopied()
        }
    }
}

@Preview
@Composable
fun MnemonicCopied() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(id = R.dimen.very_small_spacing)),
        horizontalArrangement = Arrangement.Center
    ) {
        Image(Icons.Filled.Check.withTint(AppTheme.colors.success))

        Spacer(modifier = Modifier.size(dimensionResource(R.dimen.tiny_spacing)))

        Text(
            text = stringResource(R.string.manual_backup_copied),
            textAlign = TextAlign.Center,
            style = AppTheme.typography.body2,
            color = AppTheme.colors.success
        )
    }
}
