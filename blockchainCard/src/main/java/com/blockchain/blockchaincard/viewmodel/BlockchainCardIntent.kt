package com.blockchain.blockchaincard.viewmodel

import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed class BlockchainCardIntent : Intent<BlockchainCardModelState> {
    // Order Card
    object OrderCard : BlockchainCardIntent()
    object LinkCard : BlockchainCardIntent()
    data class CreateCard(val productCode: String, val ssn: String) : BlockchainCardIntent()
    object OnSeeProductDetails : BlockchainCardIntent()
    object ManageCard : BlockchainCardIntent()
    object HideProductDetailsBottomSheet : BlockchainCardIntent()

    // ManageCard
    object DeleteCard : BlockchainCardIntent()
    object LockCard : BlockchainCardIntent()
    object UnlockCard : BlockchainCardIntent()
    object ManageCardDetails : BlockchainCardIntent()
    object LoadCardWidget : BlockchainCardIntent()
    object ChoosePaymentMethod : BlockchainCardIntent()
    object TopUp : BlockchainCardIntent()
    data class LinkSelectedAccount(val accountCurrencyNetworkTicker: String) : BlockchainCardIntent()
    object LoadLinkedAccount : BlockchainCardIntent()
    data class LoadAccountBalance(val tradingAccount: BlockchainAccount) : BlockchainCardIntent()
    data class LoadEligibleAccountsBalances(val eligibleAccounts: List<TradingAccount>) : BlockchainCardIntent()
}
