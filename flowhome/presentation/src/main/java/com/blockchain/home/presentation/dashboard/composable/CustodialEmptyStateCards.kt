package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.coincore.AssetAction
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.MinimalButton
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.tablerow.TableRow
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.Grey800
import com.blockchain.componentlib.theme.Grey900
import com.blockchain.componentlib.theme.SmallHorizontalSpacer
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.domain.onboarding.DashboardOnboardingStepState
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.dashboard.CustodialEmptyCardIntent
import com.blockchain.home.presentation.dashboard.CustodialEmptyCardViewModel
import com.blockchain.home.presentation.dashboard.CustodialEmptyCardViewState
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.koin.payloadScope
import org.koin.androidx.compose.getViewModel

@Composable
fun CustodialEmptyStateCards(
    assetActionsNavigation: AssetActionsNavigation,
    viewModel: CustodialEmptyCardViewModel = getViewModel(scope = payloadScope)
) {
    val viewState: CustodialEmptyCardViewState by viewModel.viewState.collectAsStateLifecycleAware()

    DisposableEffect(key1 = viewModel) {
        viewModel.onIntent(CustodialEmptyCardIntent.LoadEmptyStateConfig)
        onDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        BuyProgressCard(
            viewState.steps.size,
            viewState.steps.filter { it.state == DashboardOnboardingStepState.COMPLETE }.size
        ) {
            assetActionsNavigation.onBoardingNavigation(viewState.steps)
        }

        Spacer(modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.standard_spacing)))
        Card(
            backgroundColor = AppTheme.colors.background,
            shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
            elevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = CenterHorizontally
            ) {
                Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.standard_spacing)))
                CustomStackedIcon(
                    icon = StackedIcon.SmallTag(
                        main = ImageResource.Remote(viewState.trendCurrency.logo),
                        tag = ImageResource.Remote(viewState.tradingCurrency.logo)
                    ),
                    size = 88.dp
                )

                Text(
                    modifier = Modifier.padding(
                        vertical = AppTheme.dimensions.smallSpacing,
                    ),
                    textAlign = TextAlign.Center,
                    text = stringResource(id = R.string.to_get_started_buy_your_first_btc),
                    style = AppTheme.typography.title2,
                    color = Grey900
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTheme.dimensions.smallSpacing)
                ) {
                    viewState.amounts.map { amount ->
                        Button(
                            content = {
                                Text(
                                    text = amount.toStringWithSymbol(false),
                                    color = Color.White,
                                    style = AppTheme.typography.paragraphMono
                                )
                            },
                            onClick = {
                                if (viewState.userCanBuy) {
                                    assetActionsNavigation.buyCrypto(
                                        viewState.trendCurrency,
                                        amount
                                    )
                                } else {
                                    assetActionsNavigation.navigate(AssetAction.Buy)
                                }
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Grey800),
                            modifier = Modifier.weight(1f)
                        )

                        SmallHorizontalSpacer()
                    }

                    Button(
                        content = {
                            Text(
                                text = stringResource(id = R.string.common_other),
                                color = Color.White,
                                style = AppTheme.typography.paragraphMono
                            )
                        },
                        onClick = {
                            if (viewState.userCanBuy) {
                                assetActionsNavigation.buyCrypto(
                                    currency = viewState.trendCurrency,
                                    amount = null
                                )
                            } else {
                                assetActionsNavigation.navigate(AssetAction.Buy)
                            }
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Grey800),
                        modifier = Modifier.weight(1f)
                    )
                }

                MinimalButton(
                    modifier = Modifier
                        .padding(
                            vertical = AppTheme.dimensions.standardSpacing,
                            horizontal = AppTheme.dimensions.smallSpacing
                        )
                        .fillMaxWidth(),
                    text = stringResource(id = R.string.buy_different_crypto),
                    onClick = {
                        assetActionsNavigation.navigate(AssetAction.Buy)
                    }
                )
            }
        }
    }
}

@Composable
fun BuyProgressCard(totalSteps: Int, completedSteps: Int, onboardingLaunch: () -> Unit) {
    Card(
        backgroundColor = AppTheme.colors.background,
        shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
        elevation = 3.dp
    ) {

        TableRow(
            content = {
                Box {
                    val animateFloat = remember { Animatable(0f) }

                    LaunchedEffect(animateFloat) {
                        animateFloat.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = 666,
                                easing = LinearEasing
                            )
                        )
                    }

                    Canvas(
                        modifier = Modifier.size(50.dp),
                        onDraw = {
                            drawCircle(
                                color = Grey000,
                                style = Stroke(
                                    width = 12f
                                )
                            )
                            drawArc(
                                color = Blue600,
                                startAngle = -90f,
                                sweepAngle = (
                                    completedSteps.times(360f)
                                        .div(totalSteps)
                                    ).times(animateFloat.value),
                                useCenter = false,
                                style = Stroke(
                                    width = 12f
                                )
                            )
                        }
                    )

                    Text(
                        modifier = Modifier.align(Center),
                        text = "$completedSteps/$totalSteps",
                        color = AppTheme.colors.primary,
                        style = AppTheme.typography.paragraphMono
                    )
                }

                Column(
                    modifier = Modifier.padding(
                        start = AppTheme.dimensions.smallSpacing
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.complete_your_profile),
                        style = AppTheme.typography.caption1,
                        color = Grey400
                    )
                    Text(
                        text = stringResource(id = R.string.buy_crypto_today),
                        style = AppTheme.typography.body2,
                        color = Grey900
                    )
                }
            }, onContentClicked = onboardingLaunch
        )
    }
}

@Preview
@Composable
fun PreviewBuyCard() {
    AppTheme {
        BuyProgressCard(totalSteps = 3, completedSteps = 1, onboardingLaunch = {})
    }
}
