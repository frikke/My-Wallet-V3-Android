package com.blockchain.unifiedcryptowallet.domain.balances

import info.blockchain.balance.Currency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import java.lang.IllegalArgumentException

interface UnifiedBalancesService {
    suspend fun balances(): List<NetworkBalance>

    suspend fun balanceForAccount(
        name: String,
        index: Int,
        currency: Currency
    ): NetworkBalance
}

interface NetworkAccountsService {
    suspend fun allWallets(): List<NetworkNonCustodialAccount>
}

data class NetworkBalance(
    val currency: Currency,
    val balance: Money,
    val unconfirmedBalance: Money,
    val index: Int,
    val name: String,
    val exchangeRate: ExchangeRate
)

interface NetworkNonCustodialAccount {
    val label: String
    val currency: Currency
    val index: Int

    /**
     * The descriptor field will need some explanation. Over time some currencies change the
     * way that keys are derived as well as how such keys are used. Most notably, Bitcoin uses
     * a new derivation for SegWit, and the addresses are derived differently, etc etc. A big part of the
     * refactor to add SegWit to our wallet was to add this model, that each account can have multiple xpubs,
     * and this descriptor field is that same abstraction. One needs to continue to monitor all addresses,
     * as previous old addresses may receive funds in the future.
     */
    val descriptor: Int
        get() = DEFAULT_ADDRESS_DESCRIPTOR

    val style: String
        get() = SINGLE_PUB_KEY_STYLE

    suspend fun publicKey(): String

    companion object {
        const val SINGLE_PUB_KEY_STYLE = "SINGLE"
        const val EXTENDED_PUB_KEY_STYLE = "EXTENDED"
        const val DEFAULT_SINGLE_ACCOUNT_INDEX = 0
        const val DEFAULT_ADDRESS_DESCRIPTOR = 0
        const val MULTIPLE_ADDRESSES_DESCRIPTOR = 1
    }
}

class UnifiedBalanceNotFoundException(currency: Currency, index: Int, name: String) :
    IllegalArgumentException("No balance found for ${currency.networkTicker} for account $name at index $index")
