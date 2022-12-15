package com.blockchain.componentlib.tag

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

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
                is TagType.Default -> DefaultTag(text = tag.value, size = tag.type.size, onClick = tag.onClick)
                is TagType.InfoAlt -> InfoAltTag(text = tag.value, size = tag.type.size, onClick = tag.onClick)
                is TagType.Success -> SuccessTag(text = tag.value, size = tag.type.size, onClick = tag.onClick)
                is TagType.Warning -> WarningTag(text = tag.value, size = tag.type.size, onClick = tag.onClick)
                is TagType.Error -> ErrorTag(text = tag.value, size = tag.type.size, onClick = tag.onClick)
            }
            if (index != tags.lastIndex) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Preview
@Composable
fun TagRowWithHelp() {
    AppSurface {
        AppTheme {
            TagsRow(
                tags = listOf(
                    TagViewState("Default", TagType.Default()),
                    TagViewState("InfoAlt", TagType.InfoAlt()),
                    TagViewState("Warning", TagType.Warning()),
                    TagViewState("Success", TagType.Success()),
                    TagViewState("Error", TagType.Error())
                )
            )
        }
    }
}
