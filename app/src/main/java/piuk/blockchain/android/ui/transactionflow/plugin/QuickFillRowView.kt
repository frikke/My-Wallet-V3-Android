package piuk.blockchain.android.ui.transactionflow.plugin

import android.content.Context
import android.util.AttributeSet
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.core.limits.TxLimits
import com.blockchain.presentation.complexcomponents.QuickFillButtonData
import com.blockchain.presentation.complexcomponents.QuickFillDisplayAndAmount
import com.blockchain.presentation.complexcomponents.QuickFillRowView
import info.blockchain.balance.Currency
import info.blockchain.balance.CurrencyType
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import java.math.BigDecimal
import kotlin.math.floor
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.PrefillAmounts
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.engine.domain.model.QuickFillRoundingData
import piuk.blockchain.android.ui.transactionflow.flow.convertFiatToCrypto
import piuk.blockchain.android.ui.transactionflow.flow.customisations.EnterAmountCustomisations

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

        areButtonsTransparent = false
    }

    override fun update(state: TransactionState) {
        state.pendingTx?.limits?.let { limits ->
            state.fiatRate?.let { fiatRate ->
                when (state.currencyType) {
                    CurrencyType.CRYPTO -> {
                        renderCryptoButtons(limits, state, fiatRate)
                    }
                    CurrencyType.FIAT -> {
                        renderFiatButtons(state, fiatRate, limits)
                    }
                    null -> {
                        // do nothing - screen is initialising
                    }
                }
            }
            maxButtonText = customiser.quickFillRowMaxButtonLabel(state)

            onMaxItemClick = { maxAmount ->
                state.fiatRate?.let { rate ->
                    model.process(
                        TransactionIntent.UpdatePrefillAmount(
                            PrefillAmounts(
                                cryptoValue = maxAmount,
                                fiatValue = state.convertBalanceToFiat(maxAmount, rate)
                            )
                        )
                    )
                    analytics.onQuickMaxClicked(
                        state = state,
                        maxAmount = maxAmount
                    )
                }
            }
        }
    }

    private fun renderFiatButtons(
        state: TransactionState,
        fiatRate: ExchangeRate,
        limits: TxLimits
    ) {
        val spendableFiatBalance = state.convertBalanceToFiat(state.maxSpendable, fiatRate)
        val fiatCurrency = spendableFiatBalance.currency

        val listOfAmounts = mutableListOf<Money>()

        val roundingData = state.quickFillRoundingData

        val spendableBalanceWithinLimits = limits.getSpendableBalanceWithinLimits(
            amount = state.maxSpendable,
            currency = fiatCurrency
        )

        if (roundingData.all { it is QuickFillRoundingData.SellSwapRoundingData }) {
            roundingData.forEach {
                val data = it as QuickFillRoundingData.SellSwapRoundingData
                val prefillValue = getRoundedFiatAndCryptoValues(
                    state = state,
                    spendableBalance = spendableBalanceWithinLimits,
                    fiatRate = fiatRate,
                    multiplicationFactor = data.multiplier,
                    roundingValues = data.rounding
                )

                if (limits.isAmountInRange(prefillValue.roundedCryptoValue)) {
                    listOfAmounts.add(prefillValue.roundedFiatValue)
                }
            }
        }

        quickFillButtonData = QuickFillButtonData(
            maxAmount = state.maxSpendable,
            quickFillButtons = listOfAmounts.distinct().map { amount ->
                QuickFillDisplayAndAmount(
                    displayValue = amount.toStringWithSymbol(includeDecimalsWhenWhole = false),
                    amount = amount,
                    position = listOfAmounts.indexOf(amount)
                )
            }
        )

        onQuickFillItemClick = { quickFillData ->
            model.process(
                TransactionIntent.UpdatePrefillAmount(
                    PrefillAmounts(
                        cryptoValue = quickFillData.amount.convertFiatToCrypto(fiatRate, state),
                        fiatValue = quickFillData.amount
                    )
                )
            )
            analytics.onQuickButtonsClicked(
                state = state,
                buttonTapped = quickFillData.amount,
                position = quickFillData.position
            )
        }
    }

    private fun renderCryptoButtons(
        limits: TxLimits,
        state: TransactionState,
        fiatRate: ExchangeRate
    ) {
        val adjustedBalance =
            limits.getSpendableBalanceWithinLimits(state.maxSpendable, state.maxSpendable.currency)

        val listOfAmounts = mutableListOf<QuickFillDisplayAndAmount>()

        val multiplierValues =
            listOf(
                Pair(TWENTY_FIVE_PERCENT, resources.getString(R.string.enter_amount_quickfill_25)),
                Pair(FIFTY_PERCENT, resources.getString(R.string.enter_amount_quickfill_50)),
                Pair(SEVENTY_FIVE_PERCENT, resources.getString(R.string.enter_amount_quickfill_75))
            )

        multiplierValues.forEach { value ->
            val prefillAmount = adjustedBalance.times(value.first)

            if (limits.isAmountInRange(prefillAmount)) {
                listOfAmounts.add(
                    QuickFillDisplayAndAmount(
                        displayValue = value.second,
                        amount = prefillAmount,
                        position = multiplierValues.indexOf(value)
                    )
                )
            }
        }

        quickFillButtonData = QuickFillButtonData(
            maxAmount = state.maxSpendable,
            quickFillButtons = listOfAmounts.distinct()
        )

        onQuickFillItemClick = { quickFillData ->
            model.process(
                TransactionIntent.UpdatePrefillAmount(
                    PrefillAmounts(
                        cryptoValue = quickFillData.amount,
                        fiatValue = state.convertBalanceToFiat(quickFillData.amount, fiatRate)
                    )
                )
            )
            analytics.onQuickButtonsClicked(
                state = state,
                buttonTapped = quickFillData.amount,
                position = quickFillData.position
            )
        }
    }

    private fun getRoundedFiatAndCryptoValues(
        state: TransactionState,
        spendableBalance: Money,
        fiatRate: ExchangeRate,
        multiplicationFactor: Float,
        roundingValues: List<Int>
    ): RoundedFiatAndCryptoValues {
        require(roundingValues.size == 6) { "rounding values missing" }

        val prefillCrypto = spendableBalance.times(multiplicationFactor)
        val prefillFiat = state.convertBalanceToFiat(prefillCrypto, fiatRate)
        val prefillFiatParts = prefillFiat.toStringParts()

        val lowestPrefillRoundedFiat = when (
            prefillFiatParts.major.filterNot {
                it == prefillFiatParts.groupingSeparator
            }.length
        ) {
            0,
            1 -> {
                roundToNearest(prefillFiat, roundingValues[0])
            }
            2 -> {
                roundToNearest(prefillFiat, roundingValues[1])
            }
            3 -> {
                roundToNearest(prefillFiat, roundingValues[2])
            }
            4 -> {
                roundToNearest(prefillFiat, roundingValues[3])
            }
            5 -> {
                roundToNearest(prefillFiat, roundingValues[4])
            }
            else -> {
                roundToNearest(prefillFiat, roundingValues[5])
            }
        }

        val lowestPrefillRoundedCrypto = lowestPrefillRoundedFiat.convertFiatToCrypto(fiatRate, state)

        return RoundedFiatAndCryptoValues(lowestPrefillRoundedFiat, lowestPrefillRoundedCrypto)
    }

    private fun TxLimits.getSpendableBalanceWithinLimits(amount: Money, currency: Currency): Money {
        val isMaxLimited = isAmountOverMax(amount)
        val isMinLimited = isAmountUnderMin(amount)

        return when {
            isMinLimited && isMaxLimited -> Money.fromMajor(currency, BigDecimal.ZERO)
            isMinLimited -> minAmount
            isMaxLimited -> maxAmount
            else -> amount
        }
    }

    private fun roundToNearest(lastAmount: Money, nearest: Int): Money {
        return Money.fromMajor(
            lastAmount.currency, (nearest * (floor(lastAmount.toFloat() / nearest))).toBigDecimal()
        )
    }

    override fun setVisible(isVisible: Boolean) {
        this.visibleIf { isVisible }
    }

    companion object {
        private const val TWENTY_FIVE_PERCENT = 0.25f
        private const val FIFTY_PERCENT = 0.5f
        private const val SEVENTY_FIVE_PERCENT = 0.75f
    }
}

private data class RoundedFiatAndCryptoValues(
    val roundedFiatValue: Money,
    val roundedCryptoValue: Money
)
