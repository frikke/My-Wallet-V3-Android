package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.res.stringResource
import com.blockchain.componentlib.card.NextStepCard
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.lazylist.paddedRoundedCornersItems
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.home.handhold.HandholStatus
import com.blockchain.home.handhold.HandholdStepStatus
import com.blockchain.home.presentation.handhold.composable.HandholdTask
import com.blockchain.stringResources.R
import kotlinx.collections.immutable.ImmutableList

internal fun LazyListScope.handhold(
    data: ImmutableList<HandholdStepStatus>
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
        NextStepCard(
            stepsCount = data.size,
            completedSteps = data.count { it.isComplete },
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
            enabled = !disabled
        )
    }
}
