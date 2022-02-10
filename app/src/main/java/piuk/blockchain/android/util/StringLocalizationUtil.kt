package piuk.blockchain.android.util

import piuk.blockchain.android.R

class StringLocalizationUtil {
    companion object {
        @JvmStatic fun getBankDepositTitle(currencyTicker: String): Int =
            if (currencyTicker == "USD") {
                R.string.wire_transfer
            } else {
                R.string.bank_transfer
            }
    }
}