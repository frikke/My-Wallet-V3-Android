package com.blockchain.core.chains.erc20.call

import com.blockchain.api.ethereum.layertwo.L2TransactionResponse
import com.blockchain.api.ethereum.layertwo.TransactionDirection
import com.blockchain.api.services.Erc20Transfer
import com.blockchain.api.services.NonCustodialErc20Service
import com.blockchain.api.services.NonCustodialEthL2Service
import com.blockchain.core.chains.erc20.model.Erc20HistoryEvent
import com.blockchain.core.chains.erc20.model.Erc20HistoryList
import com.blockchain.outcome.fold
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.rxSingle
import piuk.blockchain.androidcore.data.ethereum.EthDataManager

// This doesn't cache anything at this time, since it makes a call for a single
// asset. We can review this, when we look at activity caching in detail

internal class Erc20HistoryCallCache(
    private val ethDataManager: EthDataManager,
    private val erc20Service: NonCustodialErc20Service,
    private val erc20L2Service: NonCustodialEthL2Service,
    private val assetCatalogue: AssetCatalogue
) {
    fun fetch(accountHash: String, asset: AssetInfo): Single<Erc20HistoryList> {
        val contractAddress = asset.l2identifier
        checkNotNull(contractAddress)

        return asset.l1chainTicker?.let { parentChain ->
            if (parentChain == CryptoCurrency.ETHER.networkTicker) {
                fetchErc20FromEthNetwork(accountHash, asset)
            } else {
                rxSingle {
                    erc20L2Service.getTransactionHistory(accountHash, contractAddress, parentChain)
                        .fold(
                            onFailure = { throw it.throwable },
                            onSuccess = { response ->
                                response.history.map { l2TransactionResponse ->
                                    l2TransactionResponse.toHistoryEvent(
                                        asset,
                                        getFeeForL2(l2TransactionResponse, parentChain)
                                    )
                                }
                            }
                        )
                }
            }
        } ?: fetchErc20FromEthNetwork(accountHash, asset)
    }

    private fun fetchErc20FromEthNetwork(accountHash: String, asset: AssetInfo): Single<Erc20HistoryList> {
        val contractAddress = asset.l2identifier
        checkNotNull(contractAddress)
        return erc20Service.getTokenTransfers(accountHash, contractAddress)
            .map { list ->
                list.map { tx ->
                    tx.toHistoryEvent(
                        asset,
                        getFeeFetcher(tx.transactionHash)
                    )
                }
            }
    }

    private fun getFeeFetcher(txHash: String): Single<Money> =
        ethDataManager.getTransaction(txHash)
            .map { transaction ->
                val fee = transaction.gasUsed * transaction.gasPrice
                Money.fromMinor(CryptoCurrency.ETHER, fee)
            }.firstOrError()

    private fun getFeeForL2(l2TransactionResponse: L2TransactionResponse, parentChain: String): Single<Money> =
        ethDataManager.supportedNetworks.map { supportedNetworks ->
            supportedNetworks.firstOrNull { it.networkTicker == parentChain }?.let { ethL2Chain ->
                assetCatalogue.fromNetworkTicker(ethL2Chain.networkTicker)?.let { asset ->
                    val fee = l2TransactionResponse.extraData.gasUsed * l2TransactionResponse.extraData.gasPrice
                    Money.fromMinor(asset, fee)
                } ?: throw IllegalAccessException("Unsupported L2 Network")
            } ?: throw IllegalAccessException("Unsupported L2 Network")
        }

    fun flush(asset: AssetInfo) {
        // Do nothing
    }
}

private fun Erc20Transfer.toHistoryEvent(
    asset: AssetInfo,
    feeFetcher: Single<Money>
): Erc20HistoryEvent =
    Erc20HistoryEvent(
        transactionHash = transactionHash,
        value = CryptoValue.fromMinor(asset, value),
        from = from,
        to = to,
        blockNumber = blockNumber,
        timestamp = timestamp,
        fee = feeFetcher
    )

private fun L2TransactionResponse.toHistoryEvent(
    asset: AssetInfo,
    feeFetcher: Single<Money>
): Erc20HistoryEvent {
    val sourceAddress = movements.firstOrNull { transactionMovement ->
        transactionMovement.type == TransactionDirection.SENT
    }?.address ?: ""
    val targetAddress = movements.firstOrNull { transactionMovement ->
        transactionMovement.type == TransactionDirection.RECEIVED
    }?.address ?: ""
    return Erc20HistoryEvent(
        transactionHash = id,
        value = CryptoValue.fromMinor(asset, movements.first().amount),
        from = sourceAddress,
        to = targetAddress,
        blockNumber = extraData.blockNumber,
        timestamp = timeStamp,
        fee = feeFetcher
    )
}
