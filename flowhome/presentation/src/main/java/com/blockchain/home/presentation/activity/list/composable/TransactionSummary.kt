package com.blockchain.home.presentation.activity.list.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.tablerow.StyledTableRow
import com.blockchain.componentlib.tablerow.StyledTableRowField
import com.blockchain.home.presentation.activity.list.TransactionStatus

@Composable
fun TransactionSummary(
    status: TransactionStatus,
    iconUrl: String,
    coinIconUrl: String?,
    valueTopStart: String,
    valueTopEnd: String,
    valueBottomStart: String?,
    valueBottomEnd: String?,
) {
    StyledTableRow(
        topStartText = valueTopStart,
        topStartTextStyle = StyledTableRowField.Primary, // todo(othman) must be muted for defi
        bottomStartText = valueBottomStart,
        bottomStartTextStyle = status.bottomStartStyle(),
        topEndText = valueTopEnd,
        topEndTextStyle = status.topEndStyle(),
        bottomEndText = valueBottomEnd,
        bottomEndTextStyle = status.bottomEndStyle(),
        startMainImageUrl = iconUrl,
        startSecondaryImageUrl = coinIconUrl
    )
}

// --------------
// styles
@Composable
fun TransactionStatus.bottomStartStyle(): StyledTableRowField = when (this) {
    TransactionStatus.Confirmed -> {
        StyledTableRowField.Muted()
    }
    is TransactionStatus.Pending -> {
        when (isRbfTransaction) {
            true -> StyledTableRowField.Error
            false -> StyledTableRowField.Muted()
        }
    }
    TransactionStatus.Canceled -> {
        StyledTableRowField.Warning
    }
    TransactionStatus.Declined,
    TransactionStatus.Failed -> {
        StyledTableRowField.Info
    }
}

fun TransactionStatus.topEndStyle(): StyledTableRowField = when (this) {
    is TransactionStatus.Pending,
    TransactionStatus.Confirmed -> {
        StyledTableRowField.Primary
    }
    TransactionStatus.Canceled,
    TransactionStatus.Declined,
    TransactionStatus.Failed -> {
        StyledTableRowField.Muted(strikeThrough = true)
    }
}

@Composable
fun TransactionStatus.bottomEndStyle(): StyledTableRowField {
    val strikeThrough = when (this) {
        is TransactionStatus.Pending,
        TransactionStatus.Confirmed -> {
            false
        }
        TransactionStatus.Canceled,
        TransactionStatus.Declined,
        TransactionStatus.Failed -> {
            true
        }
    }

    return StyledTableRowField.Muted(strikeThrough = strikeThrough)
}

@Preview(showBackground = true)
@Composable
fun PreviewTransactionState() {
    TransactionSummary(
        iconUrl = "Sent Ethereum",
        coinIconUrl = "transactionCoinIcon",
        status = TransactionStatus.Confirmed,
        valueTopStart = "Sent Ethereum",
        valueTopEnd = "-10.00",
        valueBottomStart = "June 14",
        valueBottomEnd = "-0.00893208 ETH"
    )
}
