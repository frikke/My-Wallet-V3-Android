package com.blockchain.componentlib.sheets

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun BasicSheet(
    title: String,
    description: String,
    actionText: String,
    closeClicked: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SheetHeader(
            onClosePress = closeClicked,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.background),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
            SimpleText(
                text = title,
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )
            SimpleText(
                modifier = Modifier.padding(
                    horizontal = AppTheme.dimensions.smallSpacing,
                    vertical = AppTheme.dimensions.tinySpacing
                ),
                text = description,
                style = ComposeTypographies.Body1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre
            )
            PrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = AppTheme.dimensions.smallSpacing,
                        vertical = AppTheme.dimensions.standardSpacing
                    ),
                text = actionText,
                onClick = closeClicked
            )
        }
    }
}

@Preview
@Composable
private fun PreviewBasicSheet() {
    BasicSheet(
        closeClicked = {},
        title = "title",
        description = "description description description",
        actionText = "Got it"
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewBasicSheetDark() {
    PreviewBasicSheet()
}
