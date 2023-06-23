package com.blockchain.componentlib.tablerow

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.ChevronRight
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class DefaultTableRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var primaryText by mutableStateOf("")
    var secondaryText by mutableStateOf(null as? String?)
    var paragraphText by mutableStateOf(null as? String?)
    var onClick by mutableStateOf({})
    var tags by mutableStateOf(null as? List<TagViewState>?)
    var endTag by mutableStateOf(null as? TagViewState?)
    var startImageResource: ImageResource by mutableStateOf(ImageResource.None)
    var endImageResource: ImageResource by mutableStateOf(Icons.ChevronRight)
    var primaryTextColor: ComposeColors by mutableStateOf(ComposeColors.Title)
    var secondaryTextColor: ComposeColors by mutableStateOf(ComposeColors.Body)

    @Composable
    override fun Content() {
        DefaultTableRow(
            primaryText = primaryText,
            secondaryText = secondaryText,
            paragraphText = paragraphText,
            onClick = onClick,
            tags = tags,
            endTag = endTag,
            startImageResource = startImageResource,
            endImageResource = endImageResource.run {
                if (this is ImageResource.Local) {
                    withTint(AppColors.body)
                } else {
                    this
                }
            },
            primaryTextColor = primaryTextColor.toComposeColor(),
            secondaryTextColor = secondaryTextColor.toComposeColor()
        )
    }

    fun clearState() {
        primaryText = ""
        secondaryText = null
        paragraphText = null
        onClick = {}
        tags = null
        endTag = null
        startImageResource = ImageResource.None
        endImageResource = ImageResource.Local(
            id = R.drawable.ic_chevron_end,
            contentDescription = null
        )
    }
}
