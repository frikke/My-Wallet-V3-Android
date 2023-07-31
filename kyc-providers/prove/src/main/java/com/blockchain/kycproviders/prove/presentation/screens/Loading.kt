package com.blockchain.kycproviders.prove.presentation.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.loader.LoadingIndicator
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme

@Composable
internal fun Loading(description: String? = null) {
    Column(
        modifier = Modifier.fillMaxSize().background(AppColors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LoadingIndicator(
            color = AppColors.primary
        )

        if (description != null) {
            SimpleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = AppTheme.dimensions.xLargeSpacing,
                        start = AppTheme.dimensions.standardSpacing,
                        end = AppTheme.dimensions.standardSpacing
                    ),
                text = description,
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )
        }
    }
}

@Preview
@Composable
private fun PreviewLoading() {
    Loading()
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewLoadingDark() {
    PreviewLoading()
}

@Preview
@Composable
private fun PreviewDescription() {
    Loading("Verifying information")
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDescriptionDark() {
    PreviewDescription()
}
