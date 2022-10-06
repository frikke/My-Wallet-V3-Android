package com.blockchain.api.services

import com.blockchain.api.paymentmethods.PaymentMethodsApi
import com.blockchain.api.paymentmethods.models.AddNewCardBodyRequest
import com.blockchain.api.paymentmethods.models.AliasInfoRequestBody
import com.blockchain.api.paymentmethods.models.LinkWithAliasRequestBody
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
        currency: String,
        tier: Int?,
        eligibleOnly: Boolean
    ): Single<List<PaymentMethodResponse>> = api.getAvailablePaymentMethodsTypes(
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
    fun getCards(cardProvidersSupported: Boolean) =
        api.getCards(cardProvidersSupported)

    fun addNewCard(
        addNewCardBodyRequest: AddNewCardBodyRequest
    ) = api.addNewCard(addNewCardBodyRequest, getLocalisedErrorIfEnabled())

    fun activateCard(
        cardId: String,
        attributes: SimpleBuyConfirmationAttributes
    ) = api.activateCard(cardId, attributes)

    fun getCardDetails(
        cardId: String
    ) = api.getCardDetails(cardId)

    fun deleteCard(cardId: String) = api.deleteCard(cardId)

    fun getLinkedBank(id: String) = api.getLinkedBank(
        id = id,
        localisedError = getLocalisedErrorIfEnabled()
    )

    fun getBanks() = api.getBanks()

    fun removeBeneficiary(id: String) = api.removeBeneficiary(id)

    fun removeLinkedBank(id: String) = api.removeLinkedBank(
        id = id,
        localisedError = getLocalisedErrorIfEnabled()
    )

    fun linkBank(
        fiatCurrency: String,
        supportedPartners: List<String>,
        applicationId: String
    ) = api.linkBank(
        body = CreateLinkBankRequestBody(
            fiatCurrency,
            Attributes(supportedPartners, applicationId)
        ),
        localisedError = getLocalisedErrorIfEnabled()
    )

    fun updateAccountProviderId(
        id: String,
        body: UpdateProviderAccountBody
    ) = api.updateProviderAccount(
        id = id,
        body = body,
        localisedError = getLocalisedErrorIfEnabled()
    )

    fun linkPLaidAccount(
        id: String,
        body: LinkPlaidAccountBody
    ) = api.linkPlaidAccount(
        id,
        body
    )

    fun checkSettlement(
        accountId: String,
        body: SettlementBody
    ) = api.checkSettlement(
        accountId,
        body,
        localisedError = getLocalisedErrorIfEnabled()
    )

    fun startBankTransferPayment(
        id: String,
        body: BankTransferPaymentBody
    ) = api.startBankTransferPayment(
        id = id,
        body = body,
        localisedError = getLocalisedErrorIfEnabled()
    )

    fun refreshPlaidAccount(
        bankAccountId: String,
        body: RefreshPlaidRequestBody
    ) = api.refreshPlaidAccount(
        id = bankAccountId,
        body = body,
        localisedError = getLocalisedErrorIfEnabled()
    )

    fun updateOpenBankingToken(
        url: String,
        body: OpenBankingTokenBody
    ) = api.updateOpenBankingToken(
        url = url,
        body = body
    )

    fun getBankTransferCharge(
        paymentId: String
    ) = api.getBankTransferCharge(
        paymentId = paymentId,
        localisedError = getLocalisedErrorIfEnabled()
    )

    fun getGooglePayInfo(
        currency: String
    ) = api.getGooglePayInfo(
        currency = currency
    )

    suspend fun getBeneficiaryInfo(
        currency: String,
        address: String
    ) = api.getBeneficiaryInfo(
        body = AliasInfoRequestBody(
            currency = currency,
            address = address
        ),
        localisedError = getLocalisedErrorIfEnabled()
    )

    suspend fun activateBeneficiary(
        beneficiaryId: String
    ) = api.activateBeneficiary(
        body = LinkWithAliasRequestBody(beneficiaryId)
    )

    suspend fun checkCardRejectionState(
        binNumber: String
    ) = api.checkNewCardRejectionState(
        binNumber = binNumber
    )

    private fun getLocalisedErrorIfEnabled(): String? =
        if (environmentConfig.isRunningInDebugMode() && remoteConfigPrefs.brokerageErrorsEnabled) {
            remoteConfigPrefs.brokerageErrorsCode
        } else {
            null
        }
}
