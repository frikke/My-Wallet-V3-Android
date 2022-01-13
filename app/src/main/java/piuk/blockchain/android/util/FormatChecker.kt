package piuk.blockchain.android.util

import android.util.Patterns

class FormatChecker {
    fun isValidEmailAddress(address: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(address).matches()
    }

    fun isValidMobileNumber(mobile: String): Boolean {
        return Patterns.PHONE.matcher(mobile).matches()
    }
}
