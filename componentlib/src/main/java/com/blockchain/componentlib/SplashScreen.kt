package com.blockchain.componentlib

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.componentlib.databinding.SplashScreenBinding

class SplashScreen : ConstraintLayout {

    private val binding = SplashScreenBinding.inflate(LayoutInflater.from(context), this)

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )
}