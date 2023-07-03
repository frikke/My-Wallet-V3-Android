package com.blockchain.componentlib.expandables

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class ExpandableItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var expandableText by mutableStateOf("")
    var buttonTextToExpand by mutableStateOf("")
    var buttonTextToCollapse by mutableStateOf("")
    var numOfVisibleLines: Int = 1

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                ExpandableItem(
                    text = expandableText,
                    numLinesVisible = numOfVisibleLines,
                    textButtonToExpand = buttonTextToExpand,
                    textButtonToCollapse = buttonTextToCollapse
                )
            }
        }
    }
}

@Preview(name = "expandable item", group = "Expandable")
@Composable
fun ExpandablesPreview_expanded() {
    AppTheme {
        AppSurface {
            ExpandableItem(
                text = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum " +
                    "has been the industry's standard dummy text ever since the 1500s, when an unknown printer " +
                    "took a galley of type and scrambled it to make a type specimen book. took a galley of type" +
                    " and scrambled it to make a type specimen book. took a galley of type and scrambled it " +
                    "to make a type specimen book.  ",
                numLinesVisible = Int.MAX_VALUE,
                textButtonToExpand = "Read More",
                textButtonToCollapse = "Read Less"
            )
        }
    }
}

@Preview(name = "collapsed item", group = "Expandable")
@Composable
fun ExpandablesPreview_collapsed() {
    AppTheme {
        AppSurface {
            ExpandableItem(
                text = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum " +
                    "has been the industry's standard dummy text ever since the 1500s, when an unknown printer " +
                    "took a galley of type and scrambled it to make a type specimen book. took a galley of type" +
                    " and scrambled it to make a type specimen book. took a galley of type and scrambled it " +
                    "to make a type specimen book.  ",
                numLinesVisible = 3,
                textButtonToExpand = "Read More",
                textButtonToCollapse = "Read Less"
            )
        }
    }
}
