package piuk.blockchain.android.ui.customviews.inputview

import android.text.InputType
import com.blockchain.keyboard.InputKeyboard
import java.text.DecimalFormatSymbols

class InputAmountKeyboard : InputKeyboard {
    private fun getDecimalSeparator(): Char = DecimalFormatSymbols.getInstance().decimalSeparator
    private fun getDeviceManufacturer(): String = android.os.Build.MANUFACTURER
    override fun inputTypeForAmount(): Int {
        return if (getDeviceManufacturer().equals(SAMSUNG_DEVICE, ignoreCase = true) &&
            getDecimalSeparator() != SEPARATOR_US
        ) {
            InputType.TYPE_CLASS_PHONE
        } else {
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
        }
    }

    override fun validInputCharacters(): String = DIGITS + getDecimalSeparator()

    companion object {
        const val SAMSUNG_DEVICE = "samsung"
        const val SEPARATOR_US = '.'
        const val DIGITS = "0123456789"
    }
}
