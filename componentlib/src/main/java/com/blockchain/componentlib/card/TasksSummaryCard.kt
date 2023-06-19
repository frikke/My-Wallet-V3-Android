package com.blockchain.componentlib.card

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.loader.TasksIndicator
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun TasksSummaryCard(
    allTasksCount: Int,
    completedTasksCount: Int,
    title: String,
    description: String
) {
    require(allTasksCount >= completedTasksCount) { "completedTasksCount cannot be greater than allTasksCount" }

    Surface(
        color = AppColors.backgroundSecondary,
        shape = AppTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = AppTheme.dimensions.standardSpacing,
                    horizontal = AppTheme.dimensions.smallSpacing
                )
        ) {
            TasksIndicator(
                allTasksCount = allTasksCount,
                completedTasksCount = completedTasksCount,
                color = AppColors.primary
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

            Column {
                Text(
                    text = title,
                    style = AppTheme.typography.caption1,
                    color = AppColors.body
                )

                Text(
                    text = description,
                    style = AppTheme.typography.body2,
                    color = AppColors.title
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewTasksSummaryCard() {
    TasksSummaryCard(
        allTasksCount = 3, completedTasksCount = 1,
        title = "Complete your profile", description = "Trade crypto today"
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewTasksSummaryCardDark() {
    PreviewTasksSummaryCard()
}
