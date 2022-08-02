package com.blockchain.blockchaincard.viewmodel

import com.blockchain.blockchaincard.domain.models.BlockchainCardAddress
import com.blockchain.blockchaincard.domain.models.BlockchainCardLegalDocument
import com.blockchain.blockchaincard.domain.models.BlockchainCardTransaction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed class BlockchainCardIntent : Intent<BlockchainCardModelState> {
    // Common
    object HideBottomSheet : BlockchainCardIntent()
    object SnackbarDismissed : BlockchainCardIntent()
    object LoadLegalDocuments : BlockchainCardIntent()

    // Order Card
    data class OrderCardKycComplete(val ssn: String) : BlockchainCardIntent()
    object RetryOrderCard : BlockchainCardIntent()
    object LinkCard : BlockchainCardIntent()
    object CreateCard : BlockchainCardIntent() {
        override fun isValidFor(modelState: BlockchainCardModelState): Boolean {
            return !modelState.ssn.isNullOrEmpty() && modelState.selectedCardProduct != null
        }
    }
    object OnSeeProductDetails : BlockchainCardIntent()
    object OnSeeProductLegalInfo : BlockchainCardIntent()
    object ManageCard : BlockchainCardIntent()
    object OrderCardKYCAddress : BlockchainCardIntent()
    object OrderCardSSNAddress : BlockchainCardIntent()
    data class OnSeeSingleLegalDocument(val legalDocument: BlockchainCardLegalDocument) : BlockchainCardIntent()
    object OnSeeLegalDocuments : BlockchainCardIntent()
    data class OnLegalDocSeen(val name: String) : BlockchainCardIntent()
    object OnFinishLegalDocReview : BlockchainCardIntent()

    // ManageCard
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
    object SeeTransactionControls : BlockchainCardIntent()
    object SeePersonalDetails : BlockchainCardIntent()
    object LoadResidentialAddress : BlockchainCardIntent()
    object SeeBillingAddress : BlockchainCardIntent()
    data class UpdateBillingAddress(val newAddress: BlockchainCardAddress) : BlockchainCardIntent()
    data class SeeTransactionDetails(val transaction: BlockchainCardTransaction) : BlockchainCardIntent()

    object DismissBillingAddressUpdateResult : BlockchainCardIntent()
    object SeeSupport : BlockchainCardIntent()
    object CloseCard : BlockchainCardIntent()
    object ConfirmCloseCard : BlockchainCardIntent()
    object LoadUserFirstAndLastName : BlockchainCardIntent()
    object LoadTransactions : BlockchainCardIntent()
    object RefreshTransactions : BlockchainCardIntent()
    object SeeCardLostPage : BlockchainCardIntent()
    object SeeFAQPage : BlockchainCardIntent()
    object SeeContactSupportPage : BlockchainCardIntent()
}
