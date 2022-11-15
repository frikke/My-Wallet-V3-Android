package com.blockchain.componentlib.tablerow.custom

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tag.TagType

/**
 * styles
 */
sealed interface ViewStyle {
    data class TextStyle(
        val style: androidx.compose.ui.text.TextStyle,
        val color: Color,
        val strikeThrough: Boolean = false
    ) : ViewStyle
}

/**
 * types
 */
sealed interface ViewType {
    data class Text(
        val value: String,
        val style: ViewStyle.TextStyle
    ) : ViewType

    data class Tag(
        val value: String,
        val style: TagType
    ) : ViewType

    object Unknown : ViewType
}

fun ViewStyle.TextStyle.textDecoration(): TextDecoration {
    return if (strikeThrough) {
        TextDecoration.LineThrough
    } else {
        TextDecoration.None
    }
}

sealed interface StackedIcon {
    data class SmallTag(
        val main: ImageResource,
        val tag: ImageResource
    ) : StackedIcon

    data class OverlappingPair(
        val front: ImageResource,
        val back: ImageResource
    ) : StackedIcon

    data class SingleIcon(
        val icon: ImageResource
    ) : StackedIcon

    object None : StackedIcon
}
