package com.blockchain.blockchaincard.viewmodel

import androidx.navigation.NavHostController
import com.blockchain.blockchaincard.domain.models.BlockchainCard
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddress
import com.blockchain.blockchaincard.domain.models.BlockchainCardGoogleWalletPushTokenizeData
import com.blockchain.blockchaincard.ui.BlockchainCardHostActivity
import com.blockchain.blockchaincard.ui.BlockchainCardHostFragment
import com.blockchain.coincore.FiatAccount
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationDestination
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationRouter
import com.blockchain.extensions.exhaustive
import info.blockchain.balance.AssetInfo

class BlockchainCardNavigationRouter(override val navController: NavHostController) :
    ComposeNavigationRouter<BlockchainCardNavigationEvent> {

    override fun route(navigationEvent: BlockchainCardNavigationEvent) {
        var destination: BlockchainCardDestination = BlockchainCardDestination.NoDestination

        @Suppress("IMPLICIT_CAST_TO_ANY")
        when (navigationEvent) {

            is BlockchainCardNavigationEvent.ShowHowToOrderCard -> {
                destination = BlockchainCardDestination.HowToOrderCardDestination
            }

            is BlockchainCardNavigationEvent.OrderCardKycAddress -> {
                destination = BlockchainCardDestination.OrderCardKycAddressDestination
            }

            is BlockchainCardNavigationEvent.OrderCardKycSSN -> {
                destination = BlockchainCardDestination.OrderCardKycSSNDestination
            }

            is BlockchainCardNavigationEvent.ChooseCardProduct -> {
                destination = BlockchainCardDestination.ChooseCardProductDestination
            }

            is BlockchainCardNavigationEvent.ReviewAndSubmitCard -> {
                // Check if this destination is already in the backstack (popBackStack returns true)
                // If not, create it and navigate to it
                // If yes pop to it and return null.
                if (!navController.popBackStack(BlockchainCardDestination.ReviewAndSubmitCardDestination.route, false))
                    destination = BlockchainCardDestination.ReviewAndSubmitCardDestination
                else null
            }

            is BlockchainCardNavigationEvent.RetryOrderCard -> {
                navController.popBackStack(BlockchainCardDestination.OrderCardDestination.route, false)
            }

            is BlockchainCardNavigationEvent.SeeProductDetails -> {
                destination = BlockchainCardDestination.SeeProductDetailsDestination
            }

            is BlockchainCardNavigationEvent.SeeProductLegalInfo -> {
                destination = BlockchainCardDestination.SeeProductLegalInfoDestination
            }

            is BlockchainCardNavigationEvent.HideBottomSheet -> {
                navController.popBackStack()
            }

            is BlockchainCardNavigationEvent.CreateCardInProgress -> {
                destination = BlockchainCardDestination.CreateCardInProgressDestination
            }

            is BlockchainCardNavigationEvent.CreateCardSuccess -> {
                navController.popBackStack(BlockchainCardDestination.OrderCardDestination.route, true)
                destination = BlockchainCardDestination.CreateCardSuccessDestination
            }

            is BlockchainCardNavigationEvent.CreateCardFailed -> {
                navController.popBackStack(BlockchainCardDestination.OrderCardDestination.route, false)
                destination = BlockchainCardDestination.CreateCardFailedDestination
            }

            is BlockchainCardNavigationEvent.ManageCard -> {
                /*
                 Since we are navigating into the manage card screen which uses a different view model we must replace
                 the current fragment with a new instance of BlockchainCardFragment that uses the correct args
                */

                val fragmentManager = (navController.context as? BlockchainActivity)?.supportFragmentManager
                val fragmentOld = fragmentManager?.fragments?.first { it is BlockchainCardHostFragment }

                fragmentOld?.let {
                    replaceCurrentFragment(
                        containerViewId = fragmentOld.id,
                        fragment = (fragmentOld as BlockchainCardHostFragment).newInstance(navigationEvent.card),
                        addToBackStack = false
                    )
                }
            }

            is BlockchainCardNavigationEvent.ManageCardDetails -> {
                destination = BlockchainCardDestination.ManageCardDetailsDestination
            }

            is BlockchainCardNavigationEvent.ChoosePaymentMethod -> {
                destination = BlockchainCardDestination.ChoosePaymentMethodDestination
            }

            is BlockchainCardNavigationEvent.CardClosed -> {
                (navController.context as? BlockchainCardHostActivity)?.finish()
            }

            is BlockchainCardNavigationEvent.ChooseFundingAccountAction -> {
                destination = BlockchainCardDestination.FundingAccountActionsDestination
            }

            is BlockchainCardNavigationEvent.TopUpCrypto -> {
                (navController.context as? BlockchainCardHostActivity)?.startBuy(navigationEvent.asset)
            }

            is BlockchainCardNavigationEvent.TopUpFiat -> {
                (navController.context as? BlockchainCardHostActivity)?.startDeposit(navigationEvent.account)
            }

            is BlockchainCardNavigationEvent.SeeTransactionControls -> {
                destination = BlockchainCardDestination.TransactionControlsDestination
            }

            is BlockchainCardNavigationEvent.SeePersonalDetails -> {
                destination = BlockchainCardDestination.PersonalDetailsDestination
            }

            is BlockchainCardNavigationEvent.SeeBillingAddress -> {
                (navController.context as? BlockchainCardHostActivity)?.startKycAddressVerification(
                    address = navigationEvent.address
                )
            }

            is BlockchainCardNavigationEvent.SeeSupport -> {
                destination = BlockchainCardDestination.SupportDestination
            }

            is BlockchainCardNavigationEvent.CloseCard -> {
                destination = BlockchainCardDestination.CloseCardDestination
            }

            is BlockchainCardNavigationEvent.BillingAddressUpdated -> {
                navController.popBackStack(BlockchainCardDestination.BillingAddressDestination.route, true)
                if (navigationEvent.success)
                    destination = BlockchainCardDestination.BillingAddressUpdateSuccessDestination
                else
                    destination = BlockchainCardDestination.BillingAddressUpdateFailedDestination
            }

            is BlockchainCardNavigationEvent.DismissBillingAddressUpdateResult -> {
                navController.popBackStack()
            }

            is BlockchainCardNavigationEvent.SeeAllTransactions -> {
                destination = BlockchainCardDestination.AllTransactionsDestination
            }

            is BlockchainCardNavigationEvent.SeeTransactionDetails -> {
                destination = BlockchainCardDestination.TransactionDetailsDestination
            }

            is BlockchainCardNavigationEvent.SeeSingleLegalDocument -> {
                destination = BlockchainCardDestination.SingleLegalDocumentDestination
            }

            is BlockchainCardNavigationEvent.SeeLegalDocuments -> {
                destination = BlockchainCardDestination.LegalDocumentsDestination
            }

            is BlockchainCardNavigationEvent.FinishLegalDocReview -> {
                navController.popBackStack(
                    route = BlockchainCardDestination.ReviewAndSubmitCardDestination.route,
                    inclusive = false
                )
            }

            is BlockchainCardNavigationEvent.SeeCardLostPage -> {
                destination = BlockchainCardDestination.CardLostPageDestination
            }

            is BlockchainCardNavigationEvent.SeeFAQPage -> {
                destination = BlockchainCardDestination.FAQPageDestination
            }

            is BlockchainCardNavigationEvent.SeeContactSupportPage -> {
                destination = BlockchainCardDestination.ContactSupportPageDestination
            }

            is BlockchainCardNavigationEvent.AddCardToGoogleWallet -> {
                (navController.context as BlockchainCardHostActivity).startAddCardToGoogleWallet(
                    pushTokenizeData = navigationEvent.blockchainCardTokenizationRequest
                )
            }
        }.exhaustive

        if (destination !is BlockchainCardDestination.NoDestination)
            navController.navigate(destination.route)
    }
}

sealed class BlockchainCardNavigationEvent : NavigationEvent {

    // Order Card

    object ShowHowToOrderCard : BlockchainCardNavigationEvent()

    object OrderCardKycAddress : BlockchainCardNavigationEvent()

    object OrderCardKycSSN : BlockchainCardNavigationEvent()

    object ChooseCardProduct : BlockchainCardNavigationEvent()

    object ReviewAndSubmitCard : BlockchainCardNavigationEvent()

    object RetryOrderCard : BlockchainCardNavigationEvent()

    object CreateCardInProgress : BlockchainCardNavigationEvent()

    object CreateCardSuccess : BlockchainCardNavigationEvent()

    object CreateCardFailed : BlockchainCardNavigationEvent()

    object HideBottomSheet : BlockchainCardNavigationEvent()

    object SeeProductDetails : BlockchainCardNavigationEvent()

    object SeeProductLegalInfo : BlockchainCardNavigationEvent()

    object FinishLegalDocReview : BlockchainCardNavigationEvent()

    // Manage Card
    data class ManageCard(val card: BlockchainCard) : BlockchainCardNavigationEvent()

    object ManageCardDetails : BlockchainCardNavigationEvent()

    object ChoosePaymentMethod : BlockchainCardNavigationEvent()

    object ChooseFundingAccountAction : BlockchainCardNavigationEvent()

    data class TopUpCrypto(val asset: AssetInfo) : BlockchainCardNavigationEvent()

    data class TopUpFiat(val account: FiatAccount) : BlockchainCardNavigationEvent()

    object SeeTransactionControls : BlockchainCardNavigationEvent()

    object SeePersonalDetails : BlockchainCardNavigationEvent()

    data class SeeBillingAddress(val address: BlockchainCardAddress) : BlockchainCardNavigationEvent()

    object SeeSupport : BlockchainCardNavigationEvent()

    object CloseCard : BlockchainCardNavigationEvent()

    object CardClosed : BlockchainCardNavigationEvent()

    data class BillingAddressUpdated(val success: Boolean) : BlockchainCardNavigationEvent()

    object DismissBillingAddressUpdateResult : BlockchainCardNavigationEvent()

    object SeeAllTransactions : BlockchainCardNavigationEvent()

    object SeeTransactionDetails : BlockchainCardNavigationEvent()

    object SeeSingleLegalDocument : BlockchainCardNavigationEvent()

    object SeeLegalDocuments : BlockchainCardNavigationEvent()

    object SeeCardLostPage : BlockchainCardNavigationEvent()

    object SeeFAQPage : BlockchainCardNavigationEvent()

    object SeeContactSupportPage : BlockchainCardNavigationEvent()

    data class AddCardToGoogleWallet(
        val blockchainCardTokenizationRequest: BlockchainCardGoogleWalletPushTokenizeData
    ) : BlockchainCardNavigationEvent()
}

sealed class BlockchainCardDestination(override val route: String) : ComposeNavigationDestination {

    object NoDestination : BlockchainCardDestination(route = "")

    object OrderCardDestination : BlockchainCardDestination(route = "order_card")

    object HowToOrderCardDestination : BlockchainCardDestination(route = "how_to_order_card")

    object OrderCardKycAddressDestination : BlockchainCardDestination(route = "order_card_kyc_address")

    object OrderCardKycSSNDestination : BlockchainCardDestination(route = "order_card_kyc_ssn")

    object ChooseCardProductDestination : BlockchainCardDestination(route = "choose_card_product")

    object ReviewAndSubmitCardDestination : BlockchainCardDestination(route = "review_and_submit")

    object CreateCardInProgressDestination : BlockchainCardDestination(route = "create_card_in_progress")

    object CreateCardSuccessDestination : BlockchainCardDestination(route = "create_card_success")

    object CreateCardFailedDestination : BlockchainCardDestination(route = "create_card_failed")

    // TODO(labreu): these should just be named "ProductDetailsDestination" and "ProductLegalInfoDestination"
    object SeeProductDetailsDestination : BlockchainCardDestination(route = "product_details")

    object SeeProductLegalInfoDestination : BlockchainCardDestination(route = "product_legal_info")

    object ManageCardDestination : BlockchainCardDestination(route = "manage_card")

    object ManageCardDetailsDestination : BlockchainCardDestination(route = "manage_card_details")

    object FundingAccountActionsDestination : BlockchainCardDestination(route = "funding_account_actions")

    object ChoosePaymentMethodDestination : BlockchainCardDestination(route = "choose_payment_method")

    object TransactionControlsDestination : BlockchainCardDestination(route = "transaction_controls")

    object PersonalDetailsDestination : BlockchainCardDestination(route = "personal_details")

    object BillingAddressDestination : BlockchainCardDestination(route = "billing_address")

    object SupportDestination : BlockchainCardDestination(route = "support")

    object CloseCardDestination : BlockchainCardDestination(route = "close_card")

    object BillingAddressUpdateSuccessDestination :
        BlockchainCardDestination(route = "billing_address_update_success")

    object BillingAddressUpdateFailedDestination :
        BlockchainCardDestination(route = "billing_address_update_failed")

    object AllTransactionsDestination : BlockchainCardDestination(route = "all_transactions")

    object TransactionDetailsDestination : BlockchainCardDestination(route = "transaction_details")

    object SingleLegalDocumentDestination : BlockchainCardDestination(route = "single_legal_document")

    object LegalDocumentsDestination : BlockchainCardDestination(route = "legal_documents")

    object CardLostPageDestination : BlockchainCardDestination(route = "card_lost_page")

    object FAQPageDestination : BlockchainCardDestination(route = "faq_page")

    object ContactSupportPageDestination : BlockchainCardDestination(route = "contact_support_page")
}
