package com.blockchain.componentlib.tag

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TagsRow(
    tags: List<TagViewState>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState())
    ) {
        tags.forEachIndexed { index, tag ->
            when (tag.type) {
                TagType.Default -> DefaultTag(text = tag.value)
                TagType.InfoAlt -> InfoAltTag(text = tag.value)
                TagType.Success -> SuccessTag(text = tag.value)
                TagType.Warning -> WarningTag(text = tag.value)
                TagType.Error -> ErrorTag(text = tag.value)
            }
            if (index != tags.lastIndex) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}
