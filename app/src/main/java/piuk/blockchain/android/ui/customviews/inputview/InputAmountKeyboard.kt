package piuk.blockchain.android.ui.customviews.inputview

import android.text.InputType
import com.blockchain.keyboard.InputKeyboard
import java.text.DecimalFormatSymbols

class InputAmountKeyboard : InputKeyboard {
    private fun getDecimalSeparator(): Char = DecimalFormatSymbols.getInstance().decimalSeparator
    private fun getDeviceManufacturer(): String = android.os.Build.MANUFACTURER
    private fun isDeviceManufacturerFlagged(): Boolean =
        FLAGGED_DEVICES.any { it.equals(getDeviceManufacturer(), ignoreCase = true) }

    override fun specialInputForAmounts(): Int? {
        return if (isDeviceManufacturerFlagged() && getDecimalSeparator() != SEPARATOR_US) {
            InputType.TYPE_CLASS_PHONE
        } else {
            null
        }
    }

    override fun validInputCharacters(): String = DIGITS + getDecimalSeparator()

    companion object {
        private const val SAMSUNG_DEVICE = "samsung"
        private const val XIAOMI_DEVICE = "xiaomi"
        private val FLAGGED_DEVICES = listOf(SAMSUNG_DEVICE, XIAOMI_DEVICE)

        private const val SEPARATOR_US = '.'
        private const val DIGITS = "0123456789"
    }
}
