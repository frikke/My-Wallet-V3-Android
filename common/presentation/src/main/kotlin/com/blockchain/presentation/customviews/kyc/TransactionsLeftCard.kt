package com.blockchain.presentation.customviews.kyc

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.blockchain.common.databinding.ViewTransactionsLeftBinding

class TransactionsLeftCard @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(ctx, attr, defStyle) {

    private val binding: ViewTransactionsLeftBinding =
        ViewTransactionsLeftBinding.inflate(LayoutInflater.from(context), this, true)

    fun setup(maxTransactions: Int, transactionsLeft: Int) {
        val progress = transactionsLeft.toFloat() / maxTransactions.toFloat()
        binding.progressSteps.setProgress((1f - progress) * 100)
        binding.textSteps.text = "$transactionsLeft"
    }
}
