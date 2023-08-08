package piuk.blockchain.android.ui.transactionflow.flow.customisations

import android.content.res.Resources
import android.graphics.Typeface
import android.os.Parcelable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import androidx.annotation.StringRes
import com.blockchain.coincore.AssetAction
import com.blockchain.core.limits.TxLimit
import com.blockchain.core.limits.TxLimitPeriod
import com.blockchain.domain.eligibility.model.TransactionsLimit
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CurrencyType
import java.io.Serializable
import java.math.RoundingMode
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.transactionflow.engine.TransactionFlowStateInfo

interface TransactionFlowInfoBottomSheetCustomiser {
    fun info(
        type: InfoBottomSheetType,
        state: TransactionFlowStateInfo,
        input: CurrencyType
    ): TransactionFlowBottomSheetInfo?
}

@Parcelize
data class TransactionFlowBottomSheetInfo(
    val type: InfoBottomSheetType,
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
        type: InfoBottomSheetType,
        state: TransactionFlowStateInfo,
        input: CurrencyType
    ): TransactionFlowBottomSheetInfo? {
        return when (type) {
            InfoBottomSheetType.INSUFFICIENT_FUNDS -> infoForInsufficientFunds(type, state)
            InfoBottomSheetType.BELOW_MIN_LIMIT -> infoForBelowMinLimit(type, state, input)
            InfoBottomSheetType.OVER_MAX_LIMIT -> infoForMaxLimit(type, state, input)
            InfoBottomSheetType.ABOVE_MAX_PAYMENT_METHOD_LIMIT -> infoForOverMaxPaymentMethodLimit(type, state, input)
            InfoBottomSheetType.TRANSACTIONS_LIMIT -> infoForTransactionsLimit(type, state)
        }
    }

    private fun infoForMaxLimit(
        type: InfoBottomSheetType,
        state: TransactionFlowStateInfo,
        input: CurrencyType
    ): TransactionFlowBottomSheetInfo? {
        val limits = state.limits
        val maxLimit = (limits.max as? TxLimit.Limited)?.amount ?: throw IllegalStateException(
            "Max limit should be specified for error state ${state.errorState}"
        )
        val effectiveLimit = limits.periodicLimits.find { it.effective } ?: return null

        val exchangeRate = state.fiatRate ?: return null

        val availableAmount = maxLimit.toEnteredCurrency(input, exchangeRate, RoundingMode.FLOOR)
        val effectiveLimitAmount = effectiveLimit.amount.toEnteredCurrency(input, exchangeRate, RoundingMode.FLOOR)

        val limitPeriodText = when (effectiveLimit.period) {
            TxLimitPeriod.DAILY -> resources.getString(com.blockchain.stringResources.R.string.tx_periodic_limit_daily)
            TxLimitPeriod.MONTHLY -> resources.getString(
                com.blockchain.stringResources.R.string.tx_periodic_limit_monthly
            )

            TxLimitPeriod.YEARLY -> resources.getString(
                com.blockchain.stringResources.R.string.tx_periodic_limit_yearly
            )
        }

        return if (state.limits.suggestedUpgrade != null) {
            // user can be upgraded
            infoForOverMaxLimitWithUpgradeAvailable(type, state, availableAmount, limitPeriodText, effectiveLimitAmount)
        } else {
            infoForOverMaxLimitWithoutUpgrade(type, state, availableAmount, limitPeriodText, effectiveLimitAmount)
        }
    }

    private fun infoForOverMaxLimitWithUpgradeAvailable(
        type: InfoBottomSheetType,
        state: TransactionFlowStateInfo,
        availableAmount: String,
        limitPeriodText: String,
        effectiveLimitAmount: String
    ): TransactionFlowBottomSheetInfo? {
        return when (state.action) {
            AssetAction.Send -> TransactionFlowBottomSheetInfo(
                type = type,
                title = resources.getString(com.blockchain.stringResources.R.string.over_your_limit),
                description = infoDescriptionForPeriodicLimits(
                    com.blockchain.stringResources.R.string.send_enter_amount_max_limit_from_custodial_info,
                    effectiveLimitAmount,
                    limitPeriodText,
                    availableAmount
                ),
                action = infoActionForSuggestedUpgrade(state)
            )

            AssetAction.Swap -> TransactionFlowBottomSheetInfo(
                type = type,
                title = resources.getString(com.blockchain.stringResources.R.string.over_your_limit),
                description = infoDescriptionForPeriodicLimits(
                    when (state.sourceAccountType) {
                        AssetCategory.TRADING ->
                            com.blockchain.stringResources.R.string.swap_enter_amount_max_limit_from_custodial_info
                        AssetCategory.NON_CUSTODIAL ->
                            com.blockchain.stringResources.R.string.swap_enter_amount_max_limit_from_noncustodial_info
                        AssetCategory.DELEGATED_NON_CUSTODIAL ->
                            com.blockchain.stringResources.R.string.swap_enter_amount_max_limit_from_noncustodial_info

                        AssetCategory.INTEREST -> throw IllegalStateException("Cannot swap an interest account")
                    },
                    effectiveLimitAmount,
                    limitPeriodText,
                    availableAmount
                ),
                action = infoActionForSuggestedUpgrade(state)
            )

            AssetAction.Buy -> TransactionFlowBottomSheetInfo(
                type = type,
                title = resources.getString(com.blockchain.stringResources.R.string.over_your_limit),
                description = infoDescriptionForPeriodicLimits(
                    com.blockchain.stringResources.R.string.buy_enter_amount_max_limit_suggested_tier_upgrade_info,
                    effectiveLimitAmount,
                    limitPeriodText,
                    availableAmount
                ),
                action = infoActionForSuggestedUpgrade(state)
            )
            // No max Limit for FiatDeposit,Sell and WithDraw. Those actions are not supported by Silver users
            AssetAction.Sell,
            AssetAction.FiatWithdraw,
            AssetAction.FiatDeposit -> null

            else -> null
        }
    }

    private fun infoForOverMaxLimitWithoutUpgrade(
        type: InfoBottomSheetType,
        state: TransactionFlowStateInfo,
        availableAmount: String,
        limitPeriodText: String,
        effectiveLimitAmount: String
    ): TransactionFlowBottomSheetInfo? {
        return when (state.action) {
            AssetAction.Send -> TransactionFlowBottomSheetInfo(
                type = type,
                title = resources.getString(
                    com.blockchain.stringResources.R.string.maximum_with_value, availableAmount
                ),
                description = infoDescriptionForPeriodicLimits(
                    com.blockchain.stringResources.R.string.send_enter_amount_max_limit_from_custodial_info,
                    effectiveLimitAmount,
                    limitPeriodText,
                    availableAmount
                )
            )

            AssetAction.FiatWithdraw -> TransactionFlowBottomSheetInfo(
                type = type,
                title = resources.getString(
                    com.blockchain.stringResources.R.string.maximum_with_value, availableAmount
                ),
                description = infoDescriptionForPeriodicLimits(
                    com.blockchain.stringResources.R.string.withdrawal_max_limit_info,
                    effectiveLimitAmount,
                    limitPeriodText,
                    availableAmount
                )
            )

            AssetAction.Swap -> {
                val asset = state.sendingAsset
                require(asset != null)
                TransactionFlowBottomSheetInfo(
                    type = type,
                    title = resources.getString(
                        com.blockchain.stringResources.R.string.maximum_with_value,
                        availableAmount
                    ),
                    description = infoDescriptionForPeriodicLimits(
                        com.blockchain.stringResources.R.string.max_swap_amount_description,
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
                    type = type,
                    title = resources.getString(
                        com.blockchain.stringResources.R.string.maximum_with_value,
                        availableAmount
                    ),
                    description = infoDescriptionForPeriodicLimits(
                        com.blockchain.stringResources.R.string.max_sell_amount_description,
                        asset.displayTicker,
                        state.receivingAsset.displayTicker,
                        availableAmount
                    )
                )
            }

            AssetAction.Buy -> TransactionFlowBottomSheetInfo(
                type = type,
                title = resources.getString(
                    com.blockchain.stringResources.R.string.maximum_with_value, availableAmount
                ),
                description = infoDescriptionForPeriodicLimits(
                    com.blockchain.stringResources.R.string.buy_enter_amount_max_limit_without_upgrade_info,
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
            StyleSpan(Typeface.BOLD),
            firstBoldRange.first,
            firstBoldRange.last,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableStringBuilder.setSpan(
            StyleSpan(Typeface.BOLD),
            secondBoldRange.first,
            secondBoldRange.last,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannableStringBuilder.setSpan(
            StyleSpan(Typeface.BOLD),
            thirdBoldRange.first,
            thirdBoldRange.last,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return spannableStringBuilder
    }

    private fun infoForBelowMinLimit(
        type: InfoBottomSheetType,
        state: TransactionFlowStateInfo,
        input: CurrencyType
    ): TransactionFlowBottomSheetInfo? {
        val fiatRate = state.fiatRate ?: return null
        val min = state.limits.minAmount.toEnteredCurrency(
            input,
            fiatRate,
            RoundingMode.CEILING
        )
        return when (state.action) {
            AssetAction.Send -> null // TODO("Missing Designs- Replace once available")
            AssetAction.FiatWithdraw -> {
                return TransactionFlowBottomSheetInfo(
                    type = type,
                    title = resources.getString(
                        com.blockchain.stringResources.R.string.minimum_with_value,
                        min
                    ),
                    description = resources.getString(
                        com.blockchain.stringResources.R.string.minimum_withdraw_error_message,
                        min
                    )
                )
            }

            AssetAction.Swap -> return TransactionFlowBottomSheetInfo(
                type = type,
                title = resources.getString(
                    com.blockchain.stringResources.R.string.minimum_with_value,
                    min
                ),
                description = resources.getString(
                    com.blockchain.stringResources.R.string.minimum_swap_error_message,
                    min
                )
            )

            AssetAction.Buy -> return TransactionFlowBottomSheetInfo(
                type = type,
                title = resources.getString(
                    com.blockchain.stringResources.R.string.minimum_with_value,
                    min
                ),
                description = resources.getString(
                    com.blockchain.stringResources.R.string.minimum_buy_error_message,
                    min
                )
            )

            AssetAction.Sell -> return TransactionFlowBottomSheetInfo(
                type = type,
                title = resources.getString(
                    com.blockchain.stringResources.R.string.minimum_with_value,
                    min
                ),
                description = resources.getString(
                    com.blockchain.stringResources.R.string.minimum_sell_error_message,
                    min
                )
            )

            AssetAction.FiatDeposit -> return TransactionFlowBottomSheetInfo(
                type = type,
                title = resources.getString(
                    com.blockchain.stringResources.R.string.minimum_with_value,
                    min
                ),
                description = resources.getString(
                    com.blockchain.stringResources.R.string.minimum_deposit_error_message,
                    min
                )
            )

            else -> null
        }
    }

    private fun infoForOverMaxPaymentMethodLimit(
        type: InfoBottomSheetType,
        state: TransactionFlowStateInfo,
        input: CurrencyType
    ): TransactionFlowBottomSheetInfo? {
        val fiatRate = state.fiatRate ?: return null
        val upgradeAvailable = state.limits.suggestedUpgrade != null
        val title = if (upgradeAvailable) {
            resources.getString(com.blockchain.stringResources.R.string.over_your_limit)
        } else {
            resources.getString(
                com.blockchain.stringResources.R.string.maximum_with_value,
                state.limits.maxAmount.toEnteredCurrency(
                    input,
                    fiatRate,
                    RoundingMode.FLOOR
                )
            )
        }

        return when (state.action) {
            AssetAction.Buy -> {
                val description = if (upgradeAvailable) {
                    resources.getString(
                        com.blockchain.stringResources.R.string.buy_above_payment_method_error_message,
                        state.limits.maxAmount.toEnteredCurrency(input, fiatRate, RoundingMode.FLOOR)
                    )
                } else {
                    resources.getString(
                        com.blockchain.stringResources.R.string.buy_above_payment_method_error_message_without_upgrade,
                        state.limits.maxAmount.toEnteredCurrency(input, fiatRate, RoundingMode.FLOOR),
                        state.amount.toStringWithSymbol()
                    )
                }
                TransactionFlowBottomSheetInfo(
                    type = type,
                    title = title,
                    description = description,
                    action = infoActionForSuggestedUpgrade(state)
                )
            }

            AssetAction.FiatDeposit -> {
                val description = if (upgradeAvailable) {
                    resources.getString(
                        com.blockchain.stringResources.R.string.deposit_above_payment_method_error_message,
                        state.limits.maxAmount.toEnteredCurrency(input, fiatRate, RoundingMode.FLOOR)
                    )
                } else {
                    resources.getString(
                        com.blockchain.stringResources.R
                            .string.deposit_above_payment_method_error_message_without_upgrade,
                        state.limits.maxAmount.toEnteredCurrency(input, fiatRate, RoundingMode.FLOOR),
                        state.amount.toStringWithSymbol()
                    )
                }
                TransactionFlowBottomSheetInfo(
                    type = type,
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

    private fun infoForTransactionsLimit(
        type: InfoBottomSheetType,
        state: TransactionFlowStateInfo
    ): TransactionFlowBottomSheetInfo =
        TransactionFlowBottomSheetInfo(
            type = type,
            title = resources.getString(
                com.blockchain.stringResources.R.string.tx_enter_amount_transactions_limit_info_title
            ),
            description = resources.getString(
                com.blockchain.stringResources.R.string.tx_enter_amount_transactions_limit_info_description,
                (state.transactionsLimit as? TransactionsLimit.Limited)?.maxTransactionsLeft ?: 0
            ),
            action = InfoAction(
                icon = com.blockchain.common.R.drawable.ic_verification_badge,
                title = resources.getString(
                    com.blockchain.stringResources.R.string.tx_enter_amount_transactions_limit_info_action_title
                ),
                description = resources.getString(
                    com.blockchain.stringResources.R.string.tx_enter_amount_transactions_limit_info_action_description
                ),
                ctaActionText = resources.getString(
                    com.blockchain.stringResources.R.string.tx_tier_suggested_upgrade_info_action_cta_button
                ),
                actionType = InfoActionType.KYC_UPGRADE
            )
        )

    private fun infoActionForSuggestedUpgrade(state: TransactionFlowStateInfo): InfoAction? =
        state.limits.suggestedUpgrade?.let {
            val title = when (state.action) {
                AssetAction.Send -> resources.getString(
                    com.blockchain.stringResources.R.string.send_enter_amount_max_limit_info_action_title
                )

                AssetAction.Swap -> resources.getString(
                    com.blockchain.stringResources.R.string.swap_enter_amount_max_limit_info_action_title
                )

                AssetAction.Buy -> resources.getString(
                    com.blockchain.stringResources.R.string.buy_enter_amount_max_limit_info_action_title
                )
                // We should never use those as actions are not permitted for upgradable accounts.
                AssetAction.FiatWithdraw -> ""
                AssetAction.Sell -> ""
                AssetAction.FiatDeposit -> ""
                else -> ""
            }
            InfoAction(
                icon = R.drawable.ic_gold_square,
                description = resources.getString(
                    com.blockchain.stringResources.R.string.tx_tier_suggested_upgrade_info_action_description
                ),
                title = title,
                ctaActionText = resources.getString(
                    com.blockchain.stringResources.R.string.tx_tier_suggested_upgrade_info_action_cta_button
                ),
                actionType = InfoActionType.KYC_UPGRADE
            )
        }

    private fun infoForInsufficientFunds(
        type: InfoBottomSheetType,
        state: TransactionFlowStateInfo
    ): TransactionFlowBottomSheetInfo? {
        val balance = state.availableBalance ?: throw IllegalArgumentException("Missing available balance")
        val sendingCurrencyTicker = state.amount.currencyCode
        val action = state.action.toHumanReadable().lowercase()
        when (state.action) {
            AssetAction.Send -> {
                val sendingCryptoCurrency =
                    state.sendingAsset ?: throw IllegalArgumentException("Missing source crypto currency")
                val diff = (state.amount - balance).toStringWithSymbol()
                return TransactionFlowBottomSheetInfo(
                    type = type,
                    title = resources.getString(
                        com.blockchain.stringResources.R.string.not_enough_funds,
                        sendingCurrencyTicker
                    ),
                    description = resources.getString(
                        com.blockchain.stringResources.R.string.not_enough_funds_send,
                        balance.toStringWithSymbol(),
                        diff
                    ),
                    action = InfoAction(
                        icon = sendingCryptoCurrency.logo,
                        description = resources.getString(com.blockchain.stringResources.R.string.tx_title_buy, diff),
                        title = resources.getString(
                            com.blockchain.stringResources.R.string.get_more,
                            sendingCryptoCurrency.displayTicker
                        ),
                        ctaActionText = resources.getString(com.blockchain.stringResources.R.string.common_buy),
                        actionType = InfoActionType.BUY
                    )
                )
            }

            AssetAction.Sell -> {
                return TransactionFlowBottomSheetInfo(
                    type = type,
                    title = resources.getString(
                        com.blockchain.stringResources.R.string.not_enough_funds,
                        sendingCurrencyTicker
                    ),
                    description = resources.getString(
                        com.blockchain.stringResources.R.string.common_actions_not_enough_funds,
                        sendingCurrencyTicker,
                        action,
                        balance.toStringWithSymbol()
                    )
                )
            }

            AssetAction.FiatWithdraw -> {
                return TransactionFlowBottomSheetInfo(
                    type = type,
                    title = resources.getString(
                        com.blockchain.stringResources.R.string.not_enough_funds,
                        sendingCurrencyTicker
                    ),
                    description = resources.getString(
                        com.blockchain.stringResources.R.string.common_actions_not_enough_funds,
                        sendingCurrencyTicker,
                        action,
                        balance.toStringWithSymbol()
                    )
                )
            }

            AssetAction.Swap -> {
                return TransactionFlowBottomSheetInfo(
                    type = type,
                    title = resources.getString(
                        com.blockchain.stringResources.R.string.not_enough_funds,
                        sendingCurrencyTicker
                    ),
                    description = resources.getString(
                        com.blockchain.stringResources.R.string.common_actions_not_enough_funds,
                        sendingCurrencyTicker,
                        action,
                        balance.toStringWithSymbol()
                    )
                )
            }

            AssetAction.Buy -> {
                return TransactionFlowBottomSheetInfo(
                    type = type,
                    title = resources.getString(
                        com.blockchain.stringResources.R.string.not_enough_funds,
                        sendingCurrencyTicker
                    ),
                    description = resources.getString(
                        com.blockchain.stringResources.R.string.common_actions_not_enough_funds,
                        sendingCurrencyTicker,
                        action,
                        balance.toStringWithSymbol()
                    )
                )
            }

            else -> return null
        }
    }

    private fun AssetAction.toHumanReadable(): String {
        return when (this) {
            AssetAction.Buy -> resources.getString(com.blockchain.stringResources.R.string.common_buy)
            AssetAction.Sell -> resources.getString(com.blockchain.stringResources.R.string.common_sell)
            AssetAction.Send -> resources.getString(com.blockchain.stringResources.R.string.common_send)
            AssetAction.Swap -> resources.getString(com.blockchain.stringResources.R.string.common_swap)
            AssetAction.InterestDeposit,
            AssetAction.StakingDeposit,
            AssetAction.ActiveRewardsDeposit,
            AssetAction.FiatDeposit -> resources.getString(com.blockchain.stringResources.R.string.common_deposit)

            AssetAction.FiatWithdraw,
            AssetAction.StakingWithdraw,
            AssetAction.ActiveRewardsWithdraw,
            AssetAction.InterestWithdraw -> resources.getString(com.blockchain.stringResources.R.string.common_withdraw)

            else -> throw IllegalArgumentException("Action not supported by this customiser")
        }
    }
}
