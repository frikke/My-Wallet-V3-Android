package com.blockchain.home.presentation.allassets

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.home.presentation.allassets.composable.CryptoAssets

// todo(othman) probably want compose single activity / need to figure out navigation
class AllAssetsActivity : BlockchainActivity() {

    override val alwaysDisableScreenshots: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CryptoAssets()
        }
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, AllAssetsActivity::class.java)
    }
}
