package com.blockchain.componentlib.control

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

class SliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var value by mutableStateOf(0f)
    var onValueChanged by mutableStateOf({ value: Float -> })
    var sliderEnabled by mutableStateOf(true)

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                Slider(
                    value = value,
                    onValueChange = {
                        value = it
                        onValueChanged(it)
                    },
                    enabled = sliderEnabled,
                )
            }
        }
    }
}
