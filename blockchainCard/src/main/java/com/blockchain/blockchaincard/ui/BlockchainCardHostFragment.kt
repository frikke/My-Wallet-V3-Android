package com.blockchain.blockchaincard.ui

import androidx.fragment.app.Fragment
import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
import com.blockchain.blockchaincard.viewmodel.BlockchainCardArgs
import com.blockchain.blockchaincard.viewmodel.managecard.ManageCardViewModel
import com.blockchain.blockchaincard.viewmodel.ordercard.OrderCardViewModel
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.koin.payloadScope
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.core.scope.Scope

abstract class BlockchainCardHostFragment : Fragment(), AndroidScopeComponent {

    override val scope: Scope = payloadScope

    val orderCardViewModel: OrderCardViewModel by sharedViewModel()
    val manageCardViewModel: ManageCardViewModel by sharedViewModel()

    val modelArgs: ModelConfigArgs by lazy {
        (arguments?.getParcelableArray(BLOCKCHAIN_CARD_LIST) as? Array<BlockchainCard>)?.toList()?.let { cards ->
            BlockchainCardArgs.CardArgs(
                cards = cards,
                cardProducts = (
                    arguments?.getParcelableArray(BLOCKCHAIN_CARD_PRODUCT_LIST)
                        as? Array<BlockchainCardProduct>
                    )?.toList() ?: emptyList(),
                preselectedCard = (
                    arguments?.getParcelable(BlockchainCardHostActivity.PRESELECTED_BLOCKCHAIN_CARD) as? BlockchainCard
                    )
            )
        } ?: (
            arguments?.getParcelableArray(BLOCKCHAIN_CARD_PRODUCT_LIST)
                as? Array<BlockchainCardProduct>
            )?.toList()?.let { products ->
            BlockchainCardArgs.ProductArgs(products)
        } ?: throw IllegalStateException("Missing card or product data")
    }

    companion object {
        const val PRESELECTED_BLOCKCHAIN_CARD = "PRESELECTED_BLOCKCHAIN_CARD"
        const val BLOCKCHAIN_CARD_LIST = "BLOCKCHAIN_CARD_LIST"
        const val BLOCKCHAIN_CARD_PRODUCT_LIST = "BLOCKCHAIN_CARD_PRODUCT_LIST"
    }
}
