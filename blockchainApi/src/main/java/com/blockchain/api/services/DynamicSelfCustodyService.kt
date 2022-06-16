package com.blockchain.api.services

import com.blockchain.api.selfcustody.AccountInfo
import com.blockchain.api.selfcustody.AddSubscriptionRequest
import com.blockchain.api.selfcustody.AddressesRequest
import com.blockchain.api.selfcustody.AuthInfo
import com.blockchain.api.selfcustody.AuthRequest
import com.blockchain.api.selfcustody.CurrencyAddressInfo
import com.blockchain.api.selfcustody.GetSubscriptionsRequest
import com.blockchain.api.selfcustody.PubKeyInfo
import com.blockchain.api.selfcustody.RemoveSubscriptionRequest
import com.blockchain.api.selfcustody.SelfCustodyApi
import com.blockchain.api.selfcustody.SubscriptionInfo

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

    suspend fun getSubscriptions(guidHash: String, sharedKeyHash: String) = selfCustodyApi.getSubscriptions(
        request = GetSubscriptionsRequest(
            auth = AuthInfo(
                guidHash = guidHash,
                sharedKeyHash = sharedKeyHash,
            )
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
}
