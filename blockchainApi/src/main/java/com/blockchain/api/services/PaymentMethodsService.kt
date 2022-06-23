package com.blockchain.api.services

import com.blockchain.api.paymentmethods.PaymentMethodsApi
import com.blockchain.api.paymentmethods.models.AddNewCardBodyRequest
import com.blockchain.api.paymentmethods.models.PaymentMethodResponse
import com.blockchain.api.paymentmethods.models.SimpleBuyConfirmationAttributes
import com.blockchain.api.payments.data.Attributes
import com.blockchain.api.payments.data.BankTransferPaymentBody
import com.blockchain.api.payments.data.CreateLinkBankRequestBody
import com.blockchain.api.payments.data.LinkPlaidAccountBody
import com.blockchain.api.payments.data.OpenBankingTokenBody
import com.blockchain.api.payments.data.RefreshPlaidRequestBody
import com.blockchain.api.payments.data.SettlementBody
import com.blockchain.api.payments.data.UpdateProviderAccountBody
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.preferences.RemoteConfigPrefs
import io.reactivex.rxjava3.core.Single

class PaymentMethodsService internal constructor(
    private val api: PaymentMethodsApi,
    private val remoteConfigPrefs: RemoteConfigPrefs,
    private val environmentConfig: EnvironmentConfig
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

    fun getLinkedBank(authorization: String, id: String) = api.getLinkedBank(
        authorization = authorization,
        id = id,
        localisedError = getLocalisedErrorIfEnabled()
    )

    fun getBanks(authorization: String) = api.getBanks(authorization)

    fun removeBeneficiary(authorization: String, id: String) = api.removeBeneficiary(authorization, id)

    fun removeLinkedBank(authorization: String, id: String) = api.removeLinkedBank(
        authHeader = authorization,
        id = id,
        localisedError = getLocalisedErrorIfEnabled()
    )

    fun linkBank(
        authorization: String,
        fiatCurrency: String,
        supportedPartners: List<String>,
        applicationId: String
    ) = api.linkBank(
        authorization = authorization,
        body = CreateLinkBankRequestBody(
            fiatCurrency,
            Attributes(supportedPartners, applicationId)
        ),
        localisedError = getLocalisedErrorIfEnabled()
    )

    fun updateAccountProviderId(
        authorization: String,
        id: String,
        body: UpdateProviderAccountBody
    ) = api.updateProviderAccount(
        authorization = authorization,
        id = id,
        body = body,
        localisedError = getLocalisedErrorIfEnabled()
    )

    fun linkPLaidAccount(
        authorization: String,
        id: String,
        body: LinkPlaidAccountBody
    ) = api.linkPlaidAccount(
        authorization,
        id,
        body
    )

    fun checkSettlement(
        authorization: String,
        accountId: String,
        body: SettlementBody
    ) = api.checkSettlement(
        authorization,
        accountId,
        body,
        localisedError = getLocalisedErrorIfEnabled()
    )

    fun startBankTransferPayment(
        authorization: String,
        id: String,
        body: BankTransferPaymentBody
    ) = api.startBankTransferPayment(
        authorization = authorization,
        id = id,
        body = body,
        localisedError = getLocalisedErrorIfEnabled()
    )

    fun refreshPlaidAccount(
        authorization: String,
        bankAccountId: String,
        body: RefreshPlaidRequestBody
    ) = api.refreshPlaidAccount(
        authorization = authorization,
        id = bankAccountId,
        body = body,
        localisedError = getLocalisedErrorIfEnabled()
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
        paymentId = paymentId,
        localisedError = getLocalisedErrorIfEnabled()
    )

    fun getGooglePayInfo(
        authorization: String,
        currency: String
    ) = api.getGooglePayInfo(
        authorization = authorization,
        currency = currency
    )

    private fun getLocalisedErrorIfEnabled(): String? =
        if (environmentConfig.isRunningInDebugMode() && remoteConfigPrefs.brokerageErrorsEnabled) {
            remoteConfigPrefs.brokerageErrorsCode
        } else {
            null
        }
}
