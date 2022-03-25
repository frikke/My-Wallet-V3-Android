package piuk.blockchain.android.ui.transactionflow.flow.customisations

import android.content.res.Resources
import android.graphics.Typeface
import android.os.Parcelable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import androidx.annotation.StringRes
import com.blockchain.coincore.AssetAction
import com.blockchain.core.eligibility.models.TransactionsLimit
import com.blockchain.core.limits.TxLimit
import com.blockchain.core.limits.TxLimitPeriod
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CurrencyType
import java.io.Serializable
import java.math.RoundingMode
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.transactionflow.engine.TransactionFlowStateInfo

interface TransactionFlowInfoBottomSheetCustomiser {
    fun info(
        info: InfoBottomSheetType,
        state: TransactionFlowStateInfo,
        input: CurrencyType
    ): TransactionFlowBottomSheetInfo?
}

@Parcelize
data class TransactionFlowBottomSheetInfo(
    val title: String,
    val description: CharSequence,
    val action: InfoAction? = null
) : Parcelable

@Parcelize
data class InfoAction(
    val icon: Serializable,
    val title: String,
    val description: String,
    val ctaActionText: String,
    val actionType: InfoActionType
) : Parcelable

enum class InfoActionType {
    BUY, KYC_UPGRADE
}

enum class InfoBottomSheetType {
    INSUFFICIENT_FUNDS,
    BELOW_MIN_LIMIT,
    OVER_MAX_LIMIT,
    ABOVE_MAX_PAYMENT_METHOD_LIMIT,
    TRANSACTIONS_LIMIT
}

class TransactionFlowInfoBottomSheetCustomiserImpl(
    private val resources: Resources
) : TransactionFlowInfoBottomSheetCustomiser {
    override fun info(
        info: InfoBottomSheetType,
        state: TransactionFlowStateInfo,
        input: CurrencyType
    ): TransactionFlowBottomSheetInfo? {
        return when (info) {
            InfoBottomSheetType.INSUFFICIENT_FUNDS -> infoForInsufficientFunds(state)
            InfoBottomSheetType.BELOW_MIN_LIMIT -> infoForBelowMinLimit(state, input)
            InfoBottomSheetType.OVER_MAX_LIMIT -> infoForMaxLimit(state, input)
            InfoBottomSheetType.ABOVE_MAX_PAYMENT_METHOD_LIMIT -> infoForOverMaxPaymentMethodLimit(state, input)
            InfoBottomSheetType.TRANSACTIONS_LIMIT -> infoForTransactionsLimit(state)
        }
    }

    private fun infoForMaxLimit(state: TransactionFlowStateInfo, input: CurrencyType): TransactionFlowBottomSheetInfo? {
        val limits = state.limits
        val maxLimit = (limits.max as? TxLimit.Limited)?.amount ?: throw IllegalStateException(
            "Max limit should be specified for error state ${state.errorState}"
        )
        val effectiveLimit = limits.periodicLimits.find { it.effective } ?: return null

        val exchangeRate = state.fiatRate ?: return null

        val availableAmount = maxLimit.toEnteredCurrency(input, exchangeRate, RoundingMode.FLOOR)
        val effectiveLimitAmount = effectiveLimit.amount.toEnteredCurrency(input, exchangeRate, RoundingMode.FLOOR)

        val limitPeriodText = when (effectiveLimit.period) {
            TxLimitPeriod.DAILY -> resources.getString(R.string.tx_periodic_limit_daily)
            TxLimitPeriod.MONTHLY -> resources.getString(R.string.tx_periodic_limit_monthly)
            TxLimitPeriod.YEARLY -> resources.getString(R.string.tx_periodic_limit_yearly)
        }

        return if (state.limits.suggestedUpgrade != null) {
            // user can be upgraded
            infoForOverMaxLimitWithUpgradeAvailable(state, availableAmount, limitPeriodText, effectiveLimitAmount)
        } else {
            infoForOverMaxLimitWithoutUpgrade(state, availableAmount, limitPeriodText, effectiveLimitAmount)
        }
    }

    private fun infoForOverMaxLimitWithUpgradeAvailable(
        state: TransactionFlowStateInfo,
        availableAmount: String,
        limitPeriodText: String,
        effectiveLimitAmount: String
    ): TransactionFlowBottomSheetInfo? {

        return when (state.action) {
            AssetAction.Send -> TransactionFlowBottomSheetInfo(
                title = resources.getString(R.string.over_your_limit),
                description = infoDescriptionForPeriodicLimits(
                    R.string.send_enter_amount_max_limit_from_custodial_info,
                    effectiveLimitAmount,
                    limitPeriodText,
                    availableAmount
                ),
                action = infoActionForSuggestedUpgrade(state)
            )
            AssetAction.Swap -> TransactionFlowBottomSheetInfo(
                title = resources.getString(R.string.over_your_limit),
                description = infoDescriptionForPeriodicLimits(
                    when (state.sourceAccountType) {
                        AssetCategory.CUSTODIAL -> R.string.swap_enter_amount_max_limit_from_custodial_info
                        AssetCategory.NON_CUSTODIAL -> R.string.swap_enter_amount_max_limit_from_noncustodial_info
                    },
                    effectiveLimitAmount,
                    limitPeriodText,
                    availableAmount
                ),
                action = infoActionForSuggestedUpgrade(state)
            )
            AssetAction.Buy -> TransactionFlowBottomSheetInfo(
                title = resources.getString(R.string.over_your_limit),
                description = infoDescriptionForPeriodicLimits(
                    R.string.buy_enter_amount_max_limit_suggested_tier_upgrade_info,
                    effectiveLimitAmount,
                    limitPeriodText,
                    availableAmount
                ),
                action = infoActionForSuggestedUpgrade(state)
            )
            // No max Limit for FiatDeposit,Sell and WithDraw. Those actions are not supported by Silver users
            AssetAction.Sell,
            AssetAction.Withdraw,
            AssetAction.FiatDeposit -> null
            else -> null
        }
    }

    private fun infoForOverMaxLimitWithoutUpgrade(
        state: TransactionFlowStateInfo,
        availableAmount: String,
        limitPeriodText: String,
        effectiveLimitAmount: String
    ): TransactionFlowBottomSheetInfo? {
        return when (state.action) {
            AssetAction.Send -> TransactionFlowBottomSheetInfo(
                title = resources.getString(R.string.maximum_with_value, availableAmount),
                description = infoDescriptionForPeriodicLimits(
                    R.string.send_enter_amount_max_limit_from_custodial_info,
                    effectiveLimitAmount,
                    limitPeriodText,
                    availableAmount
                )
            )
            AssetAction.Withdraw -> TransactionFlowBottomSheetInfo(
                title = resources.getString(R.string.maximum_with_value, availableAmount),
                description = infoDescriptionForPeriodicLimits(
                    R.string.withdrawal_max_limit_info,
                    effectiveLimitAmount,
                    limitPeriodText,
                    availableAmount
                )
            )
            AssetAction.Swap -> {
                val asset = state.sendingAsset
                require(asset != null)
                TransactionFlowBottomSheetInfo(
                    title = resources.getString(R.string.maximum_with_value, availableAmount),
                    description = infoDescriptionForPeriodicLimits(
                        R.string.max_swap_amount_description,
                        asset.displayTicker,
                        state.receivingAsset.displayTicker,
                        availableAmount
                    )
                )
            }
            AssetAction.Sell -> {
                val asset = state.sendingAsset
                require(asset != null)
                TransactionFlowBottomSheetInfo(
                    title = resources.getString(R.string.maximum_with_value, availableAmount),
                    description = infoDescriptionForPeriodicLimits(
                        R.string.max_sell_amount_description,
                        asset.displayTicker,
                        state.receivingAsset.displayTicker,
                        availableAmount
                    )
                )
            }
            AssetAction.Buy -> TransactionFlowBottomSheetInfo(
                title = resources.getString(R.string.maximum_with_value, availableAmount),
                description = infoDescriptionForPeriodicLimits(
                    R.string.buy_enter_amount_max_limit_without_upgrade_info,
                    effectiveLimitAmount,
                    limitPeriodText,
                    availableAmount
                )
            )
            AssetAction.FiatDeposit -> null
            else -> null
        }
    }

    private fun infoDescriptionForPeriodicLimits(
        @StringRes
        stringRes: Int,
        effectiveLimitAmount: String,
        limitPeriodText: String,
        availableAmount: String
    ): CharSequence {
        val text = resources.getString(stringRes, effectiveLimitAmount, limitPeriodText, availableAmount)
        val spannableStringBuilder = SpannableStringBuilder(text)

        val effectiveLimitAmountStartIndex = text.indexOf(effectiveLimitAmount)
        val limitPeriodTextStartIndex = text.indexOf(limitPeriodText)
        val availableAmountStartIndex = text.indexOf(availableAmount)
        val firstBoldRange =
            effectiveLimitAmountStartIndex..(effectiveLimitAmountStartIndex + effectiveLimitAmount.length)
        val secondBoldRange = limitPeriodTextStartIndex..(limitPeriodTextStartIndex + limitPeriodText.length)
        val thirdBoldRange = availableAmountStartIndex..(availableAmountStartIndex + availableAmount.length)

        spannableStringBuilder.setSpan(
            StyleSpan(Typeface.BOLD), firstBoldRange.first, firstBoldRange.last, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableStringBuilder.setSpan(
            StyleSpan(Typeface.BOLD), secondBoldRange.first, secondBoldRange.last, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannableStringBuilder.setSpan(
            StyleSpan(Typeface.BOLD), thirdBoldRange.first, thirdBoldRange.last, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return spannableStringBuilder
    }

    private fun infoForBelowMinLimit(
        state: TransactionFlowStateInfo,
        input: CurrencyType
    ): TransactionFlowBottomSheetInfo? {
        val fiatRate = state.fiatRate ?: return null
        val min = state.limits.minAmount.toEnteredCurrency(
            input, fiatRate, RoundingMode.CEILING
        )
        return when (state.action) {
            AssetAction.Send -> null // TODO("Missing Designs- Replace once available")
            AssetAction.Withdraw -> {
                return TransactionFlowBottomSheetInfo(
                    title = resources.getString(
                        R.string.minimum_with_value,
                        min
                    ),
                    description = resources.getString(
                        R.string.minimum_withdraw_error_message, min
                    )
                )
            }
            AssetAction.Swap -> return TransactionFlowBottomSheetInfo(
                title = resources.getString(
                    R.string.minimum_with_value,
                    min
                ),
                description = resources.getString(
                    R.string.minimum_swap_error_message, min
                )
            )
            AssetAction.Buy -> return TransactionFlowBottomSheetInfo(
                title = resources.getString(
                    R.string.minimum_with_value,
                    min
                ),
                description = resources.getString(
                    R.string.minimum_buy_error_message, min
                )
            )
            AssetAction.Sell -> return TransactionFlowBottomSheetInfo(
                title = resources.getString(
                    R.string.minimum_with_value,
                    min
                ),
                description = resources.getString(
                    R.string.minimum_sell_error_message, min
                )
            )
            AssetAction.FiatDeposit -> return TransactionFlowBottomSheetInfo(
                title = resources.getString(
                    R.string.minimum_with_value,
                    min
                ),
                description = resources.getString(
                    R.string.minimum_deposit_error_message, min
                )
            )
            else -> null
        }
    }

    private fun infoForOverMaxPaymentMethodLimit(
        state: TransactionFlowStateInfo,
        input: CurrencyType
    ): TransactionFlowBottomSheetInfo {
        val fiatRate = state.fiatRate
        check(fiatRate != null)
        val upgradeAvailable = state.limits.suggestedUpgrade != null
        val title = if (upgradeAvailable) {
            resources.getString(R.string.over_your_limit)
        } else {
            resources.getString(
                R.string.maximum_with_value,
                state.limits.maxAmount.toEnteredCurrency(
                    input, fiatRate, RoundingMode.FLOOR
                )
            )
        }

        return when (state.action) {
            AssetAction.Buy -> {
                val description = if (upgradeAvailable) {
                    resources.getString(
                        R.string.buy_above_payment_method_error_message,
                        state.limits.maxAmount.toEnteredCurrency(input, fiatRate, RoundingMode.FLOOR)
                    )
                } else {
                    resources.getString(
                        R.string.buy_above_payment_method_error_message_without_upgrade,
                        state.limits.maxAmount.toEnteredCurrency(input, fiatRate, RoundingMode.FLOOR),
                        state.amount.toStringWithSymbol()
                    )
                }
                TransactionFlowBottomSheetInfo(
                    title = title,
                    description = description,
                    action = infoActionForSuggestedUpgrade(state)
                )
            }
            AssetAction.FiatDeposit -> {
                val description = if (upgradeAvailable) {
                    resources.getString(
                        R.string.deposit_above_payment_method_error_message,
                        state.limits.maxAmount.toEnteredCurrency(input, fiatRate, RoundingMode.FLOOR)
                    )
                } else {
                    resources.getString(
                        R.string.deposit_above_payment_method_error_message_without_upgrade,
                        state.limits.maxAmount.toEnteredCurrency(input, fiatRate, RoundingMode.FLOOR),
                        state.amount.toStringWithSymbol()
                    )
                }
                TransactionFlowBottomSheetInfo(
                    title = title,
                    description = description,
                    action = infoActionForSuggestedUpgrade(state)
                )
            }
            else -> throw IllegalStateException(
                "Error state ${state.errorState} is not applicable for action ${state.action}"
            )
        }
    }

    private fun infoForTransactionsLimit(state: TransactionFlowStateInfo): TransactionFlowBottomSheetInfo =
        TransactionFlowBottomSheetInfo(
            title = resources.getString(R.string.tx_enter_amount_transactions_limit_info_title),
            description = resources.getString(
                R.string.tx_enter_amount_transactions_limit_info_description,
                (state.transactionsLimit as? TransactionsLimit.Limited)?.maxTransactionsLeft ?: 0
            ),
            action = InfoAction(
                icon = R.drawable.ic_verification_badge,
                title = resources.getString(R.string.tx_enter_amount_transactions_limit_info_action_title),
                description = resources.getString(R.string.tx_enter_amount_transactions_limit_info_action_description),
                ctaActionText = resources.getString(R.string.tx_tier_suggested_upgrade_info_action_cta_button),
                actionType = InfoActionType.KYC_UPGRADE
            )
        )

    private fun infoActionForSuggestedUpgrade(state: TransactionFlowStateInfo): InfoAction? =
        state.limits.suggestedUpgrade?.let {
            val title = when (state.action) {
                AssetAction.Send -> resources.getString(R.string.send_enter_amount_max_limit_info_action_title)

                AssetAction.Swap -> resources.getString(R.string.swap_enter_amount_max_limit_info_action_title)
                AssetAction.Buy -> resources.getString(R.string.buy_enter_amount_max_limit_info_action_title)
                // We should never use those as actions are not permitted for upgradable accounts.
                AssetAction.Withdraw -> ""
                AssetAction.Sell -> ""
                AssetAction.FiatDeposit -> ""
                else -> ""
            }
            InfoAction(
                icon = R.drawable.ic_gold_square,
                description = resources.getString(R.string.tx_tier_suggested_upgrade_info_action_description),
                title = title,
                ctaActionText = resources.getString(R.string.tx_tier_suggested_upgrade_info_action_cta_button),
                actionType = InfoActionType.KYC_UPGRADE
            )
        }

    private fun infoForInsufficientFunds(state: TransactionFlowStateInfo): TransactionFlowBottomSheetInfo? {
        val balance = state.availableBalance ?: throw IllegalArgumentException("Missing available balance")
        val sendingCurrencyTicker = state.amount.currencyCode
        val action = state.action.toHumanReadable().lowercase()
        when (state.action) {
            AssetAction.Send -> {
                val sendingCryptoCurrency =
                    state.sendingAsset ?: throw IllegalArgumentException("Missing source crypto currency")
                val diff = (state.amount - balance).toStringWithSymbol()
                return TransactionFlowBottomSheetInfo(
                    title = resources.getString(R.string.not_enough_funds, sendingCurrencyTicker),
                    description = resources.getString(
                        R.string.not_enough_funds_send, balance.toStringWithSymbol(),
                        diff
                    ),
                    action = InfoAction(
                        icon = sendingCryptoCurrency.logo,
                        description = resources.getString(R.string.tx_title_buy, diff),
                        title = resources.getString(R.string.get_more, sendingCryptoCurrency.displayTicker),
                        ctaActionText = resources.getString(R.string.common_buy),
                        actionType = InfoActionType.BUY
                    )
                )
            }
            AssetAction.Sell -> {
                return TransactionFlowBottomSheetInfo(
                    title = resources.getString(R.string.not_enough_funds, sendingCurrencyTicker),
                    description = resources.getString(
                        R.string.common_actions_not_enough_funds, sendingCurrencyTicker, action,
                        balance.toStringWithSymbol()
                    )
                )
            }
            AssetAction.Withdraw -> {
                return TransactionFlowBottomSheetInfo(
                    title = resources.getString(R.string.not_enough_funds, sendingCurrencyTicker),
                    description = resources.getString(
                        R.string.common_actions_not_enough_funds, sendingCurrencyTicker, action,
                        balance.toStringWithSymbol()
                    )
                )
            }
            AssetAction.Swap -> {
                return TransactionFlowBottomSheetInfo(
                    title = resources.getString(R.string.not_enough_funds, sendingCurrencyTicker),
                    description = resources.getString(
                        R.string.common_actions_not_enough_funds, sendingCurrencyTicker, action,
                        balance.toStringWithSymbol()
                    )
                )
            }
            AssetAction.Buy -> {
                return TransactionFlowBottomSheetInfo(
                    title = resources.getString(R.string.not_enough_funds, sendingCurrencyTicker),
                    description = resources.getString(
                        R.string.common_actions_not_enough_funds, sendingCurrencyTicker, action,
                        balance.toStringWithSymbol()
                    )
                )
            }
            else -> return null
        }
    }

    private fun AssetAction.toHumanReadable(): String {
        return when (this) {
            AssetAction.Buy -> resources.getString(R.string.common_buy)
            AssetAction.Sell -> resources.getString(R.string.common_sell)
            AssetAction.Send -> resources.getString(R.string.common_send)
            AssetAction.Swap -> resources.getString(R.string.common_swap)
            AssetAction.InterestDeposit,
            AssetAction.FiatDeposit -> resources.getString(R.string.common_deposit)
            AssetAction.Withdraw,
            AssetAction.InterestWithdraw -> resources.getString(R.string.common_withdraw)
            else -> throw IllegalArgumentException("Action not supported by this customiser")
        }
    }
}
