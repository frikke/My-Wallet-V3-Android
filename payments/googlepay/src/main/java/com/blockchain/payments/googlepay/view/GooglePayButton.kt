package com.blockchain.payments.googlepay.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.payments.googlepay.databinding.GooglePayButtonBinding

class GooglePayButton(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    private val binding: GooglePayButtonBinding =
        GooglePayButtonBinding.inflate(LayoutInflater.from(context), this, true)

    fun showLoading() {
        with(binding) {
            image.visibility = View.INVISIBLE
            loading.visibility = View.VISIBLE
        }
        isEnabled = false
    }

    fun hideLoading() {
        with(binding) {
            image.visibility = View.VISIBLE
            loading.visibility = View.INVISIBLE
        }
        isEnabled = true
    }
}
