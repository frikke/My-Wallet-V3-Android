package com.blockchain.componentlib.charts

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class BalanceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var price by mutableStateOf("$0.00")
    var percentageChangeData by mutableStateOf(PercentageChangeData("$0.00", 0.0, ""))

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                Balance(
                    price = price,
                    percentageChangeData = percentageChangeData,
                )
            }
        }
    }

    fun clearState() {
        price = "$0.00"
        percentageChangeData = PercentageChangeData("$0.00", 0.0, "")
    }
}
