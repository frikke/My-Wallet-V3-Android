package com.blockchain.transactions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.base.setContent
import com.blockchain.presentation.extensions.getAccount
import com.blockchain.presentation.extensions.getTarget
import com.blockchain.presentation.extensions.putAccount
import com.blockchain.presentation.extensions.putTarget
import com.blockchain.transactions.sell.SellGraphHost
import com.blockchain.transactions.swap.SwapGraphHost
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi

@OptIn(ExperimentalMaterialNavigationApi::class)
class NewTransactionFlowActivity : BlockchainActivity() {
    override val alwaysDisableScreenshots: Boolean = false

    // TODO(aromano): use source and target to initialise the models
    private val sourceAccount: SingleAccount by lazy {
        intent.extras?.getAccount(SOURCE) as SingleAccount
    }

    private val transactionTarget: TransactionTarget by lazy {
        intent.extras?.getTarget(TARGET)!!
    }

    private val action: AssetAction by lazy {
        intent.extras?.getSerializable(ACTION) as AssetAction
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            when (action) {
                AssetAction.Swap -> SwapGraphHost(exitFlow = ::finish)
                AssetAction.Sell -> SellGraphHost(exitFlow = ::finish)
                else -> throw UnsupportedOperationException()
            }
        }
    }

    companion object {
        private const val SOURCE = "SOURCE_ACCOUNT"
        private const val TARGET = "TARGET_ACCOUNT"
        private const val ACTION = "ASSET_ACTION"

        fun newIntent(
            context: Context,
            sourceAccount: BlockchainAccount? = NullCryptoAccount(),
            target: TransactionTarget? = NullCryptoAccount(),
            action: AssetAction
        ): Intent {
            val bundle = Bundle().apply {
                putAccount(SOURCE, sourceAccount ?: NullCryptoAccount())
                putTarget(TARGET, target ?: NullCryptoAccount())
                putSerializable(ACTION, action)
            }

            return Intent(context, NewTransactionFlowActivity::class.java).apply {
                putExtras(bundle)
            }
        }
    }
}
