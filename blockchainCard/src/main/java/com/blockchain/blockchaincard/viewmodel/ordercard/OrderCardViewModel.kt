package com.blockchain.blockchaincard.viewmodel.ordercard

import com.blockchain.blockchaincard.domain.BlockchainCardRepository
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddress
import com.blockchain.blockchaincard.domain.models.BlockchainCardAddressType
import com.blockchain.blockchaincard.domain.models.BlockchainCardKycErrorField
import com.blockchain.blockchaincard.domain.models.BlockchainCardKycState
import com.blockchain.blockchaincard.domain.models.BlockchainCardKycStatus
import com.blockchain.blockchaincard.domain.models.BlockchainCardKycUpdate
import com.blockchain.blockchaincard.domain.models.BlockchainCardType
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
import timber.log.Timber

class OrderCardViewModel(private val blockchainCardRepository: BlockchainCardRepository) : BlockchainCardViewModel() {

    override fun viewCreated(args: ModelConfigArgs) {
        when (args) {
            is BlockchainCardArgs.ProductArgs -> {
                updateState { it.copy(cardProductList = args.products) }
                onIntent(BlockchainCardIntent.LoadKycStatus)
                onIntent(BlockchainCardIntent.LoadResidentialAddress)
            }

            else -> {
                throw IllegalStateException("OrderCardViewModel the provided arguments are not valid")
            }
        }
    }

    override fun reduce(state: BlockchainCardModelState): BlockchainCardViewState = BlockchainCardViewState(
        errorState = state.errorState,
        currentCard = state.currentCard,
        cardProductList = state.cardProductList,
        selectedCardProduct = state.selectedCardProduct,
        residentialAddress = state.residentialAddress,
        ssn = state.ssn,
        countryStateList = state.countryStateList,
        legalDocuments = state.legalDocuments,
        isLegalDocReviewComplete = state.isLegalDocReviewComplete,
        singleLegalDocumentToSee = state.singleLegalDocumentToSee,
        isAddressLoading = state.isAddressLoading,
        userFirstAndLastName = state.userFirstAndLastName,
        shippingAddress = state.shippingAddress,
        kycStatus = state.kycStatus
    )

    override suspend fun handleIntent(
        modelState: BlockchainCardModelState,
        intent: BlockchainCardIntent
    ) {
        when (intent) {
            is BlockchainCardIntent.LoadKycStatus -> {
                blockchainCardRepository.getKycStatus()
                    .doOnSuccess { kycStatus ->
                        updateState { it.copy(kycStatus = kycStatus) }

                        when (kycStatus.state) {
                            BlockchainCardKycState.PENDING -> {
                                navigate(BlockchainCardNavigationEvent.OrderCardKycPending)
                            }

                            BlockchainCardKycState.FAILURE -> {
                                navigate(BlockchainCardNavigationEvent.OrderCardKycFailure)
                            }

                            else -> {
                                navigate(BlockchainCardNavigationEvent.ShowOrderCardIntro)
                            }
                        }
                    }.doOnFailure { error ->
                        Timber.e("Unable to get kyc status: $error")
                        updateState {
                            it.copy(
                                errorState = BlockchainCardErrorState.SnackbarErrorState(error)
                            )
                        }
                    }
            }

            is BlockchainCardIntent.HowToOrderCard -> {
                navigate(BlockchainCardNavigationEvent.ShowHowToOrderCard)
            }

            is BlockchainCardIntent.OrderCardPerformKyc -> {
                modelState.kycStatus?.let { kycStatus ->
                    when (kycStatus.state) {
                        BlockchainCardKycState.FAILURE,
                        BlockchainCardKycState.UNVERIFIED -> {
                            onIntent(BlockchainCardIntent.OrderCardKYCAddress)
                        }

                        else -> {
                            navigate(BlockchainCardNavigationEvent.ChooseCardProduct)
                        }
                    }
                }
            }

            is BlockchainCardIntent.OrderCardKYCAddress -> {
                modelState.kycStatus?.let { kycStatus ->
                    if (kycStatus.shouldUpdateAddress()) {
                        navigate(BlockchainCardNavigationEvent.OrderCardKycAddress)
                    } else {
                        onIntent(BlockchainCardIntent.OrderCardKycSSN)
                    }
                }
            }

            is BlockchainCardIntent.OrderCardKycSSN -> {
                modelState.kycStatus?.let { kycStatus ->
                    if (kycStatus.shouldUpdateSSN()) {
                        navigate(BlockchainCardNavigationEvent.OrderCardKycSSN)
                    } else {
                        onIntent(BlockchainCardIntent.OrderCardKycComplete)
                    }
                }
            }

            is BlockchainCardIntent.UpdateSSN -> {
                updateState { it.copy(ssn = intent.ssn) }
                onIntent(BlockchainCardIntent.OrderCardKycComplete)
            }

            is BlockchainCardIntent.OrderCardKycComplete -> {

                modelState.kycStatus?.let { kycStatus ->

                    if (kycStatus.state == BlockchainCardKycState.PENDING) {
                        navigate(BlockchainCardNavigationEvent.OrderCardKycPendingComplete)
                    } else {
                        val kycUpdate = BlockchainCardKycUpdate(
                            address = if (kycStatus.shouldUpdateAddress()) modelState.residentialAddress else null,
                            ssn = if (kycStatus.shouldUpdateSSN()) modelState.ssn else null
                        )

                        blockchainCardRepository.updateKyc(kycUpdate)
                            .doOnSuccess { updatedKycStatus ->
                                updateState { it.copy(kycStatus = updatedKycStatus) }
                                navigate(BlockchainCardNavigationEvent.ChooseCardProduct)
                            }
                            .doOnFailure { error ->
                                Timber.e("Unable to update kyc: $error")
                                updateState {
                                    it.copy(
                                        errorState = BlockchainCardErrorState.SnackbarErrorState(error)
                                    )
                                }
                                navigate(BlockchainCardNavigationEvent.ChooseCardProduct)
                            }
                    }
                }
            }

            is BlockchainCardIntent.LoadResidentialAddress -> {

                updateState { it.copy(isAddressLoading = true) }

                blockchainCardRepository.getResidentialAddress()
                    .doOnSuccess { address ->
                        updateState { it.copy(residentialAddress = address, isAddressLoading = false) }
                    }
                    .doOnFailure { error ->
                        Timber.e("Unable to get residential address: $error")
                        updateState {
                            it.copy(
                                errorState = BlockchainCardErrorState.SnackbarErrorState(error),
                                isAddressLoading = false
                            )
                        }
                    }
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
                    navigate(BlockchainCardNavigationEvent.SeeAddress(address))
                }
            }

            is BlockchainCardIntent.OnChangeShippingAddress -> {
                modelState.shippingAddress?.let { address ->
                    navigate(BlockchainCardNavigationEvent.SeeAddress(address))
                }
            }

            is BlockchainCardIntent.UpdateAddress -> {

                when (intent.newAddress.addressType) {
                    BlockchainCardAddressType.BILLING -> {
                        updateState { it.copy(residentialAddress = intent.newAddress, isAddressLoading = false) }
                        navigate(BlockchainCardNavigationEvent.BillingAddressUpdated(success = true))
                    }

                    BlockchainCardAddressType.SHIPPING -> {
                        updateState { it.copy(shippingAddress = intent.newAddress) }
                    }
                }
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

            is BlockchainCardIntent.OnOrderCardConfirm -> {
                modelState.residentialAddress?.let { residentialAddress ->
                    val shippingAddress = when (intent.selectedProduct.type) {
                        BlockchainCardType.PHYSICAL -> BlockchainCardAddress(
                            line1 = residentialAddress.line1,
                            line2 = residentialAddress.line2,
                            postCode = residentialAddress.postCode,
                            city = residentialAddress.city,
                            state = residentialAddress.state,
                            country = residentialAddress.country,
                            addressType = BlockchainCardAddressType.SHIPPING
                        )
                        else -> null
                    }

                    updateState {
                        it.copy(
                            selectedCardProduct = intent.selectedProduct,
                            shippingAddress = shippingAddress
                        )
                    }
                    onIntent(BlockchainCardIntent.LoadLegalDocuments)
                    onIntent(BlockchainCardIntent.LoadUserFirstAndLastName)
                    navigate(BlockchainCardNavigationEvent.ReviewAndSubmitCard)
                }
            }

            is BlockchainCardIntent.CreateCard -> {
                if (modelState.selectedCardProduct != null && modelState.legalDocuments != null) {
                    navigate(BlockchainCardNavigationEvent.CreateCardInProgress)
                    blockchainCardRepository.acceptLegalDocuments(
                        modelState.legalDocuments
                    ).flatMap {
                        blockchainCardRepository.createCard(
                            productCode = modelState.selectedCardProduct.productCode,
                            shippingAddress = modelState.shippingAddress
                        )
                            .doOnFailure { error ->
                                updateState {
                                    it.copy(
                                        errorState = BlockchainCardErrorState.ScreenErrorState(error)
                                    )
                                }
                                navigate(BlockchainCardNavigationEvent.CreateCardFailed)
                            }
                            .doOnSuccess { card ->
                                updateState { it.copy(currentCard = card) }
                                navigate(BlockchainCardNavigationEvent.CreateCardSuccess)
                            }
                    }.doOnFailure { error ->
                        Timber.e("Unable to update legal documents: $error")
                        updateState {
                            it.copy(
                                errorState = BlockchainCardErrorState.ScreenErrorState(error)
                            )
                        }
                        navigate(BlockchainCardNavigationEvent.CreateCardFailed)
                    }
                }
            }

            is BlockchainCardIntent.HideBottomSheet -> {
                navigate(BlockchainCardNavigationEvent.HideBottomSheet)
            }

            is BlockchainCardIntent.SnackbarDismissed -> {
                updateState { it.copy(errorState = null) }
            }

            is BlockchainCardIntent.LoadLegalDocuments -> {
                blockchainCardRepository.getLegalDocuments()
                    .doOnSuccess { documents ->
                        Timber.d("Legal documents loaded: $documents")
                        updateState { it.copy(legalDocuments = documents) }
                    }
                    .doOnFailure { error ->
                        Timber.e("Unable to get legal documents: $error")
                        updateState { it.copy(errorState = BlockchainCardErrorState.SnackbarErrorState(error)) }
                    }
            }

            is BlockchainCardIntent.OnSeeSingleLegalDocument -> {
                updateState { it.copy(singleLegalDocumentToSee = intent.legalDocument) }
                navigate(BlockchainCardNavigationEvent.SeeSingleLegalDocument)
            }

            is BlockchainCardIntent.OnSeeLegalDocuments -> {
                modelState.legalDocuments?.let {
                    navigate(BlockchainCardNavigationEvent.SeeLegalDocuments)
                }
            }

            is BlockchainCardIntent.OnLegalDocSeen -> {
                modelState.legalDocuments?.let { documents ->
                    documents.find {
                        it.name == intent.name
                    }?.let { legalDoc ->
                        legalDoc.seen = true
                    }
                }
            }

            is BlockchainCardIntent.OnFinishLegalDocReview -> {
                updateState { it.copy(isLegalDocReviewComplete = true) }
                navigate(BlockchainCardNavigationEvent.FinishLegalDocReview)
            }

            is BlockchainCardIntent.LoadUserFirstAndLastName -> {
                blockchainCardRepository.getUserFirstAndLastName()
                    .doOnSuccess { firstAndLastName ->
                        updateState { it.copy(userFirstAndLastName = firstAndLastName) }
                    }
                    .doOnFailure { error ->
                        Timber.e("Unable to get user first and last name: $error")
                        updateState { it.copy(errorState = BlockchainCardErrorState.SnackbarErrorState(error)) }
                    }
            }

            is BlockchainCardIntent.OnOrderCardFlowComplete -> {
                modelState.currentCard?.let { createdCard ->
                    navigate(BlockchainCardNavigationEvent.FinishOrderCardFlow(createdCard))
                }
            }

            is BlockchainCardIntent.OnChooseProduct -> {
                navigate(BlockchainCardNavigationEvent.ChooseCardProduct)
            }

            else -> {
                Timber.e("Unknown intent: $intent")
            }
        }
    }
}

fun BlockchainCardKycStatus.hasAddressErrorField(): Boolean =
    errorFields?.contains(BlockchainCardKycErrorField.RESIDENTIAL_ADDRESS) == true

fun BlockchainCardKycStatus.shouldUpdateAddress(): Boolean =
    state == BlockchainCardKycState.UNVERIFIED || hasAddressErrorField()

fun BlockchainCardKycStatus.hasSSNErrorField(): Boolean =
    errorFields?.contains(BlockchainCardKycErrorField.SSN) == true

fun BlockchainCardKycStatus.shouldUpdateSSN(): Boolean =
    state == BlockchainCardKycState.UNVERIFIED || hasSSNErrorField()
