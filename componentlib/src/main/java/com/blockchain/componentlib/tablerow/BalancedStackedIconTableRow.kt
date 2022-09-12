package com.blockchain.componentlib.tablerow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.AnnotatedString
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icon.StackedIcons
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun BalanceStackedIconTableRow(
    titleStart: AnnotatedString,
    titleEnd: AnnotatedString? = null,
    bodyStart: AnnotatedString,
    bodyEnd: AnnotatedString? = null,
    onClick: () -> Unit,
    topImageResource: ImageResource,
    bottomImageResource: ImageResource,
) {
    TableRow(
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                TableRowText(
                    startText = titleStart,
                    endText = titleEnd,
                    textStyle = AppTheme.typography.body2,
                    textColor = AppTheme.colors.title
                )
                TableRowText(
                    startText = bodyStart,
                    endText = bodyEnd,
                    textStyle = AppTheme.typography.paragraph1,
                    textColor = AppTheme.colors.body,
                )
            }
        },
        contentStart = {
            StackedIcons(
                topImageResource = topImageResource,
                bottomImageResource = bottomImageResource,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = dimensionResource(R.dimen.medium_spacing))
            )
        },
        onContentClicked = onClick,
    )
}
