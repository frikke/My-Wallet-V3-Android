package com.blockchain.home.presentation.activity.detail.composable

import androidx.compose.runtime.Composable
import com.blockchain.componentlib.tablerow.KeyTagStyledTableRow
import com.blockchain.componentlib.tablerow.KeyValueStyledTableRow
import com.blockchain.componentlib.tablerow.StyledTableRowField
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.home.presentation.activity.detail.ActivityDetailItemState
import com.blockchain.home.presentation.activity.detail.ValueStyle

@Composable
fun ActivityDetailKeyValue(
    data: ActivityDetailItemState.KeyValue
) {
    when (data.style) {
        ValueStyle.SuccessBadge -> {
            KeyTagStyledTableRow(
                keyText = data.key,
                tag = TagViewState(data.value, TagType.Success()),
                onClick = {}
            )
        }

        else -> {
            KeyValueStyledTableRow(
                keyText = data.key,
                valueText = data.value,
                valueTextStyle = data.style.styledTableRowField(),
                onClick = {}
            )
        }
    }
}

private fun ValueStyle.styledTableRowField(): StyledTableRowField = when (this) {
    ValueStyle.SuccessBadge -> {
        error("success badge should be handled by tags")
    }
    ValueStyle.GreenText -> {
        StyledTableRowField.Success
    }
    ValueStyle.Text -> {
        StyledTableRowField.Primary
    }
}
