package com.blockchain.core.chains.erc20

import com.blockchain.core.chains.erc20.domain.model.Erc20HistoryList
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CoinNetwork
import info.blockchain.wallet.api.data.FeeOptions
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger
import org.web3j.crypto.RawTransaction

interface Erc20DataManager {
    val accountHash: String
    val requireSecondPassword: Boolean

    fun getErc20History(asset: AssetInfo, evmNetwork: CoinNetwork): Single<Erc20HistoryList>

    fun createErc20Transaction(
        asset: AssetInfo,
        to: String,
        amount: BigInteger,
        gasPriceWei: BigInteger,
        gasLimitGwei: BigInteger,
        hotWalletAddress: String
    ): Single<RawTransaction>

    fun createEvmTransaction(
        asset: AssetInfo,
        evmNetwork: String,
        to: String,
        amount: BigInteger,
        gasPriceWei: BigInteger,
        gasLimitGwei: BigInteger,
        hotWalletAddress: String
    ): Single<RawTransaction>

    fun signErc20Transaction(
        rawTransaction: RawTransaction,
        secondPassword: String = "",
        l1Chain: String
    ): Single<ByteArray>

    fun getFeesForEvmTransaction(l1Chain: String): Single<FeeOptions>

    fun pushErc20Transaction(signedTxBytes: ByteArray, l1Chain: String): Single<String>

    fun supportsErc20TxNote(asset: AssetInfo): Boolean
    fun getErc20TxNote(asset: AssetInfo, txHash: String): String?
    fun putErc20TxNote(asset: AssetInfo, txHash: String, note: String): Completable

    fun hasUnconfirmedTransactions(): Single<Boolean>
    fun latestBlockNumber(l1Chain: String? = null): Single<BigInteger>
    fun isContractAddress(address: String, l1Chain: String? = null): Single<Boolean>
}
