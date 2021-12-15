package com.blockchain.componentlib.pickers

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

class DateRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var dateRowData by mutableStateOf(DateRowData("", ""))

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                DateRow(
                    dateRowData = dateRowData
                )
            }
        }
    }

    fun clearState() {
        dateRowData = DateRowData("", "")
    }
}
