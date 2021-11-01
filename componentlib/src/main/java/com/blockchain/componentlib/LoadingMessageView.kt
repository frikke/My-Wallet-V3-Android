package com.blockchain.componentlib

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.componentlib.databinding.ViewLoaderBinding

class LoadingMessageView : ConstraintLayout {

    private val binding = ViewLoaderBinding.inflate(LayoutInflater.from(context), this)

    var text: CharSequence? = null
        set(value) {
            binding.title.text = value
            field = value
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
        context.obtainStyledAttributes(attrs, R.styleable.LoadingMessageView).apply {
            text = getText(R.styleable.LoadingMessageView_android_text)
        }.recycle()
    }
}