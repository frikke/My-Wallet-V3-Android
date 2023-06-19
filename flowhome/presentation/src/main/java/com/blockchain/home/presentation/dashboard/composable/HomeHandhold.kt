package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.res.stringResource
import com.blockchain.componentlib.card.TasksSummaryCard
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.lazylist.paddedRoundedCornersItems
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.home.handhold.HandholdTask
import com.blockchain.home.handhold.HandholdTasksStatus
import com.blockchain.home.presentation.handhold.composable.HandholdTask
import com.blockchain.stringResources.R
import kotlinx.collections.immutable.ImmutableList

internal fun LazyListScope.handhold(
    data: ImmutableList<HandholdTasksStatus>,
    onClick: (HandholdTask) -> Unit
) {
    paddedItem(
        paddingValues = {
            PaddingValues(
                start = AppTheme.dimensions.smallSpacing,
                end = AppTheme.dimensions.smallSpacing,
                top = AppTheme.dimensions.smallSpacing,
                bottom = AppTheme.dimensions.tinySpacing
            )
        }
    ) {
        TasksSummaryCard(
            allTasksCount = data.size,
            completedTasksCount = data.count { it.isComplete },
            title = stringResource(id = R.string.handhold_title),
            description = stringResource(id = R.string.handhold_subtitle)
        )
    }

    paddedRoundedCornersItems(
        items = data,
        paddingValues = {
            PaddingValues(
                start = AppTheme.dimensions.smallSpacing,
                end = AppTheme.dimensions.smallSpacing,
                top = AppTheme.dimensions.tinySpacing,
                bottom = AppTheme.dimensions.smallSpacing
            )
        }
    ) { stepStatus ->
        val isAnyPreviousIncomplete = data.subList(0, data.indexOf(stepStatus))
            .any { !it.isComplete }

        val disabled = stepStatus.isIncomplete && isAnyPreviousIncomplete

        HandholdTask(
            stepStatus = stepStatus,
            enabled = !disabled,
            onClick = {
                onClick(stepStatus.task)
            }
        )
    }
}
