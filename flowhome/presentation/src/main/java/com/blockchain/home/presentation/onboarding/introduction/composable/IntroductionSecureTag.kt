package com.blockchain.home.presentation.onboarding.introduction.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.system.ClippedShadow
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.walletmode.WalletMode

@Composable
fun EducationalWalletModeSecureTag(
    tag: IntroductionScreenTag,
    forWalletMode: WalletMode?
) {
    ClippedShadow(
        modifier = Modifier.fillMaxWidth(),
        elevation = AppTheme.dimensions.mediumElevation,
        shape = RoundedCornerShape(AppTheme.dimensions.tinySpacing),
        backgroundColor = AppTheme.colors.backgroundSecondary.copy(alpha = 0.2F)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.smallSpacing)
        ) {
            tag.title?.let {
                Text(
                    modifier = Modifier
                        .background(
                            AppTheme.colors.backgroundSecondary,
                            RoundedCornerShape(AppTheme.dimensions.borderRadiiSmall)
                        )
                        .padding(
                            vertical = AppTheme.dimensions.smallestSpacing,
                            horizontal = AppTheme.dimensions.tinySpacing
                        ),
                    text = stringResource(tag.title),
                    style = AppTheme.typography.caption2,
                    color = forWalletMode?.tagTitleColor() ?: AppTheme.colors.title,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
            }

            Text(
                text = stringResource(tag.description),
                style = if (tag.title != null) {
                    AppTheme.typography.caption1
                } else {
                    AppTheme.typography.paragraph1
                },
                color = AppTheme.colors.title
            )
        }
    }
}

@Composable
private fun WalletMode.tagTitleColor() = when (this) {
    WalletMode.CUSTODIAL -> AppColors.primary
    WalletMode.NON_CUSTODIAL -> AppColors.explorer
}

// ///////////////
// PREVIEWS
// ///////////////

@Preview(showBackground = true, backgroundColor = 0XFF321234)
@Composable
fun PreviewEducationalWalletModeSecureTag() {
    EducationalWalletModeSecureTag(
        IntroductionScreenTag(
            com.blockchain.stringResources.R.string.intro_custodial_tag_title,
            com.blockchain.stringResources.R.string.intro_custodial_tag_description
        ),
        WalletMode.CUSTODIAL
    )
}

@Preview(showBackground = true, backgroundColor = 0XFF321234)
@Composable
fun PreviewEducationalWalletModeSecureTagNull() {
    EducationalWalletModeSecureTag(
        IntroductionScreenTag(
            null,
            com.blockchain.stringResources.R.string.intro_custodial_tag_description
        ),
        null
    )
}
