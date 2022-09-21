package piuk.blockchain.android.ui.transactionflow.plugin

import android.content.Context
import android.util.AttributeSet
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.presentation.complexcomponents.QuickFillButtonData
import com.blockchain.presentation.complexcomponents.QuickFillRowView
import info.blockchain.balance.CurrencyType
import info.blockchain.balance.Money
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.floor
import kotlin.math.ln
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.PrefillAmounts
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.convertFiatToCrypto
import piuk.blockchain.android.ui.transactionflow.flow.customisations.EnterAmountCustomisations
import timber.log.Timber

class QuickFillRowView @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : QuickFillRowView(ctx, attr, defStyle),
    EnterAmountWidget {

    private lateinit var model: TransactionModel
    private lateinit var customiser: EnterAmountCustomisations
    private lateinit var analytics: TxFlowAnalytics

    override fun initControl(
        model: TransactionModel,
        customiser: EnterAmountCustomisations,
        analytics: TxFlowAnalytics
    ) {
        check(this::model.isInitialized.not()) { "QuickFillTxFlowWidget already initialised" }

        this.model = model
        this.customiser = customiser
        this.analytics = analytics
    }

    override fun update(state: TransactionState) {
        state.pendingTx?.limits?.let { limits ->
            when (state.currencyType) {
                CurrencyType.CRYPTO -> {
                }
                CurrencyType.FIAT -> {
                    state.fiatRate?.let { fiatRate ->
                        val spendableFiatBalance = state.availableBalanceInFiat(state.maxSpendable, fiatRate)
                        val fiatCurrency = spendableFiatBalance.currency

                        val isMaxLimited = limits.isAmountOverMax(state.maxSpendable)
                        val isMinLimited = limits.isAmountUnderMin(state.maxSpendable)

                        val listOfAmounts = mutableListOf<Money>()

                        val adjustedBalance = when {
                            isMinLimited && isMaxLimited -> Money.fromMajor(fiatCurrency, BigDecimal.ZERO)
                            isMinLimited -> limits.minAmount
                            isMaxLimited -> limits.maxAmount
                            else -> state.maxSpendable
                        }

                        val lowestPrefillAmount = roundAndAdjustToNearestFactor(adjustedBalance, 0.25f)

                        if (limits.isAmountInRange(lowestPrefillAmount)) {
                            listOfAmounts.add(state.availableBalanceInFiat(lowestPrefillAmount, fiatRate))
                        }

                        val mediumPrefillAmount = roundAndAdjustToNearestFactor(adjustedBalance, 0.5f)

                        if (limits.isAmountInRange(mediumPrefillAmount)) {
                            listOfAmounts.add(state.availableBalanceInFiat(mediumPrefillAmount, fiatRate))
                        }

                        val largestPrefillAmount = roundAndAdjustToNearestFactor(adjustedBalance, 0.75f)

                        if (limits.isAmountInRange(largestPrefillAmount)) {
                            listOfAmounts.add(state.availableBalanceInFiat(largestPrefillAmount, fiatRate))
                        }

                        quickFillButtonData = QuickFillButtonData(
                            maxAmount = state.maxSpendable,
                            quickFillButtons = listOfAmounts
                        )
                    }
                }
                null -> {
                    // do nothing - screen is initialising
                }
            }

            maxButtonText = customiser.quickFillRowMaxButtonLabel(state)

            onQuickFillItemClick = { quickFillAMount ->
                state.fiatRate?.let { rate ->
                    model.process(
                        TransactionIntent.UpdatePrefillAmount(
                            PrefillAmounts(
                                cryptoValue = quickFillAMount.convertFiatToCrypto(rate, state),
                                fiatValue = quickFillAMount
                            )
                        )
                    )
                }
            }
            onMaxItemClick = { maxAmount ->
                state.fiatRate?.let { rate ->
                    model.process(
                        TransactionIntent.UpdatePrefillAmount(
                            PrefillAmounts(
                                cryptoValue = maxAmount,
                                fiatValue = state.availableBalanceInFiat(maxAmount, rate)
                            )
                        )
                    )
                }
            }
        }
    }

    private fun roundAndAdjustToNearestFactor(amount: Money, multiplyFactor: Float): Money {
        val digits = getDigitCount(amount.toBigInteger())
        Timber.e("----- roundAndAdjustToNearestFactor digits $digits")
        return amount.times(multiplyFactor)
        /*
         1 if balance is single digits,
         10 if balance is double digits,
          25 if 3 digits,
          100 if 4 digits,
          500 if if 5 digits,
           1000, if 6 digits
         */
    }

    private fun getDigitCount(number: BigInteger): Int {
        val factor = ln(2.0) / ln(10.0)
        val digitCount = (factor * number.bitLength() + 1).toInt()
        return if (BigInteger.TEN.pow(digitCount - 1) > number) {
            digitCount - 1
        } else digitCount
    }

    private fun roundToNearest(lastAmount: Money, nearest: Int): Money {
        return Money.fromMajor(
            lastAmount.currency, (nearest * (floor(lastAmount.toFloat() / nearest) + 1)).toBigDecimal()
        )
    }

    override fun setVisible(isVisible: Boolean) {
        this.visibleIf { isVisible }
    }
}
