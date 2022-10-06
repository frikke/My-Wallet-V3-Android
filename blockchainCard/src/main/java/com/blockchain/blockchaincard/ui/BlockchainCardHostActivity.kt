package com.blockchain.blockchaincard.ui

import android.content.Context
import android.content.Intent
import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddress
import com.blockchain.blockchaincard.domain.models.BlockchainCardGoogleWalletPushTokenizeData
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.blockchaincard.viewmodel.BlockchainCardArgs
import com.blockchain.blockchaincard.viewmodel.managecard.ManageCardViewModel
import com.blockchain.blockchaincard.viewmodel.ordercard.OrderCardViewModel
import com.blockchain.coincore.FiatAccount
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.koin.payloadScope
import info.blockchain.balance.AssetInfo
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope

abstract class BlockchainCardHostActivity : BlockchainActivity(), AndroidScopeComponent {

    override val scope: Scope = payloadScope

    val orderCardViewModel: OrderCardViewModel by viewModel()
    val manageCardViewModel: ManageCardViewModel by viewModel()

    val modelArgs: ModelConfigArgs by lazy {
        (intent?.getParcelableExtra(BLOCKCHAIN_CARD) as? BlockchainCard)?.let { card ->
            BlockchainCardArgs.CardArgs(card)
        } ?: (intent?.getParcelableExtra(BLOCKCHAIN_PRODUCT) as? BlockchainCardProduct)?.let { product ->
            BlockchainCardArgs.ProductArgs(product)
        } ?: throw IllegalStateException("Missing card or product data")
    }

    abstract fun newIntent(context: Context, blockchainCard: BlockchainCard): Intent

    abstract fun newIntent(context: Context, blockchainCardProduct: BlockchainCardProduct): Intent

    companion object {
        const val BLOCKCHAIN_CARD = "BLOCKCHAIN_CARD"
        const val BLOCKCHAIN_PRODUCT = "BLOCKCHAIN_PRODUCT"
    }

    abstract fun startBuy(asset: AssetInfo)

    abstract fun startDeposit(account: FiatAccount)

    abstract fun startKycAddressVerification(address: BlockchainCardAddress)

    abstract fun updateKycAddress(address: BlockchainCardAddress)

    abstract fun startAddCardToGoogleWallet(pushTokenizeData: BlockchainCardGoogleWalletPushTokenizeData)
}
