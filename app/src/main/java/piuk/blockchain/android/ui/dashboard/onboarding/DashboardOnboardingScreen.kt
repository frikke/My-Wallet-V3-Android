package piuk.blockchain.android.ui.dashboard.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey600
import com.blockchain.componentlib.theme.Grey800
import com.blockchain.componentlib.theme.White
import com.blockchain.domain.onboarding.CompletableDashboardOnboardingStep
import com.blockchain.domain.onboarding.DashboardOnboardingStep
import com.blockchain.domain.onboarding.DashboardOnboardingStepState
import piuk.blockchain.android.R

@Composable
fun DashboardOnboardingScreen(
    state: DashboardOnboardingState,
    onIntent: (DashboardOnboardingIntent) -> Unit,
    backClicked: () -> Unit,
    analyticsNextStepButtonClicked: (Boolean) -> Unit
) {
    val total = state.steps.size
    val complete = state.steps.count { it.isCompleted }

    Column(
        modifier = Modifier.background(AppTheme.colors.light)
    ) {
        NavigationBar(
            title = "",
            onBackButtonClick = backClicked
        )

        Column(
            modifier = Modifier.padding(horizontal = AppTheme.dimensions.smallSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StepsProgress(complete, total)

            SimpleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTheme.dimensions.standardSpacing),
                text = stringResource(com.blockchain.stringResources.R.string.dashboard_onboarding_title2),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            SimpleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTheme.dimensions.tinySpacing),
                text = stringResource(com.blockchain.stringResources.R.string.dashboard_onboarding_title3),
                style = ComposeTypographies.Body1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTheme.dimensions.largeSpacing),
                backgroundColor = White,
                shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
                elevation = 0.dp
            ) {
                LazyColumn(
                    Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(state.steps) { index, item ->
                        val onClick = if (item.isCompleted) {
                            null
                        } else {
                            {
                                analyticsNextStepButtonClicked(false)
                                onIntent(DashboardOnboardingIntent.StepClicked(item.step))
                            }
                        }
                        val subtitleRes = when (item.state) {
                            DashboardOnboardingStepState.INCOMPLETE -> item.step.subtitleRes
                            DashboardOnboardingStepState.PENDING ->
                                com.blockchain.stringResources.R.string.dashboard_onboarding_step_pending

                            DashboardOnboardingStepState.COMPLETE ->
                                com.blockchain.stringResources.R.string.dashboard_onboarding_step_complete
                        }
                        val subtitleColor = when (item.state) {
                            DashboardOnboardingStepState.INCOMPLETE -> Grey600
                            DashboardOnboardingStepState.PENDING -> Grey800
                            DashboardOnboardingStepState.COMPLETE -> AppTheme.colors.success
                        }
                        val endImage = when (item.state) {
                            DashboardOnboardingStepState.INCOMPLETE -> ImageResource.Local(
                                id = R.drawable.ic_chevron_right,
                                colorFilter = ColorFilter.tint(colorResource(item.step.colorRes))
                            )

                            DashboardOnboardingStepState.PENDING ->
                                ImageResource.Local(R.drawable.ic_payment_progress)

                            DashboardOnboardingStepState.COMPLETE -> ImageResource.Local(
                                id = R.drawable.ic_success_check,
                                colorFilter = ColorFilter.tint(AppTheme.colors.success)
                            )
                        }
                        DefaultTableRow(
                            primaryText = stringResource(item.step.titleRes),
                            secondaryText = stringResource(subtitleRes),
                            secondaryTextColor = subtitleColor,
                            startImageResource = ImageResource.Local(
                                id = item.step.iconRes,
                                colorFilter = ColorFilter.tint(
                                    when (item.step) {
                                        DashboardOnboardingStep.UPGRADE_TO_GOLD ->
                                            colorResource(com.blockchain.common.R.color.onboarding_step_upgrade_to_gold)
                                        DashboardOnboardingStep.LINK_PAYMENT_METHOD ->
                                            colorResource(
                                                com.blockchain.common.R.color.onboarding_step_link_payment_method
                                            )
                                        DashboardOnboardingStep.BUY ->
                                            colorResource(com.blockchain.common.R.color.onboarding_step_buy)
                                    }
                                ),
                                size = AppTheme.dimensions.standardSpacing
                            ),
                            endImageResource = endImage,
                            onClick = onClick
                        )

                        if (index != state.steps.lastIndex) {
                            HorizontalDivider(Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            val firstIncompleteStep: DashboardOnboardingStep? = state.steps.find { !it.isCompleted }?.step
            if (firstIncompleteStep != null) {
                val context = LocalContext.current
                val ctaColors = remember(firstIncompleteStep) {
                    ContextCompat.getColor(context, firstIncompleteStep.colorRes).ctaButtonTint
                }

                PrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = AppTheme.dimensions.tinySpacing,
                            vertical = AppTheme.dimensions.smallSpacing
                        ),
                    text = stringResource(firstIncompleteStep.titleRes),
                    onClick = {
                        analyticsNextStepButtonClicked(true)
                        onIntent(DashboardOnboardingIntent.StepClicked(firstIncompleteStep))
                    },
                )
            }
        }
    }
}

@Composable
fun StepsProgress(complete: Int, total: Int, modifier: Modifier = Modifier) {
    val progress = complete.toFloat() / total.toFloat()
    val backgroundColor = AppTheme.colors.medium
    val progressColor = AppTheme.colors.primary

    Box(modifier = modifier.size(AppTheme.dimensions.epicSpacing)) {
        CircularProgressIndicator(
            modifier = Modifier.fillMaxSize(),
            color = backgroundColor,
            strokeWidth = AppTheme.dimensions.tinySpacing,
            progress = 1f
        )
        CircularProgressIndicator(
            modifier = Modifier.fillMaxSize(),
            color = progressColor,
            strokeWidth = AppTheme.dimensions.tinySpacing,
            progress = progress
        )

        SimpleText(
            modifier = Modifier.align(Alignment.Center),
            text = stringResource(
                com.blockchain.stringResources.R.string.dashboard_onboarding_steps_counter,
                complete,
                total
            ),
            style = ComposeTypographies.Title2,
            color = ComposeColors.Primary,
            gravity = ComposeGravities.Centre
        )
    }
}

private val Int.ctaButtonTint: CtaColors
    get() {
        val base = this
        val lighten = ColorUtils.blendARGB(base, android.graphics.Color.WHITE, 0.35f)
        val darken = ColorUtils.blendARGB(base, android.graphics.Color.BLACK, 0.35f)
        return CtaColors(
            disabled = lighten,
            enabled = base,
            pressed = darken
        )
    }

private data class CtaColors(
    val disabled: Int,
    val enabled: Int,
    val pressed: Int
)

@Preview
@Composable
private fun Preview() {
    DashboardOnboardingScreen(
        state = DashboardOnboardingState(
            steps = DashboardOnboardingStep.values().mapIndexed { index, step ->
                CompletableDashboardOnboardingStep(
                    step = step,
                    state = DashboardOnboardingStepState.values()[index]
                )
            }
        ),
        onIntent = {},
        backClicked = {},
        analyticsNextStepButtonClicked = {}
    )
}
