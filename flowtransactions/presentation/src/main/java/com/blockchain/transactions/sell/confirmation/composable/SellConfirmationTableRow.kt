package com.blockchain.transactions.sell.confirmation.composable

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tablerow.custom.CustomTableRow
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.tablerow.custom.ViewStyle
import com.blockchain.componentlib.tablerow.custom.ViewType
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun SellConfirmationTableRow(
    startTitle: String,
    onClick: (() -> Unit)?,
    endTitle: String? = null,
    endByline: String? = null,
    startImageResource: ImageResource = ImageResource.None
) {
    CustomTableRow(
        icon = if (startImageResource !is ImageResource.None) {
            StackedIcon.SingleIcon(startImageResource)
        } else {
            StackedIcon.None
        },
        leadingComponents = listOf(
            ViewType.Text(
                startTitle,
                ViewStyle.TextStyle(
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.body,
                    strikeThrough = false,
                )
            )
        ),
        trailingComponents = listOfNotNull(
            if (endTitle != null) {
                ViewType.Text(
                    endTitle,
                    ViewStyle.TextStyle(
                        style = AppTheme.typography.paragraph2,
                        color = AppTheme.colors.title,
                        strikeThrough = false,
                    )
                )
            } else {
                null
            },
            if (endByline != null) {
                ViewType.Text(
                    endByline,
                    ViewStyle.TextStyle(
                        style = AppTheme.typography.caption1,
                        color = AppTheme.colors.body,
                        strikeThrough = false,
                    )
                )
            } else {
                null
            },
        ),
        onClick = onClick,
        backgroundColor = AppColors.backgroundSecondary,
        backgroundShape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium),
    )
}
