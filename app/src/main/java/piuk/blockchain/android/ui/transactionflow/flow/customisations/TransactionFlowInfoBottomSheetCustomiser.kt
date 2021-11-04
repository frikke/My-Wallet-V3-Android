package piuk.blockchain.android.ui.transactionflow.flow.customisations

import android.content.res.Resources
import android.os.Parcelable
import com.blockchain.coincore.AssetAction
import info.blockchain.balance.AssetInfo
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.customviews.inputview.CurrencyType
import piuk.blockchain.android.ui.transactionflow.engine.TransactionErrorState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionFlowStateInfo
import java.io.Serializable
import java.lang.IllegalArgumentException
import java.math.RoundingMode

interface TransactionFlowInfoBottomSheetCustomiser {
    fun info(state: TransactionFlowStateInfo, input: CurrencyType): TransactionFlowBottomSheetInfo?
}

@Parcelize
data class TransactionFlowBottomSheetInfo(
    val title: String,
    val description: String,
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

class TransactionFlowInfoBottomSheetCustomiserImpl(
    private val resources: Resources
) : TransactionFlowInfoBottomSheetCustomiser {
    override fun info(state: TransactionFlowStateInfo, input: CurrencyType): TransactionFlowBottomSheetInfo? {
        return when (state.errorState) {
            TransactionErrorState.NONE -> null
            TransactionErrorState.ADDRESS_IS_CONTRACT -> null
            TransactionErrorState.INSUFFICIENT_FUNDS -> infoForInsufficientFunds(state)
            TransactionErrorState.BELOW_MIN_LIMIT -> infoForBelowMinLimit(state, input)
            TransactionErrorState.OVER_GOLD_TIER_LIMIT,
            TransactionErrorState.OVER_SILVER_TIER_LIMIT -> null
            else -> null
        }
    }

    private fun infoForBelowMinLimit(
        state: TransactionFlowStateInfo,
        input: CurrencyType
    ): TransactionFlowBottomSheetInfo? {
        val fiatRate = state.fiatRate ?: return null
        val min = state.limits?.minAmount?.toEnteredCurrency(
            input, fiatRate, RoundingMode.CEILING
        ) ?: return null
        return when (state.action) {
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

    private fun infoForInsufficientFunds(state: TransactionFlowStateInfo): TransactionFlowBottomSheetInfo? {
        val balance = state.availableBalance ?: throw IllegalArgumentException("Missing available balance")
        val sendingCurrencyTicker = state.amount.currencyCode
        val action = state.action.toHumanReadable().lowercase()
        when (state.action) {
            AssetAction.Send -> {
                val sendingCryptoCurrency: AssetInfo =
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