package piuk.blockchain.android.ui.transactionflow.plugin

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visibleIf
import piuk.blockchain.android.databinding.ViewTxFlowSmallBalanceBinding
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.customisations.EnterAmountCustomisations

class SmallBalanceView @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle), EnterAmountWidget {

    private lateinit var model: TransactionModel
    private lateinit var customiser: EnterAmountCustomisations
    private lateinit var analytics: TxFlowAnalytics

    private val binding: ViewTxFlowSmallBalanceBinding =
        ViewTxFlowSmallBalanceBinding.inflate(LayoutInflater.from(context), this, true)

    override fun initControl(
        model: TransactionModel,
        customiser: EnterAmountCustomisations,
        analytics: TxFlowAnalytics
    ) {
        check(this::model.isInitialized.not()) { "Control already initialised" }

        this.model = model
        this.customiser = customiser
        this.analytics = analytics

        with(binding.useMax) {
            gone()
        }
    }

    override fun update(state: TransactionState) {
        check(::model.isInitialized) { "Control not initialised" }

        updateSendMax(state)
        updateBalance(state)
    }

    override fun setVisible(isVisible: Boolean) {
        binding.root.visibleIf { isVisible }
    }

    @SuppressLint("SetTextI18n")
    private fun updateBalance(state: TransactionState) {
        val availableBalance = state.availableBalance
        if (availableBalance.isPositive || availableBalance.isZero) {
            state.fiatRate?.let { rate ->
                binding.maxAvailableValue.text =
                    "${rate.convert(availableBalance).toStringWithSymbol()} " +
                    "(${availableBalance.toStringWithSymbol()})"
            }
        }
    }

    private fun updateSendMax(state: TransactionState) =
        with(binding.useMax) {
            text = customiser.enterAmountMaxButton(state)
            onClick = {
                analytics.onMaxClicked(state)
                model.process(TransactionIntent.UseMaxSpendable)
            }
            visibleIf { !customiser.shouldDisableInput(state.errorState) }
        }
}
