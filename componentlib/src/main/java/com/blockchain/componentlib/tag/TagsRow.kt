package com.blockchain.componentlib.tag

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class TagView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {
    var tag: TagViewState? by mutableStateOf(null)

    @Composable
    override fun Content() {
        if (isInEditMode) {
            tag = TagViewState(value = "dummy text", type = TagType.Default())
        }

        Tag(
            tag = tag ?: TagViewState(value = "", type = TagType.Default())
        )
    }
}

@Composable
fun Tag(tag: TagViewState) {
    when (tag.type) {
        is TagType.Default -> DefaultTag(text = tag.value, size = tag.type.size, onClick = tag.onClick)
        is TagType.InfoAlt -> InfoAltTag(text = tag.value, size = tag.type.size, onClick = tag.onClick)
        is TagType.Success -> SuccessTag(text = tag.value, size = tag.type.size, onClick = tag.onClick)
        is TagType.Warning -> WarningTag(text = tag.value, size = tag.type.size, onClick = tag.onClick)
        is TagType.Error -> ErrorTag(text = tag.value, size = tag.type.size, onClick = tag.onClick)
    }
}

@Composable
fun TagsRow(
    tags: List<TagViewState>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState())
    ) {
        tags.forEachIndexed { index, tag ->
            Tag(tag)

            if (index != tags.lastIndex) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Preview
@Composable
fun TagRowWithHelp() {
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
