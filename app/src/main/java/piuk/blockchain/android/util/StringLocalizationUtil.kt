package piuk.blockchain.android.util

import android.content.res.Resources
import com.blockchain.domain.paymentmethods.model.DepositTerms
import com.blockchain.utils.toDayAndMonth
import java.util.Calendar
import piuk.blockchain.android.R

class StringLocalizationUtil {
    companion object {
        private const val TRADING_CURRENCY_DOLLARS = "USD"
        private const val TRADING_CURRENCY_POUNDS = "GBP"
        private const val TRADING_CURRENCY_EUROS = "EUR"
        private const val TRADING_CURRENCY_ARGENTINE_PESO = "ARS"

        @JvmStatic fun getBankDepositTitle(currencyTicker: String): Int =
            if (currencyTicker == TRADING_CURRENCY_DOLLARS) {
                com.blockchain.stringResources.R.string.wire_transfer
            } else {
                com.blockchain.stringResources.R.string.bank_transfer
            }

        fun subtitleForBankAccount(currencyCode: String): Int = when (currencyCode) {
            TRADING_CURRENCY_DOLLARS -> com.blockchain.stringResources.R.string.payment_wire_transfer_subtitle_dollars
            TRADING_CURRENCY_POUNDS -> com.blockchain.stringResources.R.string.payment_wire_transfer_subtitle_pounds
            TRADING_CURRENCY_EUROS -> com.blockchain.stringResources.R.string.payment_wire_transfer_subtitle_euros
            TRADING_CURRENCY_ARGENTINE_PESO ->
                com.blockchain.stringResources.R.string.payment_wire_transfer_subtitle_ars
            else -> com.blockchain.stringResources.R.string.payment_wire_transfer_subtitle_default
        }

        fun blurbForBankAccount(currencyCode: String): Int = when (currencyCode) {
            TRADING_CURRENCY_DOLLARS -> com.blockchain.stringResources.R.string.bank_transfer_blurb_dollars
            TRADING_CURRENCY_POUNDS -> com.blockchain.stringResources.R.string.bank_transfer_blurb_pounds
            TRADING_CURRENCY_EUROS -> com.blockchain.stringResources.R.string.bank_transfer_blurb_euros
            TRADING_CURRENCY_ARGENTINE_PESO -> com.blockchain.stringResources.R.string.bank_transfer_blurb_ars
            else -> com.blockchain.stringResources.R.string.bank_transfer_blurb_default
        }

        fun subtitleForEasyTransfer(currencyCode: String): Int = when (currencyCode) {
            TRADING_CURRENCY_DOLLARS -> com.blockchain.stringResources.R.string.payment_deposit_subtitle_dollars
            TRADING_CURRENCY_POUNDS -> com.blockchain.stringResources.R.string.payment_deposit_subtitle_pounds
            TRADING_CURRENCY_EUROS -> com.blockchain.stringResources.R.string.payment_deposit_subtitle_euros
            else -> com.blockchain.stringResources.R.string.payment_deposit_subtitle_default
        }

        fun getFormattedDepositTerms(
            resources: Resources,
            displayMode: DepositTerms.DisplayMode,
            min: Int,
            max: Int
        ): String? {
            val minDay = Calendar.getInstance().apply { add(Calendar.MINUTE, min) }.time
            val maxDay = Calendar.getInstance().apply { add(Calendar.MINUTE, max) }.time

            return when (displayMode) {
                DepositTerms.DisplayMode.IMMEDIATELY -> resources.getString(
                    com.blockchain.stringResources.R.string.deposit_terms_immediately
                )
                DepositTerms.DisplayMode.MAX_MINUTE -> String.format(
                    resources.getString(com.blockchain.stringResources.R.string.deposit_terms_max_minutes),
                    max
                )
                DepositTerms.DisplayMode.MAX_DAY -> maxDay.toDayAndMonth()
                DepositTerms.DisplayMode.MINUTE_RANGE ->
                    String.format(
                        resources.getString(com.blockchain.stringResources.R.string.deposit_terms_between_minutes),
                        min,
                        max
                    )
                DepositTerms.DisplayMode.DAY_RANGE ->
                    String.format(
                        resources.getString(com.blockchain.stringResources.R.string.deposit_terms_between_days),
                        minDay.toDayAndMonth(),
                        maxDay.toDayAndMonth()
                    )
                DepositTerms.DisplayMode.NONE -> null
            }
        }
    }
}
