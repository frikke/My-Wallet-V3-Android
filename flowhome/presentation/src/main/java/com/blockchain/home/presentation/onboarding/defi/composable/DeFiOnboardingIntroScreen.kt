package com.blockchain.home.presentation.onboarding.defi.composable

import android.content.res.Configuration
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.basic.closeImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.navigation.ModeBackgroundColor
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue000
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.theme.Grey100
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.TinyVerticalSpacer
import com.blockchain.componentlib.utils.circleAround
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.onboarding.defi.DeFiOnboardingViewModel
import com.blockchain.home.presentation.onboarding.defi.OnboardingAnalyticsEvents
import com.blockchain.koin.payloadScope
import com.blockchain.walletmode.WalletMode
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@Composable
fun DeFiOnboarding(
    analytics: Analytics = get(),
    viewModel: DeFiOnboardingViewModel = getViewModel(scope = payloadScope),
    showCloseIcon: Boolean,
    closeOnClick: () -> Unit,
    enableDeFiOnClick: () -> Unit
) {
    DisposableEffect(Unit) {
        viewModel.markAsSeen()
        analytics.logEvent(OnboardingAnalyticsEvents.OnboardingViewed)
        onDispose { }
    }

    DeFiOnboardingScreen(
        showCloseIcon = showCloseIcon,
        closeOnClick = closeOnClick,
        enableDeFiOnClick = {
            analytics.logEvent(OnboardingAnalyticsEvents.OnboardingContinueClicked)
            enableDeFiOnClick()
        }
    )
}

@Composable
fun DeFiOnboardingScreen(
    showCloseIcon: Boolean,
    closeOnClick: () -> Unit,
    enableDeFiOnClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        NavigationBar(
            title = stringResource(com.blockchain.stringResources.R.string.defi_wallet_name),
            endNavigationBarButtons = listOfNotNull(
                NavigationBarButton.IconResource(
                    image = closeImageResource(isScreenBackgroundSecondary = false),
                    onIconClick = closeOnClick
                ).takeIf { showCloseIcon }
            ),
            modeColor = ModeBackgroundColor.Override(WalletMode.NON_CUSTODIAL),
        )

        Box {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppTheme.dimensions.smallSpacing),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1F))

                Image(imageResource = ImageResource.Local(R.drawable.defi_onboarding))

                SmallVerticalSpacer()

                SimpleText(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(
                        com.blockchain.stringResources.R.string.defi_onboarding_intro_title,
                        stringResource(com.blockchain.stringResources.R.string.defi_wallet_name)
                    ),
                    style = ComposeTypographies.Title3,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Centre
                )

                TinyVerticalSpacer()

                SimpleText(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(com.blockchain.stringResources.R.string.defi_onboarding_intro_description),
                    style = ComposeTypographies.Paragraph1,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Centre
                )

                Spacer(modifier = Modifier.weight(1F))

                DeFiOnboardingProperties(
                    properties = listOf(
                        DeFiProperty(
                            title = com.blockchain.stringResources.R.string.defi_onboarding_intro_property1_title,
                            subtitle = com.blockchain.stringResources.R.string.defi_onboarding_intro_property1_subtitle
                        ),
                        DeFiProperty(
                            title = com.blockchain.stringResources.R.string.defi_onboarding_intro_property2_title,
                            subtitle = com.blockchain.stringResources.R.string.defi_onboarding_intro_property2_subtitle
                        ),
                        DeFiProperty(
                            title = com.blockchain.stringResources.R.string.defi_onboarding_intro_property3_title,
                            subtitle = com.blockchain.stringResources.R.string.defi_onboarding_intro_property3_subtitle
                        )
                    )
                )

                Spacer(modifier = Modifier.weight(2F))

                PrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = com.blockchain.stringResources.R.string.defi_onboarding_intro_cta),
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
            .background(
                color = AppColors.backgroundSecondary,
                shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium)
            )
            .padding(
                horizontal = AppTheme.dimensions.smallSpacing,
                vertical = dimensionResource(com.blockchain.componentlib.R.dimen.very_small_spacing)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .circleAround(color = AppColors.background),
            style = AppTheme.typography.body2,
            color = AppColors.primary,
            text = number.toString()
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
                subtitle = stringResource(property.subtitle)
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

@Preview
@Composable
fun PreviewDeFiOnboardingScreen() {
    AppTheme {
        DeFiOnboardingScreen(
            showCloseIcon = true,
            closeOnClick = {},
            enableDeFiOnClick = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDeFiOnboardingScreenDark() {
    PreviewDeFiOnboardingScreen()
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
                title = com.blockchain.stringResources.R.string.defi_onboarding_intro_property1_title,
                subtitle = com.blockchain.stringResources.R.string.defi_onboarding_intro_property1_subtitle
            ),
            DeFiProperty(
                title = com.blockchain.stringResources.R.string.defi_onboarding_intro_property2_title,
                subtitle = com.blockchain.stringResources.R.string.defi_onboarding_intro_property2_subtitle
            ),
            DeFiProperty(
                title = com.blockchain.stringResources.R.string.defi_onboarding_intro_property3_title,
                subtitle = com.blockchain.stringResources.R.string.defi_onboarding_intro_property3_subtitle
            )
        )
    )
}
