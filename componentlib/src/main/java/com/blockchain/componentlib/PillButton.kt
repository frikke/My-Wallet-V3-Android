package com.blockchain.componentlib

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import com.blockchain.componentlib.databinding.ViewPillButtonBinding

class PillButton : ConstraintLayout {
    private val binding = ViewPillButtonBinding.inflate(LayoutInflater.from(context), this)

    var text: CharSequence? = null
        set(value) {
            binding.pillButtonButton.text = value
            field = value
        }

    var showProgress: Boolean = false
        set(value) {
            field = value
            binding.pillButtonProgress.visibility = if (value) View.VISIBLE else View.GONE
            binding.pillButtonButton.text = if (value) null else (text)
        }

    constructor(context: Context) : super(context) {
        initWithAttributes(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initWithAttributes(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initWithAttributes(attrs)
    }

    private fun initWithAttributes(attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.PillButton).apply {
            val typeOrdinal =
                getInt(R.styleable.PillButton_pillButtonType, PillButtonType.PRIMARY.ordinal)
            setButtonStyle(PillButtonType.values()[typeOrdinal])

            text = getText(R.styleable.PillButton_android_text)
            binding.pillButtonButton.isEnabled =
                getBoolean(R.styleable.PillButton_android_enabled, true)
        }.recycle()
    }

    private fun setButtonStyle(buttonType: PillButtonType) {
        val backgroundTintList = AppCompatResources.getColorStateList(
            context,
            when (buttonType) {
                PillButtonType.PRIMARY -> R.color.paletteBasePrimary
                PillButtonType.SECONDARY -> R.color.paletteBaseWhite
            }
        )

        ViewCompat.setBackgroundTintList(binding.pillButtonButton, backgroundTintList)
        if (buttonType == PillButtonType.SECONDARY) {
            binding.pillButtonButton.setTextColor(
                AppCompatResources.getColorStateList(
                    context,
                    R.color.paletteBasePrimary
                )
            )
        }
    }

    fun setText(@StringRes resId: Int) {
        binding.pillButtonButton.setText(resId)
    }

    fun setOnClickListener(block: (View) -> Unit) {
        binding.pillButtonButton.setOnClickListener(block)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        binding.pillButtonButton.isEnabled = enabled
    }

    enum class PillButtonType {
        PRIMARY, SECONDARY
    }
}
