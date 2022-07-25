package com.blockchain.blockchaincard.viewmodel.ordercard

import com.blockchain.blockchaincard.domain.BlockchainCardRepository
import com.blockchain.blockchaincard.viewmodel.BlockchainCardArgs
import com.blockchain.blockchaincard.viewmodel.BlockchainCardErrorState
import com.blockchain.blockchaincard.viewmodel.BlockchainCardIntent
import com.blockchain.blockchaincard.viewmodel.BlockchainCardModelState
import com.blockchain.blockchaincard.viewmodel.BlockchainCardNavigationEvent
import com.blockchain.blockchaincard.viewmodel.BlockchainCardViewModel
import com.blockchain.blockchaincard.viewmodel.BlockchainCardViewState
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.flatMap
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
        errorState = state.errorState,
        card = state.card,
        selectedCardProduct = state.selectedCardProduct,
        residentialAddress = state.residentialAddress,
        ssn = state.ssn,
        countryStateList = state.countryStateList,
        legalDocuments = state.legalDocuments,
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
                onIntent(BlockchainCardIntent.LoadLegalDocuments)
                navigate(BlockchainCardNavigationEvent.OrderCardConfirm)
            }

            is BlockchainCardIntent.LoadResidentialAddress -> {
                blockchainCardRepository.getResidentialAddress().fold(
                    onSuccess = { address ->
                        updateState { it.copy(residentialAddress = address) }
                    },
                    onFailure = { error ->
                        Timber.e("Unable to get residential address: $error")
                        updateState { it.copy(errorState = BlockchainCardErrorState.SnackbarErrorState(error)) }
                    }
                )
            }

            is BlockchainCardIntent.SeeBillingAddress -> {
                modelState.residentialAddress?.let { address ->
                    blockchainCardRepository.getStatesList(address.country)
                        .doOnSuccess { states ->
                            updateState { it.copy(countryStateList = states) }
                        }
                        .doOnFailure {
                            Timber.e("Unable to get states: $it")
                        }
                }
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
                    onFailure = { error ->
                        Timber.e("Unable to update residential address: $error")
                        updateState { it.copy(errorState = BlockchainCardErrorState.ScreenErrorState(error)) }
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
                        modelState.legalDocuments?.let { documents ->
                            navigate(BlockchainCardNavigationEvent.CreateCardInProgress)
                            blockchainCardRepository.acceptLegalDocument(
                                documentName = "terms-and-conditions",
                                acceptedVersion = documents.termsAndConditions.version
                            ).flatMap {
                                blockchainCardRepository.createCard(productCode = product.productCode, ssn = ssn)
                                    .doOnFailure { error ->
                                        updateState {
                                            it.copy(
                                                errorState = BlockchainCardErrorState.ScreenErrorState(error)
                                            )
                                        }
                                        navigate(BlockchainCardNavigationEvent.CreateCardFailed)
                                    }
                                    .doOnSuccess { card ->
                                        updateState { it.copy(card = card) }
                                        navigate(BlockchainCardNavigationEvent.CreateCardSuccess)
                                    }
                            }.doOnFailure { error ->
                                Timber.e("Unable to update legal documents: $error")
                            }
                        }
                    }
                }
            }

            is BlockchainCardIntent.HideBottomSheet -> {
                navigate(BlockchainCardNavigationEvent.HideBottomSheet)
            }

            is BlockchainCardIntent.SnackbarDismissed -> {
                updateState { it.copy(errorState = null) }
            }

            is BlockchainCardIntent.ManageCard -> {
                modelState.card?.let {
                    navigate(BlockchainCardNavigationEvent.ManageCard(modelState.card))
                }
            }

            is BlockchainCardIntent.LoadLegalDocuments -> {
                blockchainCardRepository.getLegalDocuments()
                    .doOnSuccess { documents ->
                        Timber.d("Legal documents loaded: $documents")
                        updateState { it.copy(legalDocuments = documents) }
                    }
                    .doOnFailure { error ->
                        Timber.e("Unable to get legal documents: $error")
                    }
            }

            is BlockchainCardIntent.OnSeeTermsAndConditions -> {
                modelState.legalDocuments?.let { documents ->
                    val documentsUpdated = documents.copy(
                        termsAndConditions = documents.termsAndConditions.copy(seen = true)
                    )
                    updateState { it.copy(legalDocuments = documentsUpdated) }

                    navigate(BlockchainCardNavigationEvent.SeeTermsAndConditions)
                }
            }

            is BlockchainCardIntent.TermsAndConditionsAccepted -> {
                modelState.legalDocuments?.let { documents ->
                    blockchainCardRepository.acceptLegalDocument(
                        documentName = "terms-and-conditions",
                        acceptedVersion = documents.termsAndConditions.version
                    ).doOnSuccess { documentsUpdated ->
                        Timber.d("Legal documents updated: $documentsUpdated")
                        updateState { it.copy(legalDocuments = documents) }
                        navigate(BlockchainCardNavigationEvent.OrderCardConfirm)
                    }.doOnFailure { error ->
                        Timber.e("Unable to update legal documents: $error")
                    }
                }
            }

            is BlockchainCardIntent.OnSeeShortFormDisclosure -> {
                modelState.legalDocuments?.let { documents ->
                    val documentsUpdated = documents.copy(
                        shortFormDisclosure = documents.shortFormDisclosure.copy(seen = true)
                    )
                    updateState { it.copy(legalDocuments = documentsUpdated) }

                    navigate(BlockchainCardNavigationEvent.SeeShortFormDisclosure)
                }
            }
        }
    }
}
