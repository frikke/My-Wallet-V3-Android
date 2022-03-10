package com.blockchain.blockchaincard.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.blockchaincard.R
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.databinding.FragmentActivityBinding
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding

class OrderCardActivity : BlockchainActivity() {
    override val alwaysDisableScreenshots: Boolean
        get() = false

    private val binding: FragmentActivityBinding by lazy {
        FragmentActivityBinding.inflate(layoutInflater)
    }

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateToolbar { super.onBackPressed() }
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.content_frame,
                    BlockchainCardFragment.newInstance(),
                    BlockchainCardFragment::class.simpleName
                )
                .commitAllowingStateLoss()
        }
    }

    companion object {
        fun newIntent(context: Context): Intent =
            Intent(context, OrderCardActivity::class.java)
    }
}
