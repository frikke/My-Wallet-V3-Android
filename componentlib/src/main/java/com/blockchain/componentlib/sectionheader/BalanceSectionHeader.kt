package com.blockchain.componentlib.sectionheader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun BalanceSectionHeader(
    primaryText: String,
    secondaryText: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonState: ButtonState = ButtonState.Enabled,
) {
    Row(
        modifier = modifier.padding(AppTheme.dimensions.paddingLarge),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = primaryText,
                style = AppTheme.typography.title3,
                color = AppTheme.colors.title,
                modifier = Modifier
            )
            Text(
                text = secondaryText,
                style = AppTheme.typography.paragraph2,
                color = AppTheme.colors.body,
                modifier = Modifier
            )
        }
        PrimaryButton(
            text = buttonText,
            onClick = onButtonClick,
            state = buttonState
        )
    }
}

@Preview
@Composable
private fun BalanceSectionHeaderPreview() {
    AppTheme {
        AppSurface {
            BalanceSectionHeader(
                primaryText = "\$12,293.21",
                secondaryText = "0.1393819 BTC",
                buttonText = "Buy BTC",
                onButtonClick = {},
            )
        }
    }
}
