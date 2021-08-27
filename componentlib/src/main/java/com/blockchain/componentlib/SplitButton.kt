package com.blockchain.componentlib

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.componentlib.databinding.ViewSplitButtonBinding

class SplitButton : ConstraintLayout {
    private val binding = ViewSplitButtonBinding.inflate(LayoutInflater.from(context), this)

    private var leftButtonText: CharSequence? = null
        set(value) {
            binding.leftButton.text = value
            binding.leftButton.contentDescription = value
            field = value
        }

    private var rightButtonText: CharSequence? = null
        set(value) {
            binding.rightButton.text = value
            binding.rightButton.contentDescription = value
            field = value
        }

    var leftButton: Button = binding.leftButton
    var rightButton: Button = binding.rightButton

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
        context.obtainStyledAttributes(attrs, R.styleable.SplitButton).apply {
            leftButtonText = getText(R.styleable.SplitButton_leftButtonText)
            rightButtonText = getText(R.styleable.SplitButton_rightButtonText)
        }.recycle()
        setupUi()
    }

    private fun setupUi() {
        binding.root.setBackgroundResource(R.drawable.background_split_button)
    }

    fun setLeftButtonOnClickListener(block: (View) -> Unit) {
        binding.leftButton.setOnClickListener(block)
    }

    fun setRightButtonOnClickListener(block: (View) -> Unit) {
        binding.rightButton.setOnClickListener(block)
    }
}