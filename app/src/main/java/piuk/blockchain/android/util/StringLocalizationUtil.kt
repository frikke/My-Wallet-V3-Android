package piuk.blockchain.android.util

import piuk.blockchain.android.R

class StringLocalizationUtil {
    companion object {
        private const val TRADING_CURRENCY_DOLLARS = "USD"
        private const val TRADING_CURRENCY_POUNDS = "GBP"
        private const val TRADING_CURRENCY_EUROS = "EUR"

        @JvmStatic fun getBankDepositTitle(currencyTicker: String): Int =
            if (currencyTicker == TRADING_CURRENCY_DOLLARS) {
                R.string.wire_transfer
            } else {
                R.string.bank_transfer
            }

        fun subtitleForBankAccount(currencyCode: String): Int = when (currencyCode) {
            TRADING_CURRENCY_DOLLARS -> R.string.payment_wire_transfer_subtitle_dollars
            TRADING_CURRENCY_POUNDS -> R.string.payment_wire_transfer_subtitle_pounds
            TRADING_CURRENCY_EUROS -> R.string.payment_wire_transfer_subtitle_euros
            else -> throw IllegalArgumentException("subtitleForBankAccount - Fiat currency does not exist")
        }

        fun blurbForBankAccount(currencyCode: String): Int = when (currencyCode) {
            TRADING_CURRENCY_DOLLARS -> R.string.bank_transfer_blurb_dollars
            TRADING_CURRENCY_POUNDS -> R.string.bank_transfer_blurb_pounds
            TRADING_CURRENCY_EUROS -> R.string.bank_transfer_blurb_euros
            else -> throw IllegalArgumentException("blurbForBankAccount - Fiat currency does not exist")
        }

        fun subtitleForEasyTransfer(currencyCode: String): Int = when (currencyCode) {
            TRADING_CURRENCY_DOLLARS -> R.string.payment_deposit_subtitle_dollars
            TRADING_CURRENCY_POUNDS -> R.string.payment_deposit_subtitle_pounds
            TRADING_CURRENCY_EUROS -> R.string.payment_deposit_subtitle_euros
            else -> throw IllegalArgumentException("subtitleForEasyTransfer - Fiat currency does not exist")
        }
    }
}
