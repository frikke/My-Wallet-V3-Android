package com.blockchain.api.services

import com.blockchain.api.paymentmethods.PaymentMethodsApi
import com.blockchain.api.paymentmethods.models.AddNewCardBodyRequest
import com.blockchain.api.paymentmethods.models.BankTransferPaymentBody
import com.blockchain.api.paymentmethods.models.CreateLinkBankRequestBody
import com.blockchain.api.paymentmethods.models.OpenBankingTokenBody
import com.blockchain.api.paymentmethods.models.PaymentMethodResponse
import com.blockchain.api.paymentmethods.models.SimpleBuyConfirmationAttributes
import com.blockchain.api.paymentmethods.models.UpdateProviderAccountBody
import io.reactivex.rxjava3.core.Single

class PaymentMethodsService internal constructor(
    private val api: PaymentMethodsApi
) {

    /**
     * Returns a list of the available payment methods. [shouldFetchSddLimits] if true, then the responded
     * payment methods will contain the limits for SDD user. We use this argument only if we want to get back
     * these limits. To achieve back-words compatibility with the other platforms we had to use
     * a flag called visible (instead of not returning the corresponding payment methods at all.
     * Any payment method with the flag visible=false should be discarded.
     */
    fun getAvailablePaymentMethodsTypes(
        authorization: String,
        currency: String,
        tier: Int?,
        eligibleOnly: Boolean
    ): Single<List<PaymentMethodResponse>> = api.getAvailablePaymentMethodsTypes(
        authorization,
        currency,
        tier,
        eligibleOnly
    ).map {
        it.filter { paymentMethod -> paymentMethod.visible }
    }

    /**
     * cardProvidersSupported is signalling to the backend if they can list
     * cards created by the new providers for payment. The purpose is to avoid listing
     * cards which were tokenised with the new providers on other platforms if
     * the feature flag is off.
     */
    fun getCards(authorization: String, cardProvidersSupported: Boolean) =
        api.getCards(authorization, cardProvidersSupported)

    fun addNewCard(
        authorization: String,
        addNewCardBodyRequest: AddNewCardBodyRequest
    ) = api.addNewCard(authorization, addNewCardBodyRequest)

    fun activateCard(
        authorization: String,
        cardId: String,
        attributes: SimpleBuyConfirmationAttributes
    ) = api.activateCard(authorization, cardId, attributes)

    fun getCardDetails(
        authorization: String,
        cardId: String
    ) = api.getCardDetails(authorization, cardId)

    fun deleteCard(authorization: String, cardId: String) = api.deleteCard(authorization, cardId)

    fun getLinkedBank(authorization: String, id: String) = api.getLinkedBank(authorization, id)

    fun getBanks(authorization: String) = api.getBanks(authorization)

    fun removeBeneficiary(authorization: String, id: String) = api.removeBeneficiary(authorization, id)

    fun removeLinkedBank(authorization: String, id: String) = api.removeLinkedBank(authorization, id)

    fun linkBank(
        authorization: String,
        fiatCurrency: String
    ) = api.linkBank(authorization, CreateLinkBankRequestBody(fiatCurrency))

    fun updateAccountProviderId(
        authorization: String,
        id: String,
        body: UpdateProviderAccountBody
    ) = api.updateProviderAccount(
        authorization,
        id,
        body
    )

    fun startBankTransferPayment(
        authorization: String,
        id: String,
        body: BankTransferPaymentBody
    ) = api.startBankTransferPayment(
        authorization = authorization,
        id = id,
        body = body
    )

    fun updateOpenBankingToken(
        url: String,
        authorization: String,
        body: OpenBankingTokenBody
    ) = api.updateOpenBankingToken(
        url = url,
        authorization = authorization,
        body = body
    )

    fun getBankTransferCharge(
        authorization: String,
        paymentId: String
    ) = api.getBankTransferCharge(
        authorization = authorization,
        paymentId = paymentId
    )
}
