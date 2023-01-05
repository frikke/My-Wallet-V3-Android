package com.blockchain.coincore

import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.l1chain
import io.reactivex.rxjava3.core.Single

data class TrendingPair(
    val sourceAccount: CryptoAccount,
    val destinationAccount: CryptoAccount,
    val enabled: Single<Boolean>
)

interface TrendingPairsProvider {
    fun getTrendingPairs(): Single<List<TrendingPair>>
}

internal class SwapTrendingPairsProvider(
    private val coincore: Coincore,
    private val assetCatalogue: AssetCatalogue,
    private val walletModeService: WalletModeService
) : TrendingPairsProvider {

    override fun getTrendingPairs(): Single<List<TrendingPair>> {
        return walletModeService.walletModeSingle.flatMapObservable { walletMode ->
            coincore.activeWalletsInModeRx(walletMode).map { it.accounts }.map {
                it.filterIsInstance<CryptoAccount>()
                    .filterNot { it is InterestAccount }
                    .filter {
                        when (walletMode) {
                            WalletMode.CUSTODIAL_ONLY -> it is TradingAccount
                            WalletMode.NON_CUSTODIAL_ONLY -> it is NonCustodialAccount
                        }
                    }.filter { account ->
                        if (account is NonCustodialAccount)
                            account.isDefault
                        else
                            true
                    }
            }
        }.map { activeAccounts ->
            val assetList = makeRequiredAssetSet()
            activeAccounts.filter { account ->
                account.currency.networkTicker in assetList.map { it.networkTicker }
            }
        }.map { accountList ->
            val accountMap = accountList.associateBy { it.currency }
            buildPairs(accountMap)
        }.onErrorReturn {
            emptyList()
        }.firstOrError()
    }

    private fun makeRequiredAssetSet() =
        DEFAULT_SWAP_PAIRS.map { pair ->
            val l1chain = pair.first.l1chain(assetCatalogue)
            listOf(pair.first, pair.second, l1chain)
        }.flatten()
            .filterNotNull()
            .toSet()

    private fun buildPairs(accountMap: Map<AssetInfo, CryptoAccount>): List<TrendingPair> =
        DEFAULT_SWAP_PAIRS.mapNotNull { pair ->
            val source = accountMap[pair.first]
            val target = accountMap[pair.second]

            if (source != null && target != null) {
                TrendingPair(
                    sourceAccount = source,
                    destinationAccount = target,
                    enabled = source.balanceRx.firstOrError().map {

                        it.total.isPositive
                    }
                )
            } else {
                null
            }
        }

    private val DEFAULT_SWAP_PAIRS = listOfNotNull(
        Pair(CryptoCurrency.BTC, CryptoCurrency.ETHER),
        assetCatalogue.assetInfoFromNetworkTicker("PAX")?.let {
            Pair(CryptoCurrency.BTC, it)
        },
        Pair(CryptoCurrency.BTC, CryptoCurrency.XLM),
        Pair(CryptoCurrency.BTC, CryptoCurrency.BCH),
        assetCatalogue.assetInfoFromNetworkTicker("PAX")?.let {
            Pair(CryptoCurrency.ETHER, it)
        }
    )
}
