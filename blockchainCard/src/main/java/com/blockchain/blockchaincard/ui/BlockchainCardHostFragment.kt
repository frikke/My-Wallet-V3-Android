package com.blockchain.blockchaincard.ui

import androidx.fragment.app.Fragment
import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.blockchaincard.viewmodel.BlockchainCardArgs
import com.blockchain.blockchaincard.viewmodel.managecard.ManageCardViewModel
import com.blockchain.blockchaincard.viewmodel.ordercard.OrderCardViewModel
import com.blockchain.coincore.FiatAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.koin.payloadScope
import info.blockchain.balance.AssetInfo
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope

abstract class BlockchainCardHostFragment : Fragment(), AndroidScopeComponent {

    override val scope: Scope = payloadScope

    val orderCardViewModel: OrderCardViewModel by viewModel()

    val manageCardViewModel: ManageCardViewModel by viewModel()

    val modelArgs: ModelConfigArgs by lazy {
        (arguments?.getParcelable(BLOCKCHAIN_CARD) as? BlockchainCard)?.let { card ->
            BlockchainCardArgs.CardArgs(card)
        } ?: (arguments?.getParcelable(BLOCKCHAIN_PRODUCT) as? BlockchainCardProduct)?.let { product ->
            BlockchainCardArgs.ProductArgs(product)
        } ?: throw IllegalStateException("Missing card or product data")
    }

    abstract fun newInstance(blockchainCard: BlockchainCard): BlockchainCardHostFragment

    abstract fun newInstance(blockchainCardProduct: BlockchainCardProduct): BlockchainCardHostFragment

    companion object {
        const val BLOCKCHAIN_CARD = "BLOCKCHAIN_CARD"
        const val BLOCKCHAIN_PRODUCT = "BLOCKCHAIN_PRODUCT"
    }

    abstract fun startBuy(asset: AssetInfo)

    abstract fun startDeposit(account: FiatAccount)
}
