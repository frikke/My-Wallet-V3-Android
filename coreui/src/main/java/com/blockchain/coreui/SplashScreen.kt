package com.blockchain.coreui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.coreui.databinding.SplashScreenBinding

class SplashScreen: ConstraintLayout {

    private val binding = SplashScreenBinding.inflate(LayoutInflater.from(context), this)

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
        setupUi()
    }

    private fun setupUi() {

    }



}