package com.dex.presentation.uierrors

import android.content.Context
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.dex.domain.DexTransaction
import com.dex.domain.DexTxError
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CoinNetwork
import info.blockchain.balance.Currency

sealed class DexUiError {
    data class InsufficientFunds(val account: CryptoNonCustodialAccount) :
        DexUiError(),
        AlertError,
        ActionRequiredError {
        override fun message(context: Context): String =
            context.getString(com.blockchain.stringResources.R.string.not_enough_funds, account.currency.displayTicker)

        override val priority: Int
            get() = 0
    }

    data class TokenNotAllowed(val token: AssetInfo, val hasBeenApproved: Boolean) : DexUiError(), ActionRequiredError {
        override val priority: Int
            get() = 1
    }

    data class NotEnoughGas(val gasCurrency: Currency) : DexUiError(), AlertError, ActionRequiredError {
        override fun message(context: Context): String =
            context.getString(
                com.blockchain.stringResources.R.string.not_enough_gas, gasCurrency.displayTicker
            )

        override val priority: Int
            get() = 2
    }

    /**
     * In some rare cases like timeout exceptions, there is no title or description.
     */
    data class CommonUiError(val title: String?, val description: String?) : DexUiError(), AlertError {
        override fun message(context: Context): String =
            title ?: context.getString(
                com.blockchain.stringResources.R.string.unable_to_swap_tokens
            )
    }

    data class TransactionInProgressError(val coinNetwork: CoinNetwork) : DexUiError()
    data class UnknownError(val exception: Exception) : DexUiError()
}

interface AlertError {
    fun message(context: Context): String
}

interface ActionRequiredError {
    val priority: Int
}

fun DexTransaction.uiErrors(): List<DexUiError> {
    return txErrors.map {
        when (it) {
            DexTxError.NotEnoughFunds -> DexUiError.InsufficientFunds(sourceAccount.account)
            DexTxError.NotEnoughGas -> {
                val feeCurrency = quote?.networkFees?.currency
                check(feeCurrency != null)
                DexUiError.NotEnoughGas(
                    feeCurrency
                )
            }

            is DexTxError.FatalTxError -> DexUiError.UnknownError(it.exception)
            is DexTxError.TxInProgress -> DexUiError.TransactionInProgressError(it.coinNetwork)
            is DexTxError.QuoteError ->
                if (it.isInsufficientFundsError) {
                    DexUiError.InsufficientFunds(sourceAccount.account)
                } else {
                    DexUiError.CommonUiError(
                        it.title,
                        it.message
                    )
                }
            is DexTxError.TokenNotAllowed -> DexUiError.TokenNotAllowed(
                token = sourceAccount.currency,
                hasBeenApproved = it.hasBeenApproved
            )
        }
    }
}
