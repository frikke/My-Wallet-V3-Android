package com.blockchain.componentlib.price

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.blockchain.componentlib.R
import com.blockchain.componentlib.databinding.ViewPriceRowBinding
import com.blockchain.componentlib.viewextensions.px
import com.bumptech.glide.Glide
import java.text.NumberFormat

class PriceView : ConstraintLayout {
    private val binding = ViewPriceRowBinding.inflate(LayoutInflater.from(context), this)

    data class Price(
        val icon: String,
        val name: String,
        val displayTicker: String,
        val networkTicker: String,
        val price: String = "",
        val gain: Double = 0.0
    )

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        minHeight = 90.px
    }

    var price: Price? = null
        set(value) {
            if (value != null) {
                Glide.with(context)
                    .load(value.icon)
                    .into(binding.icon)
                binding.name.text = value.name
                binding.ticker.text = value.displayTicker
                binding.price.text = value.price
                binding.gain.text = getGainSpannable(value.gain)
            }

            field = value
        }

    private fun getGainSpannable(gain: Double): SpannableString {
        val percentFormatter = NumberFormat.getPercentInstance()
        percentFormatter.minimumFractionDigits = 2
        val percent = percentFormatter.format(gain / 100)

        val spannableString = if (gain < 0) {
            SpannableString("↓ $percent")
        } else {
            SpannableString("↑ $percent")
        }

        val foregroundSpan = if (gain < 0) {
            ForegroundColorSpan(ContextCompat.getColor(context, R.color.paletteBaseError))
        } else {
            ForegroundColorSpan(ContextCompat.getColor(context, R.color.paletteBaseSuccess))
        }

        spannableString.setSpan(foregroundSpan, 0, spannableString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannableString
    }
}
