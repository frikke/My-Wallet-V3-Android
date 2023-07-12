package com.blockchain.transactions.common.confirmation.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun ConfirmationSection(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .background(
                color = AppColors.backgroundSecondary,
                shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium)
            )
    ) {
        content()
    }
}
