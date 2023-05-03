package com.dex.presentation.uierrors

import android.content.Context
import com.blockchain.dex.presentation.R
import com.dex.domain.DexTransaction
import com.dex.domain.DexTxError
import info.blockchain.balance.Currency

sealed class DexUiError {
    object None : DexUiError()
    data class InsufficientFunds(val currency: Currency) : DexUiError(), AlertError {
        override fun message(context: Context): String =
            context.getString(R.string.not_enough_funds, currency.displayTicker)
    }

    object LiquidityError : DexUiError(), AlertError {
        override fun message(context: Context): String =
            context.getString(R.string.unable_to_swap_tokens)
    }

    data class TokenNotAllowed(val token: Currency) : DexUiError()
    data class NotEnoughGas(val gasCurrency: Currency) : DexUiError(), AlertError {
        override fun message(context: Context): String =
            context.getString(R.string.not_enough_gas, gasCurrency.displayTicker)
    }

    data class CommonUiError(val title: String, val description: String) : DexUiError()
    object TransactionInProgressError : DexUiError()
    data class UnknownError(val exception: Exception) : DexUiError()
}

interface AlertError {
    fun message(context: Context): String
}

fun DexTransaction.toUiError(): DexUiError {
    return when (val error = txError) {
        DexTxError.None -> DexUiError.None
        DexTxError.NotEnoughFunds -> DexUiError.InsufficientFunds(
            sourceAccount.currency
        )
        DexTxError.NotEnoughGas -> {
            val feeCurrency = quote?.networkFees?.currency
            check(feeCurrency != null)
            DexUiError.NotEnoughGas(
                feeCurrency
            )
        }
        is DexTxError.FatalTxError -> DexUiError.UnknownError(error.exception)
        is DexTxError.TxInProgress -> DexUiError.TransactionInProgressError
        is DexTxError.QuoteError ->
            if (error.isLiquidityError()) DexUiError.LiquidityError
            else
                DexUiError.CommonUiError(
                    error.title,
                    error.message
                )
        DexTxError.TokenNotAllowed -> DexUiError.TokenNotAllowed(
            sourceAccount.currency
        )
    }
}
