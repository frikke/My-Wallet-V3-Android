package piuk.blockchain.android.ui.transactionflow.plugin

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visibleIf
import info.blockchain.balance.CurrencyType
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import info.blockchain.balance.canConvert
import piuk.blockchain.android.databinding.ViewTxFullscreenFeeAndBalanceBinding
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.customisations.EnterAmountCustomisations

class BalanceAndFeeView @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle), EnterAmountWidget {

    private lateinit var model: TransactionModel
    private lateinit var customiser: EnterAmountCustomisations
    private lateinit var analytics: TxFlowAnalytics

    private val binding: ViewTxFullscreenFeeAndBalanceBinding =
        ViewTxFullscreenFeeAndBalanceBinding.inflate(LayoutInflater.from(context), this, true)

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

        updateMaxGroupAndFees(state)
        updateBalance(state)
    }

    override fun setVisible(isVisible: Boolean) {
        binding.root.visibleIf { isVisible }
    }

    private fun updateBalance(state: TransactionState) {
        with(binding) {
            val availableBalance = state.availableBalance
            maxAvailableValue.text = makeAmountString(availableBalance, state)
            feeForFullAvailableLabel.text = customiser.enterAmountMaxNetworkFeeLabel(state)

            state.pendingTx?.totalBalance?.let {
                totalAvailableValue.text = makeAmountString(it, state)
            }

            totalAvailableValue.visibleIf {
                state.pendingTx?.totalBalance != state.availableBalance &&
                    customiser.shouldDisplayTotalBalance(state)
            }
            totalAvailableLabel.visibleIf {
                state.pendingTx?.totalBalance != state.availableBalance &&
                    customiser.shouldDisplayTotalBalance(state)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun makeAmountString(value: Money, state: TransactionState): String =
        if ((value.isPositive || value.isZero) && state.fiatRate != null) {
            showFiatOrCryptoValues(
                currencyType = state.currencyType ?: (
                    state.pendingTx?.selectedFiat?.let {
                        val defaultMode = customiser.defInputType(state, it).type
                        model.process(TransactionIntent.DisplayModeChanged(defaultMode))
                        defaultMode
                    } ?: CurrencyType.CRYPTO
                    ),
                state.fiatRate,
                value
            )
        } else {
            customiser.enterAmountGetNoBalanceMessage(state)
        }

    private fun showFiatOrCryptoValues(currencyType: CurrencyType, rate: ExchangeRate, value: Money) =
        when (currencyType) {
            CurrencyType.FIAT -> {
                if (rate.canConvert(value)) {
                    rate.convert(value).toStringWithSymbol()
                } else value.toStringWithSymbol()
            }
            CurrencyType.CRYPTO -> value.toStringWithSymbol()
        }

    private fun updateMaxGroupAndFees(state: TransactionState) =
        with(binding) {
            val isPositiveAmount = state.amount.isPositive
            val shouldDisplayFees = customiser.shouldDisplayNetworkFee(state)
            val amountIsPositiveAndHasFees = isPositiveAmount && shouldDisplayFees

            networkFeeLabel.visibleIf { amountIsPositiveAndHasFees }
            networkFeeValue.visibleIf { amountIsPositiveAndHasFees }
            networkFeeArrow.visibleIf { amountIsPositiveAndHasFees }
            feeForFullAvailableLabel.visibleIf { amountIsPositiveAndHasFees }
            feeForFullAvailableValue.visibleIf { amountIsPositiveAndHasFees }

            state.pendingTx?.feeAmount?.let {
                networkFeeValue.text = makeAmountString(it, state)
            }
            state.pendingTx?.feeForFullAvailable?.let {
                feeForFullAvailableValue.text = makeAmountString(it, state)
            }

            with(useMax) {
                val amountIsZeroOrNoFees =
                    !isPositiveAmount || !shouldDisplayFees // in those cases there is room for the Max button
                text = customiser.enterAmountMaxButton(state)
                onClick = {
                    analytics.onMaxClicked(state)
                    model.process(TransactionIntent.UseMaxSpendable)
                }
                visibleIf {
                    amountIsZeroOrNoFees && !customiser.shouldDisableInput(state.errorState)
                }
            }
        }
}
