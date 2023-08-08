package com.blockchain.home.presentation.onboarding.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Pending
import com.blockchain.componentlib.icons.User
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.tag.Tag
import com.blockchain.componentlib.tag.TagSize
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.stringResources.R

@Composable
fun KycVerificationPrompt(onVerifyClicked: () -> Unit, onDismissClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .background(AppTheme.colors.background)
            .fillMaxWidth()
            .padding(horizontal = AppTheme.dimensions.smallSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        SheetHeader(onClosePress = onDismissClicked)

        StandardVerticalSpacer()

        Box(
            modifier = Modifier
                .background(Color.White, CircleShape)
                .size(88.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                modifier = Modifier.size(58.dp),
                imageResource = Icons.Filled.User
            )
        }

        StandardVerticalSpacer()

        SimpleText(
            text = stringResource(R.string.kyc_prompt_title),
            style = ComposeTypographies.Title3,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )

        SmallVerticalSpacer()

        SimpleText(
            text = stringResource(R.string.kyc_prompt_description),
            style = ComposeTypographies.Body1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre
        )

        StandardVerticalSpacer()

        Tag(
            text = stringResource(R.string.kyc_prompt_tag),
            size = TagSize.Primary,
            backgroundColor = AppColors.backgroundSecondary,
            textColor = AppColors.primary,
            startImageResource = Icons.Filled.Pending,
            onClick = null
        )

        SmallVerticalSpacer()

        PrimaryButton(
            text = stringResource(R.string.kyc_prompt_cta),
            onClick = onVerifyClicked,
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeightIn(min = 56.dp)
        )

        StandardVerticalSpacer()
    }
}

@Preview
@Composable
fun KycVerificationPromptPreview() {
    AppTheme {
        KycVerificationPrompt(onVerifyClicked = {}, onDismissClicked = {})
    }
}
