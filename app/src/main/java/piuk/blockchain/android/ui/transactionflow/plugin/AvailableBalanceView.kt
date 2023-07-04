package piuk.blockchain.android.ui.transactionflow.plugin

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.common.R
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView
import com.blockchain.componentlib.viewextensions.visibleIf
import info.blockchain.balance.CurrencyType
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.customisations.EnterAmountCustomisations

class AvailableBalanceView @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : TxFlowEnterAmountBalanceRowView(ctx, attr, defStyle),
    EnterAmountWidget {

    private lateinit var model: TransactionModel
    private lateinit var customiser: EnterAmountCustomisations
    private lateinit var analytics: TxFlowAnalytics

    override fun initControl(
        model: TransactionModel,
        customiser: EnterAmountCustomisations,
        analytics: TxFlowAnalytics
    ) {
        check(this::model.isInitialized.not()) { "AvailableBalanceView already initialised" }

        this.model = model
        this.customiser = customiser
        this.analytics = analytics
    }

    override fun update(state: TransactionState) {
        labelText = customiser.balanceRowLabel(state)
        state.fiatRate?.let { rate ->
            amountText = when (state.currencyType) {
                CurrencyType.CRYPTO -> state.maxSpendable.toStringWithSymbol()
                CurrencyType.FIAT -> state.convertBalanceToFiat(state.maxSpendable, rate).toStringWithSymbol()
                null -> ""
            }
        }
    }

    fun onClick(onClick: () -> Unit) {
        this.onClick = onClick
    }

    override fun setVisible(isVisible: Boolean) {
        visibleIf { isVisible }
    }
}

open class TxFlowEnterAmountBalanceRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var labelText by mutableStateOf("")
    var amountText by mutableStateOf("")
    var onClick by mutableStateOf({})
    var isTappable by mutableStateOf(false)

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                TxFlowEnterAmountBalanceRow(
                    labelText = labelText,
                    amountText = amountText,
                    onClick = onClick,
                    isTappable = isTappable
                )
            }
        }
    }
}

@Composable
fun TxFlowEnterAmountBalanceRow(
    labelText: String,
    amountText: String,
    onClick: () -> Unit,
    isTappable: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.light)
            .padding(
                horizontal = dimensionResource(id = com.blockchain.componentlib.R.dimen.small_spacing),
                vertical = dimensionResource(com.blockchain.componentlib.R.dimen.tiny_spacing)
            )
            .clickable(enabled = true, onClick = onClick)
    ) {
        SimpleText(
            text = labelText,
            style = ComposeTypographies.Caption1,
            color = ComposeColors.Muted,
            gravity = ComposeGravities.Start
        )

        if (isTappable) {
            Spacer(
                modifier = Modifier.size(
                    width = dimensionResource(id = com.blockchain.componentlib.R.dimen.tiny_spacing),
                    height = dimensionResource(id = com.blockchain.componentlib.R.dimen.smallest_spacing)
                )
            )

            Image(imageResource = ImageResource.Local(com.blockchain.componentlib.R.drawable.ic_question))
        }

        Spacer(
            modifier = Modifier.weight(1f)
        )

        SimpleText(
            text = amountText,
            style = ComposeTypographies.Caption1,
            color = ComposeColors.Muted,
            gravity = ComposeGravities.End
        )
    }
}

@Preview
@Composable
fun enterAmountView_notTappable() {
    AppTheme {
        AppSurface {
            TxFlowEnterAmountBalanceRow(labelText = "test", amountText = "12345$", onClick = {})
        }
    }
}
