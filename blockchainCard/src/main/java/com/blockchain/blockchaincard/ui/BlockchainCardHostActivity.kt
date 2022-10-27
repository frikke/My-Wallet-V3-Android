package com.blockchain.blockchaincard.ui

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
        (intent?.getSerializableExtra(BLOCKCHAIN_CARD_LIST) as? List<BlockchainCard>)?.let { cards ->
            BlockchainCardArgs.CardArgs(
                cards = cards,
                cardProducts = (
                    intent?.getSerializableExtra(BLOCKCHAIN_CARD_PRODUCT_LIST)
                        as? List<BlockchainCardProduct>
                    ) ?: emptyList(),
                preselectedCard = (intent?.getParcelableExtra(PRESELECTED_BLOCKCHAIN_CARD) as? BlockchainCard)
            )
        } ?: (
            intent?.getParcelableExtra(BLOCKCHAIN_CARD_PRODUCT_LIST)
                as? List<BlockchainCardProduct>
            )?.let { product ->
            BlockchainCardArgs.ProductArgs(product)
        } ?: throw IllegalStateException("Missing card or product data")
    }

    companion object {
        const val PRESELECTED_BLOCKCHAIN_CARD = "PRESELECTED_BLOCKCHAIN_CARD"
        const val BLOCKCHAIN_CARD_LIST = "BLOCKCHAIN_CARD_LIST"
        const val BLOCKCHAIN_CARD_PRODUCT_LIST = "BLOCKCHAIN_CARD_PRODUCT_LIST"
    }

    abstract fun startBuy(asset: AssetInfo)

    abstract fun startDeposit(account: FiatAccount)

    abstract fun startKycAddressVerification(address: BlockchainCardAddress)

    abstract fun updateKycAddress(address: BlockchainCardAddress)

    abstract fun startAddCardToGoogleWallet(pushTokenizeData: BlockchainCardGoogleWalletPushTokenizeData)

    abstract fun startOrderCardFlow()

    abstract fun orderCardFlowComplete(blockchainCard: BlockchainCard)

    abstract fun startManageCardFlow(
        blockchainCardProducts: List<BlockchainCardProduct>,
        blockchainCards: List<BlockchainCard>,
        preselectedCard: BlockchainCard?
    )

    abstract fun openUrl(url: String)
}
