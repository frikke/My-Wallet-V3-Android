package com.blockchain.transactions.swap.enteramount

import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.core.limits.TxLimit
import com.blockchain.core.limits.TxLimits
import com.blockchain.data.DataResource
import com.blockchain.data.dataOrNull
import com.blockchain.extensions.safeLet
import com.blockchain.transactions.common.CombinedSourceNetworkFees
import com.blockchain.transactions.common.CryptoAccountWithBalance
import com.blockchain.transactions.common.OnChainDepositInputValidationError
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.CurrencyType
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import java.io.Serializable

data class EnterAmountModelState(
    val walletMode: WalletMode? = null,

    val fromAccount: CryptoAccountWithBalance? = null,
    val secondPassword: String? = null,
    val toAccount: CryptoAccount? = null,

    val config: DataResource<EnterAmountConfig> = DataResource.Loading,

    val sourceNetworkFees: CombinedSourceNetworkFees? = null,
    val targetNetworkFeeInSourceValue: CryptoValue? = null,
    val depositEngineInputValidationError: OnChainDepositInputValidationError? = null,

    val fiatAmount: FiatValue? = null,
    val fiatAmountUserInput: String = "",
    val cryptoAmount: CryptoValue? = null,
    val cryptoAmountUserInput: String = "",

    val selectedInput: CurrencyType = CurrencyType.FIAT,

    val getDepositNetworkFeeError: Exception? = null,
    val getTargetNetworkFeeError: Exception? = null,
    val fatalError: SwapEnterAmountFatalError? = null
) : ModelState {
    /**
     * A word on limits and fees, the actual Minimum order Limit is [config.productLimits], eg. 5 XLM, for us to comply with this min order value
     * we'll have to add the [sourceNetworkFees.feeForAmount], eg. 2 XLM, so the min input limit is now 5 + 2 XLM, this behaviour is not explicit
     * in the [minLimit] calculation below because the [cryptoAmount] is the final order amount, the [sourceNetworkFees.feeForAmount] is completely
     * separate and will only be "charged" when the user performs the NC deposit into BCDC account address, hence why it's actually
     * part of the [maxLimit] instead, to ensure whatever the user inputs in [cryptoAmount] never exceeds [spendableBalance].
     * Regarding [targetNetworkFeeInSourceValue], which is being added to the [config.productLimits.min], there's no product rule
     * that enforces this, it's how it currently worked before this refactor, it will in essence ensure the user will not only submit an
     * order where the input is greater than the min product limit, but also that the output in target currency the user will get will also be
     * greater than the min product limit, so in case:
     *  - [cryptoAmount] = 5 XLM
     *  - [sourceNetworkFees.feeForAmount] = 2 XLM
     *  - [targetNetworkFeeInSourceValue] = 3 XLM
     *
     *  [minLimit] will be 8 XLM, order value will be 8 XLM, user spend 10 XLM total and will, crucially, get 5 XLM worth of
     *  the target currency back, honoring the min product limit for the amount received.
     */
    val minLimit: CryptoValue?
        get() {
            val fromAccount = fromAccount ?: return null
            val sourceAsset = fromAccount.account.currency

            val targetNetworkFeeInSourceValue: CryptoValue =
                targetNetworkFeeInSourceValue ?: CryptoValue.zero(sourceAsset)

            val minProductLimitInSourceValue =
                config.dataOrNull()?.productLimits?.min?.amount ?: CryptoValue.zero(sourceAsset)
            val minLimit = minProductLimitInSourceValue + targetNetworkFeeInSourceValue

            return minLimit as CryptoValue
        }

    val maxLimit: CryptoValue?
        get() {
            val spendableBalance = spendableBalance ?: return null

            val maxProductLimitInSourceValue =
                (config.dataOrNull()?.productLimits?.max as? TxLimit.Limited)?.amount ?: spendableBalance
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

data class EnterAmountConfig(
    val sourceAccountToFiatRate: ExchangeRate,
    val productLimits: TxLimits,
)

sealed interface SwapEnterAmountInputError : Serializable {
    data class BelowMinimum(val minValue: String) : SwapEnterAmountInputError
    data class AboveMaximum(val maxValue: String) : SwapEnterAmountInputError
    data class AboveBalance(
        val displayTicker: String,
        val balance: String,
    ) : SwapEnterAmountInputError
    data class InsufficientGas(val displayTicker: String) : SwapEnterAmountInputError
    data class Unknown(val error: String?) : SwapEnterAmountInputError
}

sealed interface SwapEnterAmountFatalError {
    object WalletLoading : SwapEnterAmountFatalError
}
