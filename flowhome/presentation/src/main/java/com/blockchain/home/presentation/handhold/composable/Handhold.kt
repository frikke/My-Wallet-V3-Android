package com.blockchain.home.presentation.handhold.composable

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.icons.Cart
import com.blockchain.componentlib.icons.Check
import com.blockchain.componentlib.icons.ChevronRight
import com.blockchain.componentlib.icons.Email
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Identification
import com.blockchain.componentlib.icons.Pending
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.tablerow.custom.CustomTableRow
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.tablerow.custom.ViewStyle
import com.blockchain.componentlib.tablerow.custom.ViewType
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.conditional
import com.blockchain.home.handhold.HandholStatus
import com.blockchain.home.handhold.HandholdStep
import com.blockchain.home.handhold.HandholdStepStatus
import com.blockchain.stringResources.R

@Composable
fun HandholdTask(
    stepStatus: HandholdStepStatus,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .alpha(if (enabled) 1F else 0.5F)
            .conditional(enabled && !stepStatus.isComplete) { clickable(onClick = onClick) },
    ) {
        CustomTableRow(
            icon = StackedIcon.SingleIcon(
                stepStatus.step.icon()
                    .withTint(stepStatus.step.tint())
                    .withBackground(backgroundColor = AppColors.backgroundSecondary)
            ),
            leadingComponents = listOf(
                ViewType.Text(
                    value = stepStatus.step.title(),
                    style = ViewStyle.TextStyle(style = AppTheme.typography.paragraph2, color = AppColors.title)
                ),
                ViewType.Text(
                    value = if (stepStatus.isComplete) {
                        stringResource(R.string.common_completed)
                    } else {
                        stepStatus.subtitle()
                    },
                    style = ViewStyle.TextStyle(
                        style = AppTheme.typography.caption1,
                        color = if (stepStatus.isComplete) {
                            AppColors.success
                        } else {
                            AppColors.body
                        }
                    )
                )
            ),
            trailingComponents = listOf(),
            endIcon = when (stepStatus.status) {
                HandholStatus.Incomplete -> Icons.ChevronRight.withTint(stepStatus.step.tint())
                HandholStatus.Pending -> Icons.Filled.Pending.withTint(AppColors.dark)
                HandholStatus.Complete -> Icons.Filled.Check.withTint(AppColors.success)
            }
        )
    }
}

@Composable
private fun HandholdStep.icon() = when (this) {
    HandholdStep.VerifyEmail -> Icons.Filled.Email
    HandholdStep.Kyc -> Icons.Filled.Identification
    HandholdStep.BuyCrypto -> Icons.Filled.Cart
}

@Composable
private fun HandholdStep.tint() = when (this) {
    HandholdStep.VerifyEmail -> AppColors.explorer
    HandholdStep.Kyc -> AppColors.primary
    HandholdStep.BuyCrypto -> AppColors.success
}

@Composable
private fun HandholdStep.title() = when (this) {
    HandholdStep.VerifyEmail -> R.string.handhold_email
    HandholdStep.Kyc -> R.string.handhold_kyc
    HandholdStep.BuyCrypto -> R.string.handhold_buy
}.run {
    stringResource(id = this)
}

@Composable
private fun HandholdStepStatus.subtitle(): String {
    return if (status == HandholStatus.Pending) {
        stringResource(R.string.in_review)
    } else {
        when (step) {
            HandholdStep.VerifyEmail -> R.string.handhold_completion_seconds to 30
            HandholdStep.Kyc -> R.string.handhold_completion_minutes to 3
            HandholdStep.BuyCrypto -> R.string.handhold_completion_seconds to 10
        }.run {
            val (id, duration) = this
            stringResource(id = id, duration)
        }
    }
}

@Preview
@Composable
private fun PreviewHandholdScreenEmailIncompleteDisabled() {
    HandholdTask(
        stepStatus = HandholdStepStatus(
            step = HandholdStep.VerifyEmail, status = HandholStatus.Incomplete
        ),
        enabled = false,
        onClick = {}
    )
}

@Preview
@Composable
private fun PreviewHandholdScreenEmailIncomplete() {
    HandholdTask(
        stepStatus = HandholdStepStatus(
            step = HandholdStep.VerifyEmail, status = HandholStatus.Incomplete
        ),
        onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewHandholdScreenEmailIncompleteDark() {
    PreviewHandholdScreenEmailIncomplete()
}

@Preview
@Composable
private fun PreviewHandholdScreenEmailComplete() {
    HandholdTask(
        stepStatus = HandholdStepStatus(
            step = HandholdStep.VerifyEmail, status = HandholStatus.Complete
        ),
        onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewHandholdScreenEmailCompleteDark() {
    PreviewHandholdScreenEmailComplete()
}

@Preview
@Composable
private fun PreviewHandholdScreenKycIncomplete() {
    HandholdTask(
        stepStatus = HandholdStepStatus(
            step = HandholdStep.Kyc, status = HandholStatus.Incomplete
        ),
        onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewHandholdScreenKycIncompleteDark() {
    PreviewHandholdScreenKycIncomplete()
}

@Preview
@Composable
private fun PreviewHandholdScreenKycPending() {
    HandholdTask(
        stepStatus = HandholdStepStatus(
            step = HandholdStep.Kyc, status = HandholStatus.Pending
        ),
        onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewHandholdScreenKycPendingDark() {
    PreviewHandholdScreenKycPending()
}

@Preview
@Composable
private fun PreviewHandholdScreenKycComplete() {
    HandholdTask(
        stepStatus = HandholdStepStatus(
            step = HandholdStep.Kyc, status = HandholStatus.Complete
        ),
        onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewHandholdScreenKycCompleteDark() {
    PreviewHandholdScreenKycComplete()
}

@Preview
@Composable
private fun PreviewHandholdScreenBuyIncomplete() {
    HandholdTask(
        stepStatus = HandholdStepStatus(
            step = HandholdStep.BuyCrypto, status = HandholStatus.Incomplete
        ),
        onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewHandholdScreenBuyIncompleteDark() {
    PreviewHandholdScreenBuyIncomplete()
}

@Preview
@Composable
private fun PreviewHandholdScreenBuyComplete() {
    HandholdTask(
        stepStatus = HandholdStepStatus(
            step = HandholdStep.BuyCrypto, status = HandholStatus.Complete
        ),
        onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewHandholdScreenBuyCompleteDark() {
    PreviewHandholdScreenBuyComplete()
}