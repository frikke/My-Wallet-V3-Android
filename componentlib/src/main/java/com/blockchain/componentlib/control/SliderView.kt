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

class SliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var value by mutableStateOf(0f)
    var onValueChanged by mutableStateOf({ value: Float -> })
    var sliderEnabled by mutableStateOf(true)

    @Composable
    override fun Content() {
        AppTheme(setSystemColors = false) {
            AppSurface {
                Slider(
                    value = value,
                    onValueChange = {
                        value = it
                        onValueChanged(it)
                    },
                    enabled = sliderEnabled
                )
            }
        }
    }

    fun clearState() {
        value = 0f
        onValueChanged = { value: Float -> }
        sliderEnabled = true
    }
}
