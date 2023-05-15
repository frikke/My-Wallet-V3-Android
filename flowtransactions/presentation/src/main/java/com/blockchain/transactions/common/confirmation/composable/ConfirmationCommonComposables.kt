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
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.White

@Composable
fun ConfirmationSection(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .background(White, shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium))
    ) {
        content()
    }
}

@Composable
fun ConfirmationTableRow(
    modifier: Modifier = Modifier,
    startTitle: String,
    onClick: (() -> Unit)?,
    startByline: String? = null,
    paragraphText: String? = null,
    endTitle: String? = null,
    endByline: String? = null,
    tags: List<TagViewState>? = null,
    endTag: TagViewState? = null,
    startImageResource: ImageResource = ImageResource.None,
    endImageResource: ImageResource = ImageResource.None
) {
    DefaultTableRow(
        modifier = modifier,
        startTitle = startTitle,
        onClick = onClick,
        startByline = startByline,
        paragraphText = paragraphText,
        endTitle = endTitle,
        endByline = endByline,
        tags = tags,
        endTag = endTag,
        startImageResource = startImageResource,
        endImageResource = endImageResource,
        backgroundColor = White,
        backgroundShape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium),
        titleColor = AppTheme.colors.title,
        titleStyle = AppTheme.typography.paragraph2,
        bylineColor = AppTheme.colors.body,
        bylineStyle = AppTheme.typography.caption1
    )
}
