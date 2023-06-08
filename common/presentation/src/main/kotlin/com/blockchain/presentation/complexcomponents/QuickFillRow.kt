package com.blockchain.presentation.complexcomponents

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.SmallOutlinedButton
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView
import com.blockchain.domain.trade.model.QuickFillRoundingData
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
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
    var areButtonsTransparent by mutableStateOf(true)

    @Composable
    override fun Content() {
        quickFillButtonData?.let {
            AppTheme {
                AppSurface {
                    QuickFillRow(
                        modifier = Modifier.padding(horizontal = AppTheme.dimensions.standardSpacing),
                        quickFillButtonData = it,
                        onQuickFillItemClick = onQuickFillItemClick,
                        onMaxItemClick = onMaxItemClick,
                        maxButtonText = maxButtonText,
                        areButtonsTransparent = areButtonsTransparent
                    )
                }
            }
        }
    }
}

@Composable
fun QuickFillRow(
    modifier: Modifier = Modifier,
    quickFillButtonData: QuickFillButtonData,
    onQuickFillItemClick: (QuickFillDisplayAndAmount) -> Unit,
    onMaxItemClick: (Money) -> Unit,
    maxButtonText: String,
    areButtonsTransparent: Boolean
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        if (quickFillButtonData.quickFillButtons.isNotEmpty()) {
            LazyRow(modifier = Modifier.weight(1f)) {
                items(
                    items = quickFillButtonData.quickFillButtons,
                    itemContent = { item ->
                        SmallOutlinedButton(
                            text = item.displayValue,
                            onClick = {
                                onQuickFillItemClick(item)
                            },
                            modifier = Modifier.padding(
                                end = dimensionResource(com.blockchain.componentlib.R.dimen.smallest_spacing)
                            ),
                            isTransparent = areButtonsTransparent
                        )
                    }
                )
            }
        }
        if (quickFillButtonData.maxAmount.isPositive) {
            SmallOutlinedButton(
                text = maxButtonText,
                onClick = {
                    onMaxItemClick(quickFillButtonData.maxAmount)
                },
                state = ButtonState.Enabled,
                modifier = Modifier
                    .wrapContentSize(Alignment.Center),
                isTransparent = areButtonsTransparent
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    val data = QuickFillButtonData(
        quickFillButtons = listOf(
            QuickFillDisplayAndAmount(
                displayValue = "25%",
                amount = CryptoValue.zero(CryptoCurrency.BTC),
                roundingData = QuickFillRoundingData.SellSwapRoundingData(0.25f, emptyList()),
                position = 0
            ),
            QuickFillDisplayAndAmount(
                displayValue = "50%",
                amount = CryptoValue.zero(CryptoCurrency.BTC),
                roundingData = QuickFillRoundingData.SellSwapRoundingData(0.5f, emptyList()),
                position = 1
            ),
            QuickFillDisplayAndAmount(
                displayValue = "75%",
                amount = CryptoValue.zero(CryptoCurrency.BTC),
                roundingData = QuickFillRoundingData.SellSwapRoundingData(0.75f, emptyList()),
                position = 2
            ),
        ),
        maxAmount = CryptoValue.fromMajor(CryptoCurrency.BTC, 1.0.toBigDecimal())
    )
    QuickFillRow(
        quickFillButtonData = data,
        onQuickFillItemClick = {},
        onMaxItemClick = {},
        maxButtonText = "1.1234567890123457 BTC",
        areButtonsTransparent = false,
    )
}

data class QuickFillButtonData(
    val quickFillButtons: List<QuickFillDisplayAndAmount>,
    val maxAmount: Money
)

data class QuickFillDisplayAndAmount(
    val displayValue: String,
    val amount: Money,
    val roundingData: QuickFillRoundingData,
    val position: Int
)
