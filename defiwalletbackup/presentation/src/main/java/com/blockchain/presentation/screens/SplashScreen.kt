package com.blockchain.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.control.Checkbox
import com.blockchain.componentlib.control.CheckboxState
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.presentation.R

@Preview
@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        NavigationBar(title = stringResource(id = R.string.secure_defi_wallets), onBackButtonClick = { })
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(.5f, true),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                contentScale = ContentScale.None,
                imageResource = ImageResource.Local(R.drawable.ic_padlock)
            )
            Spacer(Modifier.size(dimensionResource(R.dimen.small_margin)))
            Text(
                text = stringResource(R.string.lets_backup_your_wallet),
                modifier = Modifier.padding(
                    start = dimensionResource(R.dimen.small_margin),
                    end = dimensionResource(R.dimen.small_margin)
                ),
                textAlign = TextAlign.Center,
                style = AppTheme.typography.title3,
                color = AppTheme.colors.title
            )
            Text(
                text = stringResource(R.string.back_up_splash_description),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(
                    start = dimensionResource(R.dimen.standard_margin),
                    end = dimensionResource(R.dimen.standard_margin)
                ),
                style = AppTheme.typography.paragraph1,
                color = AppTheme.colors.medium
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, true)
                .padding(end = dimensionResource(id = R.dimen.standard_margin)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            val isChecked = remember { mutableStateOf(CheckboxState.Unchecked) }
            Row {
                Checkbox(
                    state = isChecked.value,
                    onCheckChanged = { checked ->
                        isChecked.value = if (checked) CheckboxState.Checked else
                            CheckboxState.Unchecked
                    }
                )
                Text(
                    text = stringResource(id = R.string.backup_phrase_checkbox_warning),
                    style = AppTheme.typography.paragraph1
                )
            }
            PrimaryButton(
                text = stringResource(id = R.string.back_up_now),
                state = if (isChecked.value == CheckboxState.Checked) ButtonState.Enabled else
                    ButtonState.Disabled,
                onClick = {}
            )
        }
    }
}
