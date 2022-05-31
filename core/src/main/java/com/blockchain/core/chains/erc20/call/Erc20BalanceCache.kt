package com.blockchain.core.chains.erc20.call

import com.blockchain.api.services.Erc20TokenBalance
import com.blockchain.api.services.NonCustodialErc20Service
import com.blockchain.api.services.NonCustodialEvmService
import com.blockchain.core.chains.erc20.model.Erc20Balance
import com.blockchain.core.common.caching.ParameteredSingleTimedCacheRequest
import com.blockchain.core.common.caching.TimedCacheRequest
import com.blockchain.outcome.fold
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.rx3.rxSingle

internal typealias Erc20BalanceMap = Map<AssetInfo, Erc20Balance>

internal class Erc20BalanceCallCache(
    private val erc20Service: NonCustodialErc20Service,
    private val evmService: NonCustodialEvmService,
    private val assetCatalogue: AssetCatalogue
) {
    private val cacheRequest: TimedCacheRequest<Erc20BalanceMap> by lazy {
        TimedCacheRequest(
            cacheLifetimeSeconds = BALANCE_CACHE_TTL_SECONDS,
            refreshFn = ::refreshCache
        )
    }

    private val l2CacheRequest: ParameteredSingleTimedCacheRequest<String, Erc20BalanceMap> by lazy {
        ParameteredSingleTimedCacheRequest(
            cacheLifetimeSeconds = BALANCE_CACHE_TTL_SECONDS,
            refreshFn = ::refreshL2Cache
        )
    }

    private val account = AtomicReference<String>()

    private fun refreshCache(): Single<Erc20BalanceMap> {
        return erc20Service.getTokenBalances(account.get())
            .map { balanceList ->
                balanceList.mapNotNull { balance ->
                    assetCatalogue.assetFromL1ChainByContractAddress(
                        CryptoCurrency.ETHER.networkTicker,
                        balance.contractAddress
                    )?.let { info ->
                        info to balance.mapBalance(info)
                    }
                }.toMap()
            }
    }

    private fun refreshL2Cache(network: String): Single<Erc20BalanceMap> {
        return rxSingle {
            evmService.getBalances(account.get(), network)
                .fold(
                    onFailure = { throw it.throwable },
                    onSuccess = { addressList ->
                        // The backend is now accepting an array of addresses/pubkeys for balance query.
                        // We only pass a single address of an account so in theory we should only get
                        // its balances in the response.
                        addressList.addresses.firstOrNull {
                            it.address == account.get()
                        }?.balances?.mapNotNull { balance ->
                            // For the native token of the L2 network the backend returns "native" in the contract
                            // address field. Use the currency name (ticker) for lookup.
                            val asset = if (balance.contractAddress == NonCustodialEvmService.NATIVE_IDENTIFIER) {
                                assetCatalogue.assetInfoFromNetworkTicker(balance.name)
                            } else {
                                assetCatalogue.assetFromL1ChainByContractAddress(
                                    network,
                                    balance.contractAddress
                                )
                            }
                            asset?.let {
                                asset to Erc20Balance(
                                    balance = CryptoValue.fromMinor(asset, balance.amount),
                                    hasTransactions = balance.amount > BigInteger.ZERO
                                )
                            }
                        }?.toMap() ?: emptyMap()
                    }
                )
        }
    }

    fun getBalances(accountHash: String, parentChainTicker: String = ""): Single<Erc20BalanceMap> {
        val oldAccountHash = account.getAndSet(accountHash)
        if (oldAccountHash != accountHash && parentChainTicker.isEmpty()) {
            cacheRequest.invalidate()
        } else if (oldAccountHash != accountHash) {
            l2CacheRequest.invalidate(parentChainTicker)
        }
        return if (parentChainTicker.isEmpty() || parentChainTicker == CryptoCurrency.ETHER.networkTicker) {
            cacheRequest.getCachedSingle()
        } else {
            l2CacheRequest.getCachedSingle(parentChainTicker)
        }
    }

    fun flush(asset: AssetInfo) {
        cacheRequest.invalidate()
        l2CacheRequest.invalidate(asset.networkTicker)
    }

    companion object {
        private const val BALANCE_CACHE_TTL_SECONDS = 10L
    }
}

private fun Erc20TokenBalance?.mapBalance(asset: AssetInfo): Erc20Balance =
    this?.let {
        Erc20Balance(
            balance = CryptoValue.fromMinor(asset, it.balance),
            hasTransactions = it.transferCount > 0
        )
    } ?: Erc20Balance(
        balance = CryptoValue.zero(asset),
        hasTransactions = false
    )
