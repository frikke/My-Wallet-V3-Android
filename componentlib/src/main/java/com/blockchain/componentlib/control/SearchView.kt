package com.blockchain.componentlib.control

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class SearchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var label by mutableStateOf("")
    var onValueChange by mutableStateOf({ _: String -> })
    private var shouldClearInput by mutableStateOf(false)

    fun clearInput() {
        shouldClearInput = true
    }

    @Composable
    override fun Content() {
        AppTheme(setSystemColors = false) {
            AppSurface {
                Search(
                    label = label,
                    onValueChange = onValueChange,
                    clearInput = if (shouldClearInput) {
                        shouldClearInput = false
                        true
                    } else {
                        false
                    }
                )
            }
        }
    }

    fun clearState() {
        label = ""
        onValueChange = { _: String -> }
    }
}
