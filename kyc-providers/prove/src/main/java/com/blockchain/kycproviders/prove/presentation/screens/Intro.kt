package com.blockchain.kycproviders.prove.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.White
import com.blockchain.kycproviders.prove.R
import com.blockchain.kycproviders.prove.presentation.ProvePrefillIntent
import com.blockchain.kycproviders.prove.presentation.ProvePrefillViewState
import com.blockchain.kycproviders.prove.presentation.defaultViewState

@Composable
internal fun Intro(
    state: ProvePrefillViewState,
    onIntent: (ProvePrefillIntent) -> Unit,
) {
    Column(
        Modifier.background(White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(ImageResource.Local(R.drawable.ic_blockchain, size = 88.dp))

            SimpleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = AppTheme.dimensions.standardSpacing,
                        start = AppTheme.dimensions.standardSpacing,
                        end = AppTheme.dimensions.standardSpacing,
                    ),
                text = stringResource(R.string.prove_intro_title),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre,
            )

            SimpleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = AppTheme.dimensions.tinySpacing,
                        start = AppTheme.dimensions.standardSpacing,
                        end = AppTheme.dimensions.standardSpacing,
                    ),
                text = stringResource(R.string.prove_intro_subtitle),
                style = ComposeTypographies.Body1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre,
            )
        }

        Column {
            HorizontalDivider(Modifier.fillMaxWidth())

            SimpleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.dimensions.smallSpacing),
                text = stringResource(R.string.prove_intro_cta_info),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre,
            )

            PrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AppTheme.dimensions.smallSpacing,
                        end = AppTheme.dimensions.smallSpacing,
                        bottom = AppTheme.dimensions.smallSpacing
                    ),
                text = stringResource(R.string.common_continue),
                onClick = { onIntent(ProvePrefillIntent.IntroContinueClicked) }
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    Intro(
        state = defaultViewState,
        onIntent = {},
    )
}
