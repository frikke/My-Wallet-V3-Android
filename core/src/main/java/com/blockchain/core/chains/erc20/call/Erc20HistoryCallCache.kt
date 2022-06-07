package com.blockchain.core.chains.erc20.call

import com.blockchain.api.ethereum.evm.EvmTransactionResponse
import com.blockchain.api.ethereum.evm.TransactionDirection
import com.blockchain.api.services.Erc20Transfer
import com.blockchain.api.services.NonCustodialErc20Service
import com.blockchain.api.services.NonCustodialEvmService
import com.blockchain.core.chains.erc20.model.Erc20HistoryEvent
import com.blockchain.core.chains.erc20.model.Erc20HistoryList
import com.blockchain.extensions.filterIf
import com.blockchain.outcome.map
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.utils.extensions.rxSingleOutcome

// This doesn't cache anything at this time, since it makes a call for a single
// asset. We can review this, when we look at activity caching in detail

internal class Erc20HistoryCallCache(
    private val ethDataManager: EthDataManager,
    private val erc20Service: NonCustodialErc20Service,
    private val evmService: NonCustodialEvmService,
    private val assetCatalogue: AssetCatalogue
) {
    fun fetch(accountHash: String, asset: AssetInfo, parentChain: String): Single<Erc20HistoryList> {

        return if (parentChain == CryptoCurrency.ETHER.networkTicker) {
            fetchErc20FromEthNetwork(accountHash, asset)
        } else {
            rxSingleOutcome {
                evmService.getTransactionHistory(accountHash, asset.l2identifier, parentChain)
                    .map { response ->
                        // We need to filter the response when we don't provide an l2identifier
                        // (like for L1 aka "native") as the backend returns all transactions for that address.
                        response.history.filterIf(
                            condition = asset.l2identifier == null,
                            predicate = ::isL1EvmTransaction
                        )
                            .map { l2TransactionResponse ->
                                l2TransactionResponse.toHistoryEvent(
                                    asset,
                                    getFeeFromEvmNetwork(l2TransactionResponse, parentChain)
                                )
                            }
                    }
            }
        }
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

    private fun getFeeFromEvmNetwork(
        evmTransactionResponse: EvmTransactionResponse,
        parentChain: String
    ): Single<Money> =
        ethDataManager.supportedNetworks.map { supportedNetworks ->
            supportedNetworks.firstOrNull { it.networkTicker == parentChain }?.let { evmNetwork ->
                assetCatalogue.fromNetworkTicker(evmNetwork.networkTicker)?.let { asset ->
                    val fee = evmTransactionResponse.extraData.gasUsed * evmTransactionResponse.extraData.gasPrice
                    Money.fromMinor(asset, fee)
                } ?: throw IllegalAccessException("Unsupported L2 Network")
            } ?: throw IllegalAccessException("Unsupported L2 Network")
        }

    private fun isL1EvmTransaction(evmTransactionResponse: EvmTransactionResponse) =
        evmTransactionResponse.movements.find { txMovement ->
            txMovement.contractAddress == NonCustodialEvmService.NATIVE_IDENTIFIER
        } != null

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

private fun EvmTransactionResponse.toHistoryEvent(
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
