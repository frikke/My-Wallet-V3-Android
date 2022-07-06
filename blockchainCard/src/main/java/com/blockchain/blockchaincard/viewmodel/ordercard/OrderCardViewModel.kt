package com.blockchain.blockchaincard.viewmodel.ordercard

import com.blockchain.blockchaincard.domain.BlockchainCardRepository
import com.blockchain.blockchaincard.viewmodel.BlockchainCardArgs
import com.blockchain.blockchaincard.viewmodel.BlockchainCardIntent
import com.blockchain.blockchaincard.viewmodel.BlockchainCardModelState
import com.blockchain.blockchaincard.viewmodel.BlockchainCardNavigationEvent
import com.blockchain.blockchaincard.viewmodel.BlockchainCardViewModel
import com.blockchain.blockchaincard.viewmodel.BlockchainCardViewState
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.fold
import timber.log.Timber

class OrderCardViewModel(private val blockchainCardRepository: BlockchainCardRepository) : BlockchainCardViewModel() {

    override fun viewCreated(args: ModelConfigArgs) {
        when (args) {
            is BlockchainCardArgs.CardArgs -> {
                updateState { it.copy(card = args.card) }
            }

            is BlockchainCardArgs.ProductArgs -> {
                updateState { it.copy(selectedCardProduct = args.product) }
            }
        }
    }

    override fun reduce(state: BlockchainCardModelState): BlockchainCardViewState = BlockchainCardViewState(
        card = state.card,
        selectedCardProduct = state.selectedCardProduct,
        residentialAddress = state.residentialAddress,
        ssn = state.ssn,
    )

    override suspend fun handleIntent(
        modelState: BlockchainCardModelState,
        intent: BlockchainCardIntent
    ) {
        when (intent) {

            is BlockchainCardIntent.OrderCardKYCAddress -> {
                onIntent(BlockchainCardIntent.LoadResidentialAddress)
                navigate(BlockchainCardNavigationEvent.OrderCardKycAddress)
            }

            is BlockchainCardIntent.OrderCardSSNAddress -> {
                navigate(BlockchainCardNavigationEvent.OrderCardKycSSN)
            }

            is BlockchainCardIntent.OrderCardKycComplete -> {
                updateState { it.copy(ssn = intent.ssn) }
                navigate(BlockchainCardNavigationEvent.OrderCardConfirm)
            }

            is BlockchainCardIntent.LoadResidentialAddress -> {
                blockchainCardRepository.getResidentialAddress().fold(
                    onSuccess = { address ->
                        updateState { it.copy(residentialAddress = address) }
                    },
                    onFailure = {
                        Timber.e("Unable to get residential address: $it")
                    }
                )
            }

            is BlockchainCardIntent.SeeBillingAddress -> {
                navigate(BlockchainCardNavigationEvent.SeeBillingAddress)
            }

            is BlockchainCardIntent.UpdateBillingAddress -> {
                blockchainCardRepository.updateResidentialAddress(
                    intent.newAddress
                ).fold(
                    onSuccess = { newAddress ->
                        updateState { it.copy(residentialAddress = newAddress) }
                        navigate(BlockchainCardNavigationEvent.BillingAddressUpdated(success = true))
                    },
                    onFailure = {
                        Timber.e("Unable to update residential address: $it")
                        navigate(BlockchainCardNavigationEvent.BillingAddressUpdated(success = false))
                    }
                )
            }

            is BlockchainCardIntent.DismissBillingAddressUpdateResult -> {
                navigate(BlockchainCardNavigationEvent.DismissBillingAddressUpdateResult)
            }

            is BlockchainCardIntent.RetryOrderCard -> {
                navigate(BlockchainCardNavigationEvent.RetryOrderCard)
            }

            is BlockchainCardIntent.OnSeeProductDetails -> {
                navigate(BlockchainCardNavigationEvent.SeeProductDetails)
            }

            is BlockchainCardIntent.OnSeeProductLegalInfo -> {
                navigate(BlockchainCardNavigationEvent.SeeProductLegalInfo)
            }

            is BlockchainCardIntent.CreateCard -> {
                modelState.selectedCardProduct?.let { product ->
                    modelState.ssn?.let { ssn ->
                        navigate(BlockchainCardNavigationEvent.CreateCardInProgress)
                        blockchainCardRepository.createCard(productCode = product.productCode, ssn = ssn)
                            .doOnFailure {
                                // Todo(labreu): update state's error here OR pass it to the destination
                                navigate(BlockchainCardNavigationEvent.CreateCardFailed)
                            }
                            .doOnSuccess { card ->
                                updateState { it.copy(card = card) }
                                navigate(BlockchainCardNavigationEvent.CreateCardSuccess)
                            }
                    }
                }
            }

            is BlockchainCardIntent.HideBottomSheet -> {
                navigate(BlockchainCardNavigationEvent.HideBottomSheet)
            }

            is BlockchainCardIntent.ManageCard -> {
                modelState.card?.let {
                    navigate(BlockchainCardNavigationEvent.ManageCard(modelState.card))
                }
            }
        }
    }
}
