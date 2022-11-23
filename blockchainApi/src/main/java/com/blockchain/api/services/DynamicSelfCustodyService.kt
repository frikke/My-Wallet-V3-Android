package com.blockchain.api.services

import com.blockchain.api.selfcustody.AccountInfo
import com.blockchain.api.selfcustody.AddSubscriptionRequest
import com.blockchain.api.selfcustody.AddressesRequest
import com.blockchain.api.selfcustody.AuthInfo
import com.blockchain.api.selfcustody.AuthRequest
import com.blockchain.api.selfcustody.BalancesRequest
import com.blockchain.api.selfcustody.BuildTxRequest
import com.blockchain.api.selfcustody.CommonResponse
import com.blockchain.api.selfcustody.CurrencyAddressInfo
import com.blockchain.api.selfcustody.CurrencyInfo
import com.blockchain.api.selfcustody.ExtraData
import com.blockchain.api.selfcustody.GetSubscriptionsRequest
import com.blockchain.api.selfcustody.GetSubscriptionsResponse
import com.blockchain.api.selfcustody.PubKeyInfo
import com.blockchain.api.selfcustody.PushTxRequest
import com.blockchain.api.selfcustody.RemoveSubscriptionRequest
import com.blockchain.api.selfcustody.SelfCustodyApi
import com.blockchain.api.selfcustody.Signature
import com.blockchain.api.selfcustody.SubscriptionInfo
import com.blockchain.api.selfcustody.TransactionHistoryRequest
import com.blockchain.api.selfcustody.activity.ActivityDetailsRequest
import com.blockchain.api.selfcustody.activity.ActivityPubKeyInfo
import com.blockchain.api.selfcustody.activity.LocalisationParams
import com.blockchain.domain.wallet.PubKeyStyle
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.flatMap
import kotlinx.serialization.json.JsonObject

class DynamicSelfCustodyService(
    private val selfCustodyApi: SelfCustodyApi,
    private val credentials: SelfCustodyServiceAuthCredentials
) {
    private suspend fun authenticate() = selfCustodyApi.authenticate(
        request = AuthRequest(
            guid = credentials.guid,
            sharedKey = credentials.hashedSharedKey
        )
    )

    private val authInfo: AuthInfo
        get() = AuthInfo(
            guidHash = credentials.hashedGuid,
            sharedKeyHash = credentials.hashedSharedKey,
        )

    suspend fun subscribe(
        currency: String,
        accountName: String,
        addresses: List<String>
    ) = authIfFails {
        selfCustodyApi.subscribe(
            request = AddSubscriptionRequest(
                auth = authInfo,
                data = listOf(
                    SubscriptionInfo(
                        currency = currency,
                        accountInfo = AccountInfo(
                            index = 0,
                            name = accountName
                        ),
                        pubKeys = addresses.map { address -> PubKeyInfo(address, PubKeyStyle.SINGLE) }
                    )
                )
            )
        )
    }

    suspend fun subscribe(
        data: List<SubscriptionInfo>
    ): Outcome<Exception, CommonResponse> {
        return authIfFails {
            selfCustodyApi.subscribe(
                request = AddSubscriptionRequest(
                    auth = authInfo,
                    data = data
                )
            )
        }
    }

    suspend fun unsubscribe(currency: String) =
        authIfFails {
            selfCustodyApi.unsubscribe(
                request = RemoveSubscriptionRequest(
                    auth = authInfo,
                    currency = currency
                )
            )
        }

    suspend fun getSubscriptions(): Outcome<Exception, GetSubscriptionsResponse> =
        authIfFails {
            selfCustodyApi.getSubscriptions(
                request = GetSubscriptionsRequest(
                    auth = authInfo
                )
            )
        }

    suspend fun getBalances(
        currencies: List<String> = emptyList(),
        fiatCurrency: String
    ) = authIfFails {
        selfCustodyApi.getBalances(
            request = BalancesRequest(
                auth = authInfo,
                currencies = currencies.map { ticker -> CurrencyInfo(ticker) }.takeIf { it.isNotEmpty() },
                fiatCurrency = fiatCurrency
            )
        )
    }

    suspend fun getAddresses(currencies: List<String>) =
        selfCustodyApi.getAddresses(
            request = AddressesRequest(
                auth = authInfo,
                currencies = currencies.map {
                    CurrencyAddressInfo(
                        ticker = it
                    )
                }
            )
        )

    suspend fun getTransactionHistory(
        currency: String,
        contractAddress: String?
    ) = selfCustodyApi.getTransactionHistory(
        request = TransactionHistoryRequest(
            auth = authInfo,
            currency = currency,
            contractAddress = contractAddress
        )
    )

    suspend fun getActivityDetails(
        txId: String,
        network: String,
        pubKey: String,
        pubKeyStyle: PubKeyStyle,
        pubKeyDescriptor: String,
        timeZone: String,
        locales: String,
        fiatCurrency: String
    ) = selfCustodyApi.getActivityDetails(
        request = ActivityDetailsRequest(
            auth = authInfo,
            txId = txId,
            network = network,
            pubKey = ActivityPubKeyInfo(
                pubKey = pubKey,
                style = pubKeyStyle,
                descriptor = pubKeyDescriptor
            ),
            params = LocalisationParams(
                timeZone = timeZone,
                locales = locales,
                fiatCurrency = fiatCurrency
            )
        )
    )

    suspend fun buildTransaction(
        currency: String,
        accountIndex: Int = 0,
        type: String,
        transactionTarget: String,
        amount: String,
        fee: String,
        memo: String = "",
        feeCurrency: String = currency
    ) = selfCustodyApi.buildTransaction(
        request = BuildTxRequest(
            auth = authInfo,
            currency = currency,
            accountIndex = accountIndex,
            type = type,
            destination = transactionTarget,
            amount = amount,
            fee = fee,
            extraData = ExtraData(
                memo = memo,
                feeCurrency = feeCurrency,
            ),
            maxVerificationVersion = null
        )
    )

    suspend fun pushTransaction(
        currency: String,
        rawTx: JsonObject,
        signatures: List<Signature>
    ) =
        selfCustodyApi.pushTransaction(
            request = PushTxRequest(
                auth = authInfo,
                currency = currency,
                rawTx = rawTx,
                signatures = signatures
            )
        )

    private suspend fun <T> authIfFails(
        f: suspend () -> Outcome<Exception, T>,

    ): Outcome<Exception, T> {
        return when (val result = f()) {
            is Outcome.Success -> result
            else -> authenticate().flatMap {
                f()
            }
        }
    }
}

interface SelfCustodyServiceAuthCredentials {
    val guid: String
    val hashedSharedKey: String
    val hashedGuid: String
}
