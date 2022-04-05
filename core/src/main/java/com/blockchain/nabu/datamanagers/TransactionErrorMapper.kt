package com.blockchain.nabu.datamanagers

import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorCodes
import com.blockchain.api.NabuErrorStatusCodes

class TransactionErrorMapper {
    fun mapToTransactionError(exception: Throwable): TransactionError {
        return if (exception is NabuApiException) {
            when (exception.getErrorStatusCode()) {
                NabuErrorStatusCodes.InternalServerError -> {
                    when (exception.getErrorCode()) {
                        NabuErrorCodes.InternalServerError -> TransactionError.InternalServerError
                        else -> TransactionError.HttpError(exception)
                    }
                }
                NabuErrorStatusCodes.Conflict -> {
                    when (exception.getErrorCode()) {
                        NabuErrorCodes.TradingTemporarilyDisabled -> TransactionError.TradingTemporarilyDisabled
                        NabuErrorCodes.InsufficientBalance -> TransactionError.InsufficientBalance
                        NabuErrorCodes.IneligibleForSwap -> TransactionError.IneligibleForSwap
                        else -> TransactionError.HttpError(exception)
                    }
                }
                NabuErrorStatusCodes.BadRequest -> {
                    when (exception.getErrorCode()) {
                        NabuErrorCodes.OrderBelowMinLimit -> TransactionError.OrderBelowMin
                        NabuErrorCodes.OrderAboveMaxLimit -> TransactionError.OrderAboveMax
                        NabuErrorCodes.DailyLimitExceeded -> TransactionError.SwapDailyLimitExceeded
                        NabuErrorCodes.WeeklyLimitExceeded -> TransactionError.SwapWeeklyLimitExceeded
                        NabuErrorCodes.AnnualLimitExceeded -> TransactionError.SwapYearlyLimitExceeded
                        NabuErrorCodes.PendingOrdersLimitReached -> TransactionError.OrderLimitReached
                        NabuErrorCodes.InvalidCryptoAddress -> TransactionError.InvalidCryptoAddress
                        NabuErrorCodes.InvalidCryptoCurrency -> TransactionError.InvalidCryptoCurrency
                        NabuErrorCodes.InvalidFiatCurrency -> TransactionError.InvalidFiatCurrency
                        NabuErrorCodes.OrderDirectionDisabled -> TransactionError.OrderDirectionDisabled
                        NabuErrorCodes.InvalidOrExpiredQuote -> TransactionError.InvalidOrExpiredQuote
                        NabuErrorCodes.InvalidDestinationAmount -> TransactionError.InvalidDestinationAmount
                        else -> TransactionError.HttpError(exception)
                    }
                }
                NabuErrorStatusCodes.Forbidden -> TransactionError.WithdrawalAlreadyPending
                else -> TransactionError.HttpError(exception)
            }
        } else throw IllegalStateException("Unknown error type $exception")
    }
}
