package com.blockchain.componentlib.sectionheader

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class LargeSectionHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var sectionHeader: LargeSectionHeaderType by mutableStateOf(LargeSectionHeaderType.Default(""))

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                LargeSectionHeader(sectionHeader)
            }
        }
    }

    fun clearState() {
        sectionHeader = LargeSectionHeaderType.Default("")
    }
}
