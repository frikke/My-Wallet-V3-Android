package com.blockchain.componentlib.tag.button

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Blockchain
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.theme.AppTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Stable
data class TagButtonValue<T>(
    val obj: T,
    val icon: ImageResource.Local? = null,
    val stringVal: String
)

@Composable
fun <T> TagButtonRow(
    modifier: Modifier = Modifier,
    selected: T,
    values: ImmutableList<TagButtonValue<T>>,
    spaceBetween: Dp = AppTheme.dimensions.tinySpacing,
    onClick: (T) -> Unit
) {
    Row(modifier = modifier) {
        values.forEachIndexed { index, value ->
            TagButton(
                modifier = Modifier.weight(1F),
                icon = value.icon,
                text = value.stringVal,
                selected = value.obj == selected,
                onClick = { onClick(value.obj) }
            )

            if (index < values.lastIndex) {
                Spacer(modifier = Modifier.size(spaceBetween))
            }
        }
    }
}

@Preview
@Composable
fun PreviewTagButtonRow() {
    var selected by remember { mutableStateOf("one") }
    TagButtonRow(
        selected = selected,
        values = listOf("one", "two", "three").mapIndexed { index, item ->
            TagButtonValue(item, Icons.Filled.Blockchain.takeIf { index == 2 }, item)
        }.toImmutableList(),
        onClick = { selected = it }
    )
}
