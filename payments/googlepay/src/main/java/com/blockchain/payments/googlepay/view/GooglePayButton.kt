package com.blockchain.payments.googlepay.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.payments.googlepay.databinding.GooglePayButtonBinding

class GooglePayButton(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    private val binding: GooglePayButtonBinding =
        GooglePayButtonBinding.inflate(LayoutInflater.from(context), this, true)
}