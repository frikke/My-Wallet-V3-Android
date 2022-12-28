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

class OutlinedSearchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var prePopulatedText by mutableStateOf("")
    var placeholder by mutableStateOf("")
    var readOnly by mutableStateOf(false)
    var showCancelButton by mutableStateOf(false)
    var onValueChange by mutableStateOf({ _: String -> })

    @Composable
    override fun Content() {
        AppTheme(setSystemColors = false) {
            AppSurface {
                if (showCancelButton) {
                    CancelableOutlinedSearch(
                        prePopulatedText = prePopulatedText,
                        placeholder = placeholder,
                        readOnly = readOnly,
                        onValueChange = onValueChange
                    )
                } else {
                    NonCancelableOutlinedSearch(
                        prePopulatedText = prePopulatedText,
                        placeholder = placeholder,
                        readOnly = readOnly,
                        onValueChange = onValueChange
                    )
                }
            }
        }
    }
}
