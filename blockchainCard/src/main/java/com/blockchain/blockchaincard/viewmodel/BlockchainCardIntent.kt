package com.blockchain.blockchaincard.viewmodel

import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddress
import com.blockchain.blockchaincard.domain.models.BlockchainCardLegalDocument
import com.blockchain.blockchaincard.domain.models.BlockchainCardProduct
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
    object HowToOrderCard : BlockchainCardIntent()
    object OrderCardPerformKyc : BlockchainCardIntent()
    object OrderCardKycComplete : BlockchainCardIntent()
    object RetryOrderCard : BlockchainCardIntent()
    object LinkCard : BlockchainCardIntent()
    object CreateCard : BlockchainCardIntent()
    object OnSeeProductDetails : BlockchainCardIntent()
    object OnSeeProductLegalInfo : BlockchainCardIntent()
    data class ManageCard(val card: BlockchainCard) : BlockchainCardIntent()
    object OrderCardKYCAddress : BlockchainCardIntent()
    object OrderCardKycSSN : BlockchainCardIntent()
    data class OnSeeSingleLegalDocument(val legalDocument: BlockchainCardLegalDocument) : BlockchainCardIntent()
    object OnSeeLegalDocuments : BlockchainCardIntent()
    data class OnLegalDocSeen(val name: String) : BlockchainCardIntent()
    object OnFinishLegalDocReview : BlockchainCardIntent()
    data class OnOrderCardConfirm(val selectedProduct: BlockchainCardProduct) : BlockchainCardIntent()
    object OnOrderCardFlowComplete : BlockchainCardIntent()
    object OnChooseProduct : BlockchainCardIntent()
    object LoadKycStatus : BlockchainCardIntent()
    data class UpdateSSN(val ssn: String) : BlockchainCardIntent()

    // ManageCard
    object SelectCard : BlockchainCardIntent()
    object OrderCard : BlockchainCardIntent()
    object LockCard : BlockchainCardIntent()
    object UnlockCard : BlockchainCardIntent()
    data class ManageCardDetails(val card: BlockchainCard) : BlockchainCardIntent()
    object LoadCardWidget : BlockchainCardIntent()
    object FundingAccountClicked : BlockchainCardIntent()
    object ChoosePaymentMethod : BlockchainCardIntent()
    object AddFunds : BlockchainCardIntent()
    data class LinkSelectedAccount(val accountCurrencyNetworkTicker: String) : BlockchainCardIntent()
    object LoadLinkedAccount : BlockchainCardIntent()
    data class LoadAccountBalance(val tradingAccount: BlockchainAccount) : BlockchainCardIntent()
    data class LoadEligibleAccountsBalances(val eligibleAccounts: List<TradingAccount>) : BlockchainCardIntent()
    object SeeTransactionControls : BlockchainCardIntent()
    object SeePersonalDetails : BlockchainCardIntent()
    object LoadResidentialAddress : BlockchainCardIntent()
    object SeeBillingAddress : BlockchainCardIntent()
    object OnChangeShippingAddress : BlockchainCardIntent()
    data class UpdateAddress(val newAddress: BlockchainCardAddress) : BlockchainCardIntent()
    object SeeAllTransactions : BlockchainCardIntent()
    data class SeeTransactionDetails(val transaction: BlockchainCardTransaction) : BlockchainCardIntent()
    object DismissBillingAddressUpdateResult : BlockchainCardIntent()
    object SeeSupport : BlockchainCardIntent()
    object CloseCard : BlockchainCardIntent()
    object ConfirmCloseCard : BlockchainCardIntent()
    object LoadUserFirstAndLastName : BlockchainCardIntent()
    object LoadTransactions : BlockchainCardIntent()
    object LoadNextTransactionsPage : BlockchainCardIntent()
    object RefreshTransactions : BlockchainCardIntent()
    object SeeCardLostPage : BlockchainCardIntent()
    object SeeFAQPage : BlockchainCardIntent()
    object SeeContactSupportPage : BlockchainCardIntent()
    object LoadGoogleWalletDetails : BlockchainCardIntent()
    object LoadGoogleWalletTokenizationStatus : BlockchainCardIntent()
    object LoadGoogleWalletPushTokenizeData : BlockchainCardIntent()
    object GoogleWalletAddCardSuccess : BlockchainCardIntent()
    object GoogleWalletAddCardFailed : BlockchainCardIntent()
    object LoadDefaultCard : BlockchainCardIntent()
    data class SaveCardAsDefault(val defaultCardId: String) : BlockchainCardIntent()
    object LoadProducts : BlockchainCardIntent()
    object LoadCards : BlockchainCardIntent()
    object LoadCardOrderState : BlockchainCardIntent()
    object ActivateCard : BlockchainCardIntent()
    object OnCardActivated : BlockchainCardIntent()
    object OnFinishCardActivation : BlockchainCardIntent()
    object SeeDocuments : BlockchainCardIntent()
    object LoadCardStatements : BlockchainCardIntent()
    data class LoadCardStatementUrl(val statementId: String) : BlockchainCardIntent()
    data class OpenDocumentUrl(val url: String) : BlockchainCardIntent()
    data class WebMessageReceived(val message: String) : BlockchainCardIntent()
    object SetPin : BlockchainCardIntent()
    object OnPinSetSuccess : BlockchainCardIntent()
    object OnFinishSetPin : BlockchainCardIntent()
}
