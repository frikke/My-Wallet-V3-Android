package com.blockchain.componentlib

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.componentlib.databinding.ViewSplitButtonBinding
import com.google.android.material.button.MaterialButton

class SplitButton : ConstraintLayout {
    private val binding = ViewSplitButtonBinding.inflate(LayoutInflater.from(context), this)

    private var leftButtonText: CharSequence? = null
        set(value) {
            binding.leftButton.text = value
            binding.leftButton.contentDescription = value
            field = value
        }

    private var leftButtonIcon: Drawable? = null
        set(value) {
            binding.leftButton.icon = value
            field = value
        }

    private var rightButtonText: CharSequence? = null
        set(value) {
            binding.rightButton.text = value
            binding.rightButton.contentDescription = value
            field = value
        }

    private var rightButtonIcon: Drawable? = null
        set(value) {
            binding.rightButton.icon = value
            field = value
        }

    var leftButton: MaterialButton = binding.leftButton
    var rightButton: MaterialButton = binding.rightButton

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
            leftButtonIcon = getDrawable(R.styleable.SplitButton_leftButtonIcon)
            rightButtonText = getText(R.styleable.SplitButton_rightButtonText)
            rightButtonIcon = getDrawable(R.styleable.SplitButton_rightButtonIcon)
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
