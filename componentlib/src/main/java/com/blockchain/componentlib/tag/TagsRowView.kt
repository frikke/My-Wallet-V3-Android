package com.blockchain.componentlib.tag

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class TagsRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var tags by mutableStateOf<List<TagViewState>>(emptyList())

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                TagsRow(tags)
            }
        }
    }

    fun clearState() {
        tags = emptyList()
    }
}
