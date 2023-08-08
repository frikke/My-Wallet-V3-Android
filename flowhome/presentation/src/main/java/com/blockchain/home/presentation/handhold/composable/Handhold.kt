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
import com.blockchain.home.handhold.HandholdStatus
import com.blockchain.home.handhold.HandholdTask
import com.blockchain.home.handhold.HandholdTasksStatus
import com.blockchain.stringResources.R

@Composable
fun HandholdTask(
    taskStatus: HandholdTasksStatus,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .alpha(if (enabled) 1F else 0.5F)
            .conditional(enabled && !taskStatus.isComplete) { clickable(onClick = onClick) },
    ) {
        CustomTableRow(
            icon = StackedIcon.SingleIcon(
                taskStatus.task.icon()
                    .withTint(taskStatus.task.tint())
                    .withBackground(backgroundColor = AppColors.backgroundSecondary)
            ),
            leadingComponents = listOf(
                ViewType.Text(
                    value = taskStatus.task.title(),
                    style = ViewStyle.TextStyle(style = AppTheme.typography.paragraph2, color = AppColors.title)
                ),
                ViewType.Text(
                    value = if (taskStatus.isComplete) {
                        stringResource(R.string.common_completed)
                    } else {
                        taskStatus.subtitle()
                    },
                    style = ViewStyle.TextStyle(
                        style = AppTheme.typography.caption1,
                        color = if (taskStatus.isComplete) {
                            AppColors.success
                        } else {
                            AppColors.body
                        }
                    )
                )
            ),
            trailingComponents = listOf(),
            endIcon = when (taskStatus.status) {
                HandholdStatus.Incomplete -> Icons.ChevronRight.withTint(taskStatus.task.tint())
                HandholdStatus.Pending -> Icons.Filled.Pending.withTint(AppColors.dark)
                HandholdStatus.Complete -> Icons.Filled.Check.withTint(AppColors.success)
            }
        )
    }
}

@Composable
private fun HandholdTask.icon() = when (this) {
    HandholdTask.VerifyEmail -> Icons.Filled.Email
    HandholdTask.Kyc -> Icons.Filled.Identification
    HandholdTask.BuyCrypto -> Icons.Filled.Cart
}

@Composable
private fun HandholdTask.tint() = when (this) {
    HandholdTask.VerifyEmail -> AppColors.explorer
    HandholdTask.Kyc -> AppColors.primary
    HandholdTask.BuyCrypto -> AppColors.success
}

@Composable
private fun HandholdTask.title() = when (this) {
    HandholdTask.VerifyEmail -> R.string.handhold_email
    HandholdTask.Kyc -> R.string.handhold_kyc
    HandholdTask.BuyCrypto -> R.string.handhold_buy
}.run {
    stringResource(id = this)
}

@Composable
private fun HandholdTasksStatus.subtitle(): String {
    return if (status == HandholdStatus.Pending) {
        stringResource(R.string.in_review)
    } else {
        when (task) {
            HandholdTask.VerifyEmail -> R.string.handhold_completion_seconds to 30
            HandholdTask.Kyc -> R.string.handhold_completion_minutes to 3
            HandholdTask.BuyCrypto -> R.string.handhold_completion_seconds to 10
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
        taskStatus = HandholdTasksStatus(
            task = HandholdTask.VerifyEmail, status = HandholdStatus.Incomplete
        ),
        enabled = false,
        onClick = {}
    )
}

@Preview
@Composable
private fun PreviewHandholdScreenEmailIncomplete() {
    HandholdTask(
        taskStatus = HandholdTasksStatus(
            task = HandholdTask.VerifyEmail, status = HandholdStatus.Incomplete
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
        taskStatus = HandholdTasksStatus(
            task = HandholdTask.VerifyEmail, status = HandholdStatus.Complete
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
        taskStatus = HandholdTasksStatus(
            task = HandholdTask.Kyc, status = HandholdStatus.Incomplete
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
        taskStatus = HandholdTasksStatus(
            task = HandholdTask.Kyc, status = HandholdStatus.Pending
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
        taskStatus = HandholdTasksStatus(
            task = HandholdTask.Kyc, status = HandholdStatus.Complete
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
        taskStatus = HandholdTasksStatus(
            task = HandholdTask.BuyCrypto, status = HandholdStatus.Incomplete
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
        taskStatus = HandholdTasksStatus(
            task = HandholdTask.BuyCrypto, status = HandholdStatus.Complete
        ),
        onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewHandholdScreenBuyCompleteDark() {
    PreviewHandholdScreenBuyComplete()
}
