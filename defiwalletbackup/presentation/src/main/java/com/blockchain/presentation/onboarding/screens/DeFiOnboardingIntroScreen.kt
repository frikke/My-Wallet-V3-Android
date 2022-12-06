package com.blockchain.presentation.onboarding.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue000
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.theme.Grey100
import com.blockchain.componentlib.utils.circleAround
import com.blockchain.presentation.R
import com.blockchain.presentation.onboarding.DeFiOnboardingIntent
import com.blockchain.presentation.onboarding.viewmodel.DeFiOnboardingViewModel

/**
 * figma: https://www.figma.com/file/VTMHbEoX0QDNOLKKdrgwdE/AND---Super-App?node-id=260%3A16643
 */
@Composable
fun DeFiOnboardingIntro(viewModel: DeFiOnboardingViewModel) {
    DeFiOnboardingIntroScreen(
        closeOnClick = { viewModel.onIntent(DeFiOnboardingIntent.EndFlow(isSuccessful = false)) },
        enableDeFiOnClick = { viewModel.onIntent(DeFiOnboardingIntent.EnableDeFiWallet) },
    )
}

@Composable
fun DeFiOnboardingIntroScreen(
    closeOnClick: () -> Unit,
    enableDeFiOnClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        NavigationBar(
            title = stringResource(R.string.defi_wallet_name),
            endNavigationBarButtons = listOf(
                NavigationBarButton.Icon(
                    drawable = R.drawable.ic_close_circle,
                    color = null,
                    contentDescription = R.string.accessibility_close,
                    onIconClick = closeOnClick
                )
            )
        )

        Box {
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(0.8f)
                    .align(Alignment.BottomCenter),
                imageResource = ImageResource.Local(R.drawable.ic_grid),
                contentScale = ContentScale.FillWidth
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppTheme.dimensions.smallSpacing),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.weight(1F))

                Image(imageResource = ImageResource.Local(R.drawable.ic_defi_onboarding))

                Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

                SimpleText(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(
                        R.string.defi_onboarding_intro_title,
                        stringResource(R.string.defi_wallet_name)
                    ),
                    style = ComposeTypographies.Title3,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Centre
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

                SimpleText(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.defi_onboarding_intro_description),
                    style = ComposeTypographies.Paragraph1,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Centre
                )

                Spacer(modifier = Modifier.weight(1F))

                DeFiOnboardingProperties(
                    properties = listOf(
                        DeFiProperty(
                            title = R.string.defi_onboarding_intro_property1_title,
                            subtitle = R.string.defi_onboarding_intro_property1_subtitle
                        ),
                        DeFiProperty(
                            title = R.string.defi_onboarding_intro_property2_title,
                            subtitle = R.string.defi_onboarding_intro_property2_subtitle
                        ),
                        DeFiProperty(
                            title = R.string.defi_onboarding_intro_property3_title,
                            subtitle = R.string.defi_onboarding_intro_property3_subtitle
                        ),
                    )
                )

                Spacer(modifier = Modifier.weight(2F))

                PrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.defi_onboarding_intro_cta),
                    state = ButtonState.Enabled,
                    onClick = enableDeFiOnClick
                )
            }
        }
    }
}

@Composable
fun DeFiOnboardingPropertyItem(
    number: Int,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = Grey100,
                shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium)
            )
            .background(color = Color.White, shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium))
            .padding(
                horizontal = AppTheme.dimensions.smallSpacing,
                vertical = dimensionResource(R.dimen.very_small_spacing)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            modifier = Modifier
                .circleAround(color = Blue000),
            style = AppTheme.typography.body2,
            color = Blue600,
            text = number.toString(),
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

        Column {
            SimpleText(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.composeSmallestSpacing))

            SimpleText(
                modifier = Modifier.fillMaxWidth(),
                text = subtitle,
                style = ComposeTypographies.Caption1,
                color = ComposeColors.Muted,
                gravity = ComposeGravities.Start
            )
        }
    }
}

@Composable
fun DeFiOnboardingProperties(properties: List<DeFiProperty>) {
    Column {
        properties.forEachIndexed { index, property ->
            DeFiOnboardingPropertyItem(
                number = index.inc(),
                title = stringResource(property.title),
                subtitle = stringResource(property.subtitle),
            )

            if (index != properties.lastIndex) {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
            }
        }
    }
}

data class DeFiProperty(@StringRes val title: Int, @StringRes val subtitle: Int)

// ///////////////
// PREVIEWS
// ///////////////

@Preview(showBackground = true)
@Composable
fun PreviewDeFiOnboardingIntroScreen() {
    DeFiOnboardingIntroScreen({}, {})
}

@Preview(showBackground = true)
@Composable
fun PreviewDeFiOnboardingPropertyItem() {
    DeFiOnboardingPropertyItem(
        number = 1,
        title = "Self-Custody Your Assets",
        subtitle = "DeFi wallets are on-chain"
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewDeFiOnboardingProperties() {
    DeFiOnboardingProperties(
        properties = listOf(
            DeFiProperty(
                title = R.string.defi_onboarding_intro_property1_title,
                subtitle = R.string.defi_onboarding_intro_property1_subtitle
            ),
            DeFiProperty(
                title = R.string.defi_onboarding_intro_property2_title,
                subtitle = R.string.defi_onboarding_intro_property2_subtitle
            ),
            DeFiProperty(
                title = R.string.defi_onboarding_intro_property3_title,
                subtitle = R.string.defi_onboarding_intro_property3_subtitle
            ),
        )
    )
}
