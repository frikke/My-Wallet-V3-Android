package com.blockchain.home.presentation.introduction.composable

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
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.system.ClippedShadow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.home.presentation.R

@Composable
fun EducationalWalletModeSecureTag(
    tag: IntroductionScreenTag
) {
    ClippedShadow(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = AppTheme.colors.background.copy(alpha = 0.2F)
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
                            AppTheme.colors.background,
                            RoundedCornerShape(AppTheme.dimensions.borderRadiiSmall)
                        )
                        .padding(
                            vertical = AppTheme.dimensions.smallestSpacing,
                            horizontal = AppTheme.dimensions.tinySpacing
                        ),
                    text = stringResource(tag.title),
                    style = AppTheme.typography.caption2,
                    color = tag.titleColor ?: AppTheme.colors.title,
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

// ///////////////
// PREVIEWS
// ///////////////

@Preview(showBackground = true, backgroundColor = 0XFF321234)
@Composable
fun PreviewEducationalWalletModeSecureTag() {
    EducationalWalletModeSecureTag(
        IntroductionScreenTag(
            R.string.intro_custodial_tag_title,
            Blue600,
            R.string.intro_custodial_tag_description
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0XFF321234)
@Composable
fun PreviewEducationalWalletModeSecureTagNull() {
    EducationalWalletModeSecureTag(
        IntroductionScreenTag(
            null, null,
            R.string.intro_custodial_tag_description
        )
    )
}
