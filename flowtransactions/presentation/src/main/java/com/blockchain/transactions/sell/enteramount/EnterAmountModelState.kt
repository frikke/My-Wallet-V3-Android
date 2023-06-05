package com.blockchain.transactions.sell.enteramount

import com.blockchain.coincore.FiatAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.core.limits.TxLimit
import com.blockchain.core.limits.TxLimits
import com.blockchain.domain.trade.model.QuickFillRoundingData
import com.blockchain.extensions.safeLet
import com.blockchain.transactions.common.CombinedSourceNetworkFees
import com.blockchain.transactions.common.CryptoAccountWithBalance
import com.blockchain.transactions.common.OnChainDepositInputValidationError
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.CurrencyType
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import java.io.Serializable

data class EnterAmountModelState(
    val fromAccount: CryptoAccountWithBalance? = null,
    val toAccount: FiatAccount? = null,
    val secondPassword: String? = null,

    val isLoadingLimits: Boolean = false,
    val productLimits: TxLimits? = null,

    val quickFillRoundingData: List<QuickFillRoundingData.SellSwapRoundingData>? = null,

    val sourceToTargetExchangeRate: ExchangeRate? = null,
    val sourceNetworkFees: CombinedSourceNetworkFees? = null,
    val depositEngineInputValidationError: OnChainDepositInputValidationError? = null,

    val fiatAmount: FiatValue? = null,
    val fiatAmountUserInput: String = "",
    val cryptoAmount: CryptoValue? = null,
    val cryptoAmountUserInput: String = "",

    val selectedInput: CurrencyType = CurrencyType.FIAT,

    val getDepositNetworkFeeError: Exception? = null,
    val getQuotePriceError: Exception? = null,
    val fatalError: SellEnterAmountFatalError? = null
) : ModelState {
    /**
     * A word on limits and fees, the actual Minimum order Limit is [productLimits], eg. 5 XLM, for us to comply with this min order value
     * we'll have to add the [sourceNetworkFees.feeForAmount], eg. 2 XLM, so the min input limit is now 5 + 2 XLM, this behaviour is not explicit
     * in the [minLimit] calculation below because the [cryptoAmount] is the final order amount, the [sourceNetworkFees.feeForAmount] is completely
     * separate and will only be "charged" when the user performs the NC deposit into BCDC account address, hence why it's actually
     * part of the [maxLimit] instead, to ensure whatever the user inputs in [cryptoAmount] never exceeds [spendableBalance].
     */
    val minLimit: CryptoValue?
        get() {
            val fromAccount = fromAccount ?: return null
            val sourceAsset = fromAccount.account.currency

            val minLimit = productLimits?.min?.amount ?: CryptoValue.zero(sourceAsset)

            return minLimit as CryptoValue
        }

    val maxLimit: CryptoValue?
        get() {
            val spendableBalance = spendableBalance ?: return null

            val maxProductLimitInSourceValue =
                (productLimits?.max as? TxLimit.Limited)?.amount ?: spendableBalance
            val maxLimit = listOf(spendableBalance, maxProductLimitInSourceValue).min()
            return maxLimit as CryptoValue
        }

    val spendableBalance: CryptoValue?
        get() = safeLet(fromAccount?.balanceCrypto, sourceNetworkFees) { balance, sourceNetworkFees ->
            if (balance.currency == sourceNetworkFees.feeForFullAvailable.currency) {
                balance - sourceNetworkFees.feeForFullAvailable
            } else {
                balance
            }
        } as CryptoValue?
}

sealed interface SellEnterAmountInputError : Serializable {
    data class BelowMinimum(val minValue: String) : SellEnterAmountInputError
    data class AboveMaximum(val maxValue: String) : SellEnterAmountInputError
    data class AboveBalance(
        val displayTicker: String,
        val balance: String,
    ) : SellEnterAmountInputError
    data class InsufficientGas(val displayTicker: String) : SellEnterAmountInputError
    data class Unknown(val error: String?) : SellEnterAmountInputError
}

sealed interface SellEnterAmountFatalError {
    object WalletLoading : SellEnterAmountFatalError
}
