package com.blockchain.presentation.complexcomponents

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.blockchain.common.R
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.SmallMinimalButton
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView
import info.blockchain.balance.Money

open class QuickFillRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var quickFillButtonData by mutableStateOf<QuickFillButtonData?>(null)
    var onQuickFillItemClick by mutableStateOf<(QuickFillDisplayAndAmount) -> Unit>({})
    var onMaxItemClick by mutableStateOf<(Money) -> Unit>({})
    var maxButtonText by mutableStateOf("")

    @Composable
    override fun Content() {
        quickFillButtonData?.let {
            AppTheme {
                AppSurface {
                    QuickFillRow(
                        quickFillButtonData = it,
                        onQuickFillItemClick = onQuickFillItemClick,
                        onMaxItemClick = onMaxItemClick,
                        maxButtonText = maxButtonText
                    )
                }
            }
        }
    }
}

@Composable
fun QuickFillRow(
    quickFillButtonData: QuickFillButtonData,
    onQuickFillItemClick: (QuickFillDisplayAndAmount) -> Unit,
    onMaxItemClick: (Money) -> Unit,
    maxButtonText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.standard_spacing))
    ) {
        LazyRow(modifier = Modifier.weight(1f)) {
            items(
                items = quickFillButtonData.quickFillButtons,
                itemContent = { item ->
                    SmallMinimalButton(
                        text = item.displayValue,
                        onClick = {
                            onQuickFillItemClick(item)
                        },
                        modifier = Modifier.padding(end = dimensionResource(R.dimen.smallest_spacing))
                    )
                }
            )
        }
        if (quickFillButtonData.maxAmount.isPositive) {
            SmallMinimalButton(
                text = maxButtonText,
                onClick = {
                    onMaxItemClick(quickFillButtonData.maxAmount)
                },
                state = ButtonState.Enabled,
                modifier = Modifier
                    .wrapContentSize(Alignment.Center)
            )
        }
    }
}

data class QuickFillButtonData(
    val quickFillButtons: List<QuickFillDisplayAndAmount>,
    val maxAmount: Money
)

data class QuickFillDisplayAndAmount(
    val displayValue: String,
    val amount: Money,
    val position: Int
)
