package com.blockchain.home.presentation.activity.list.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tablerow.FlexibleTableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.home.presentation.activity.list.TransactionStatus

@Composable
fun TransactionSummary(
    status: TransactionStatus,
    iconUrl: String?,
    coinIconUrl: String?,
    valueTopStart: String,
    valueTopEnd: String,
    valueBottomStart: String?,
    valueBottomEnd: String?,
    onClick: () -> Unit
) {
    FlexibleTableRow(
        paddingValues = PaddingValues(AppTheme.dimensions.smallSpacing),
        contentStart = {
            iconUrl?.let {
                val stackedIconPadding = 2.dp

                Box(
                    modifier = Modifier
                        .size(
                            AppTheme.dimensions.standardSpacing + stackedIconPadding // 2 extra to account for verified icon
                        )
                ) {
                    Image(
                        imageResource = ImageResource.Remote(
                            url = iconUrl,
                            shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiSmall),
                            size = AppTheme.dimensions.standardSpacing
                        )
                    )

                    coinIconUrl?.let {
                        Image(
                            modifier = Modifier
                                .align(Alignment.BottomEnd),
                            imageResource = ImageResource.Remote(coinIconUrl)
                        )
                    }
                }
            }
        },
        content = {
            Column(modifier = Modifier.weight(1F)) {
                Text(
                    text = valueTopStart,
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.title
                )

                valueBottomStart?.let {
                    Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))

                    Text(
                        text = valueBottomStart,
                        style = AppTheme.typography.caption1,
                        color = status.bottomStartTextColor()
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = valueTopEnd,
                    style = AppTheme.typography.body2.copy(
                        textDecoration = status.endTextDecoration()
                    ),
                    color = status.topEndTextColor(),
                    textAlign = TextAlign.End,
                )

                valueBottomEnd?.let {
                    Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))

                    Text(
                        text = valueBottomEnd,
                        style = AppTheme.typography.caption1.copy(
                            textDecoration = status.endTextDecoration()
                        ),
                        color = AppTheme.colors.muted,
                        textAlign = TextAlign.End
                    )
                }
            }
        },
        onContentClicked = onClick
    )
}

// --------------
// styles
fun TransactionStatus.endTextDecoration(): TextDecoration? = when (this) {
    is TransactionStatus.Pending,
    TransactionStatus.Settled -> {
        null
    }
    TransactionStatus.Canceled,
    TransactionStatus.Declined,
    TransactionStatus.Failed -> {
        TextDecoration.LineThrough
    }
}

@Composable
fun TransactionStatus.topEndTextColor(): Color = when (this) {
    TransactionStatus.Settled -> {
        AppTheme.colors.title
    }
    is TransactionStatus.Pending,
    TransactionStatus.Canceled,
    TransactionStatus.Declined,
    TransactionStatus.Failed -> {
        AppTheme.colors.muted
    }
}

@Composable
fun TransactionStatus.bottomStartTextColor(): Color = when (this) {
    TransactionStatus.Settled -> {
        AppTheme.colors.muted
    }
    is TransactionStatus.Pending -> {
        when (isRbfTransaction) {
            true -> AppTheme.colors.error
            false -> AppTheme.colors.muted
        }
    }
    TransactionStatus.Canceled -> {
        AppTheme.colors.warning
    }
    TransactionStatus.Declined,
    TransactionStatus.Failed -> {
        Color(0xFFDE0082)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTransactionState() {
    TransactionSummary(
        iconUrl = "Sent Ethereum",
        coinIconUrl = "transactionCoinIcon",
        status = TransactionStatus.Settled,
        valueTopStart = "Sent Ethereum",
        valueTopEnd = "-10.00",
        valueBottomStart = "June 14",
        valueBottomEnd = "-0.00893208 ETH",
        onClick = {}
    )
}
