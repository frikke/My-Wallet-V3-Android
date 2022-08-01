package com.blockchain.api.services

import com.blockchain.api.selfcustody.AccountInfo
import com.blockchain.api.selfcustody.AddSubscriptionRequest
import com.blockchain.api.selfcustody.AddressesRequest
import com.blockchain.api.selfcustody.AuthInfo
import com.blockchain.api.selfcustody.AuthRequest
import com.blockchain.api.selfcustody.BalancesRequest
import com.blockchain.api.selfcustody.BuildTxRequest
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
import com.blockchain.outcome.Outcome
import kotlinx.serialization.json.JsonObject

class DynamicSelfCustodyService(
    private val selfCustodyApi: SelfCustodyApi
) {
    suspend fun authenticate(guid: String, sharedKey: String) = selfCustodyApi.authenticate(
        request = AuthRequest(
            guid = guid,
            sharedKey = sharedKey
        )
    )

    suspend fun subscribe(
        guidHash: String,
        sharedKeyHash: String,
        currency: String,
        accountName: String,
        addresses: List<String>
    ) = selfCustodyApi.subscribe(
        request = AddSubscriptionRequest(
            auth = AuthInfo(
                guidHash = guidHash,
                sharedKeyHash = sharedKeyHash,
            ),
            data = listOf(
                SubscriptionInfo(
                    currency = currency,
                    accountInfo = AccountInfo(
                        index = 0,
                        name = accountName
                    ),
                    pubkeys = addresses.map { address -> PubKeyInfo(address) }
                )
            )
        )
    )

    suspend fun unsubscribe(guidHash: String, sharedKeyHash: String, currency: String) =
        selfCustodyApi.unsubscribe(
            request = RemoveSubscriptionRequest(
                auth = AuthInfo(
                    guidHash = guidHash,
                    sharedKeyHash = sharedKeyHash,
                ),
                currency = currency
            )
        )

    suspend fun getSubscriptions(
        guidHash: String,
        sharedKeyHash: String
    ): Outcome<Exception, GetSubscriptionsResponse> =
        selfCustodyApi.getSubscriptions(
            request = GetSubscriptionsRequest(
                auth = AuthInfo(
                    guidHash = guidHash,
                    sharedKeyHash = sharedKeyHash,
                )
            )
        )

    suspend fun getBalances(guidHash: String, sharedKeyHash: String, currencies: List<String>, fiatCurrency: String) =
        selfCustodyApi.getBalances(
            request = BalancesRequest(
                auth = AuthInfo(
                    guidHash = guidHash,
                    sharedKeyHash = sharedKeyHash
                ),
                currencies = currencies.map { ticker -> CurrencyInfo(ticker) },
                fiatCurrency = fiatCurrency
            )
        )

    suspend fun getAddresses(guidHash: String, sharedKeyHash: String, currencies: List<String>) =
        selfCustodyApi.getAddresses(
            request = AddressesRequest(
                auth = AuthInfo(
                    guidHash = guidHash,
                    sharedKeyHash = sharedKeyHash
                ),
                currencies = currencies.map {
                    CurrencyAddressInfo(
                        ticker = it
                    )
                }
            )
        )

    suspend fun getTransactionHistory(
        guidHash: String,
        sharedKeyHash: String,
        currency: String,
        contractAddress: String?
    ) = selfCustodyApi.getTransactionHistory(
        request = TransactionHistoryRequest(
            auth = AuthInfo(
                guidHash = guidHash,
                sharedKeyHash = sharedKeyHash
            ),
            currency = currency,
            contractAddress = contractAddress
        )
    )

    suspend fun buildTransaction(
        guidHash: String,
        sharedKeyHash: String,
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
            auth = AuthInfo(
                guidHash = guidHash,
                sharedKeyHash = sharedKeyHash
            ),
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
        guidHash: String,
        sharedKeyHash: String,
        currency: String,
        rawTx: JsonObject,
        signatures: List<Signature>
    ) =
        selfCustodyApi.pushTransaction(
            request = PushTxRequest(
                auth = AuthInfo(
                    guidHash = guidHash,
                    sharedKeyHash = sharedKeyHash
                ),
                currency = currency,
                rawTx = rawTx,
                signatures = signatures
            )
        )
}
