package piuk.blockchain.android.ui.customviews.inputview

import java.text.DecimalFormatSymbols

interface InputKeyboard {
    fun getDecimalSeparator(): Char = DecimalFormatSymbols.getInstance().decimalSeparator
    fun getDeviceManufacturer(): String = android.os.Build.MANUFACTURER
    fun setInputTypeToView()
    fun addDigitsToView()

    companion object {
        const val SAMSUNG_DEVICE = "samsung"
        const val SEPARATOR_US = '.'
        const val DIGITS = "0123456789"
    }
}
