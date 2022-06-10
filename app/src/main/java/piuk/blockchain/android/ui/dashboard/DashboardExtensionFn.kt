package piuk.blockchain.android.ui.dashboard

import android.annotation.SuppressLint
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.elyeproj.loaderviewlibrary.LoaderTextView
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import piuk.blockchain.android.R
import piuk.blockchain.android.util.context

fun LoaderTextView.showLoading() =
    resetLoader()

fun Money?.format(cryptoCurrency: Currency) =
    this?.toStringWithSymbol()
        ?: Money.zero(cryptoCurrency).toStringWithSymbol()

fun Double.asPercentString() =
    String.format("%.2f%%", this)

fun TextView.setDeltaColour(
    delta: Double,
    positiveColor: Int = R.color.dashboard_delta_positive,
    negativeColor: Int = R.color.dashboard_delta_negative
) {
    if (delta < 0)
        setTextColor(ContextCompat.getColor(context, negativeColor))
    else
        setTextColor(ContextCompat.getColor(context, positiveColor))
}

@SuppressLint("SetTextI18n")
fun TextView.asDeltaPercent(delta: Double, prefix: String = "", postfix: String = "") {

    text = prefix + if (delta.isNaN()) {
        "--"
    } else {
        delta.asPercentString()
    } + postfix
    setDeltaColour(delta)
}

fun TextView.setContentDescriptionSuffix(@StringRes accessibilityLabelRes: Int) {
    contentDescription = "${context.getString(accessibilityLabelRes)}: $text"
}

fun TextView.setContentDescriptionSuffix(accessibilityLabel: String) {
    contentDescription = "$accessibilityLabel: $text"
}
