package com.blockchain.api.services

import com.blockchain.api.ApiException
import com.blockchain.api.HttpStatus
import com.blockchain.api.bitcoin.BitcoinApi
import com.blockchain.api.bitcoin.data.BalanceResponseDto
import com.blockchain.api.bitcoin.data.MultiAddress
import com.blockchain.api.bitcoin.data.UnspentOutputsDto
import io.reactivex.rxjava3.core.Single
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.HttpException

class NonCustodialBitcoinService internal constructor(
    private val api: BitcoinApi,
    private val apiCode: String
) {
    enum class BalanceFilter(val filterInt: Int) {
        DoNotFilter(0),
        Sent(1), // (result + fee < 0)
        Received(2), // (result + fee > 0)
        Transfer(3), // (result + fee = 0)

        // 4 - is an invalid filter code
        Confirmed(5),

        // 6 - is a NO-OP code, should be same as 0
        Unconfirmed(7)
    }

    /**
     * Returns the address balance summary for each address provided
     *
     * @param coin The code of the coin to be returned, ie "btc" or "bch"
     * @param addressAndXpubListLegacy List of addresses and legacy xpubs.
     * All addresses should be passed through this parameter. In base58, bech32 or xpub format.
     * @param xpubListBech32 Segwit xpub addresses. Do not pass normal addresses here.
     * @param filter the filter for transactions selection, use null to indicate default
     * @return [Call] object which can be executed synchronously or asynchronously to return a
     * response object
     */
    @Deprecated("Use the Rx version")
    fun getBalance(
        coin: String,
        addressAndXpubListLegacy: List<String>,
        xpubListBech32: List<String>,
        filter: BalanceFilter
    ): Call<BalanceResponseDto> {
        val legacyAddrAndXpubs = addressAndXpubListLegacy.joinToString(",")
        val bech32Xpubs = xpubListBech32.joinToString(",")

        return api.getBalance(
            coin,
            legacyAddrAndXpubs,
            bech32Xpubs,
            filter.filterInt,
            apiCode
        )
    }

    /**
     * Returns the address balance summary for each address provided
     *
     * @param coin The code of the coin to be returned, ie "btc" or "bch"
     * @param addressAndXpubListLegacy List of addresses and legacy xpubs.
     * All addresses should be passed through this parameter. In base58, bech32 or xpub format.
     * @param xpubListBech32 Segwit xpub addresses. Do not pass normal addresses here.
     * @param filter the filter for transactions selection, use null to indicate default
     */
    fun getBalanceRx(
        coin: String,
        addressAndXpubListLegacy: List<String>,
        xpubListBech32: List<String>,
        filter: BalanceFilter
    ): Single<BalanceResponseDto> {
        val legacyAddressesAndXpubs = addressAndXpubListLegacy.joinToString(",")
        val bech32Xpubs = xpubListBech32.joinToString("|")

        return api.getBalanceRx(
            coin,
            legacyAddressesAndXpubs,
            bech32Xpubs,
            filter.filterInt,
            apiCode
        )
    }

    /**
     * Returns an aggregated summary on all addresses provided.
     *
     * @param coin The code of the coin to be returned, ie "btc" or "bch"
     * @param addressListLegacy a list of Base58 or xpub addresses
     * @param filter the filter for transactions selection, use null to indicate default
     * @param limit an integer to limit number of transactions to display, use null to indicate default
     * @param offset an integer to set number of transactions to skip when fetch
     * @param onlyShow A context for the results
     * @return [Call] object which can be executed synchronously or asynchronously to return a
     * response object
     */
    fun getMultiAddress(
        coin: String,
        addressListLegacy: List<String>,
        addressListBech32: List<String>,
        onlyShow: String?,
        filter: BalanceFilter,
        limit: Int,
        offset: Int
    ): Call<MultiAddress> {
        val legacyAddresses = addressListLegacy.joinToString("|")
        val bech32Addresses = addressListBech32.joinToString("|")

        return api.getMultiAddress(
            coin = coin,
            activeLegacy = legacyAddresses,
            activeBech32 = bech32Addresses,
            limit = limit,
            offset = offset,
            filter = filter.filterInt,
            onlyShow = onlyShow,
            apiCode = apiCode
        )
    }

    /**
     * Returns list of unspent outputs.
     *
     * @param coin The code of the coin to be returned, ie "btc" or "bch"
     * @param addressList a list of Base58 or xpub addresses
     * @param confirms an integer for minimum confirms of the outputs, use null to indicate default
     * @param limit an integer to limit number of transactions to display, use null to indicate default
     * @return [Call] object which can be executed synchronously or asynchronously to return a
     * response object
     */
    fun getUnspentOutputs(
        coin: String,
        addressListLegacy: List<String>,
        addressListBech32: List<String>,
        confirms: Int?,
        limit: Int?
    ): Single<UnspentOutputsDto> {
        val legacyAddresses = addressListLegacy.joinToString("|")
        val bech32Addresses = addressListBech32.joinToString("|")

        return api.getUnspent(
            coin,
            legacyAddresses,
            bech32Addresses,
            confirms,
            limit,
            apiCode
        ).onErrorResumeNext { e ->
            when {
                e is HttpException && e.code() == HttpStatus.INTERNAL_SERVER_ERROR -> Single.just(UnspentOutputsDto())
                else -> Single.error(ApiException(cause = e, message = ""))
            }
        }
    }

    /**
     * Push a Bitcoin or Bitcoin Cash transaction to network.
     *
     * @param coin The coin type, either BTC or BCH
     * @param hash Transaction hash
     * @return [Call] object which can be executed synchronously or asynchronously to return a
     * response object
     */
    fun pushTx(coin: String, hash: String): Call<ResponseBody> {
        return api.pushTx(coin, hash, apiCode)
    }

    /**
     * Push transaction to network with lock secret.
     *
     * @param coin The coin type, either BTC or BCH
     * @param hash Transaction hash
     * @param lockSecret Secret used server side
     * @return [Call] object which can be executed synchronously or asynchronously to return a
     * response object
     */
    fun pushTxWithSecret(
        coin: String,
        hash: String,
        lockSecret: String
    ): Call<ResponseBody> {
        return api.pushTxWithSecret(coin, hash, lockSecret, apiCode)
    }

    companion object {
        const val BITCOIN = "btc"
        const val BITCOIN_CASH = "bch"
    }
}
