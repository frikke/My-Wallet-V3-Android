package com.blockchain.core.fees

import com.blockchain.rx.MainScheduler
import info.blockchain.wallet.api.FeeApi
import info.blockchain.wallet.api.data.FeeOptions
import info.blockchain.wallet.api.data.FeeOptions.Companion.defaultForBch
import info.blockchain.wallet.api.data.FeeOptions.Companion.defaultForBtc
import info.blockchain.wallet.api.data.FeeOptions.Companion.defaultForErc20
import info.blockchain.wallet.api.data.FeeOptions.Companion.defaultForEth
import info.blockchain.wallet.api.data.FeeOptions.Companion.defaultForEvm
import info.blockchain.wallet.api.data.FeeOptions.Companion.defaultForXlm
import io.reactivex.rxjava3.core.Observable

class FeeDataManager(private val feeApi: FeeApi) {
    /**
     * Returns a [FeeOptions] object which contains both a "regular" and a "priority" fee
     * option, both listed in Satoshis/byte.
     *
     * @return An [Observable] wrapping a [FeeOptions] object
     */
    val btcFeeOptions: Observable<FeeOptions>
        get() = feeApi.btcFeeOptions
            .onErrorReturnItem(defaultForBtc())
            .observeOn(MainScheduler.main())

    /**
     * Returns a [FeeOptions] object which contains both a "regular" and a "priority" fee
     * option for Ethereum.
     *
     * @return An [Observable] wrapping a [FeeOptions] object
     */
    val ethFeeOptions: Observable<FeeOptions>
        get() = feeApi.ethFeeOptions
            .onErrorReturnItem(defaultForEth())
            .observeOn(MainScheduler.main())

    /**
     * Returns a [FeeOptions] object which contains both a "regular" and a "priority" fee
     * option for ERC20 tokens.
     * @param contractAddress the contract address for ERC20
     *
     * @return An [Observable] wrapping a [FeeOptions] object
     */
    fun getErc20FeeOptions(parentChain: String, contractAddress: String?): Observable<FeeOptions> {
        return feeApi.getEvmFeeOptions(parentChain, contractAddress)
            .onErrorReturnItem(defaultForErc20())
            .observeOn(MainScheduler.main())
    }

    /**
     * Returns a [FeeOptions] object which contains both a "regular" and a "priority" fee
     * option for EVM L1 tokens.
     * @param network the ticker of the native token on the blockchain
     *
     * @return An [Observable] wrapping a [FeeOptions] object
     */
    fun getEvmFeeOptions(network: String): Observable<FeeOptions> {
        return feeApi.getEvmFeeOptions(network, null)
            .onErrorReturnItem(defaultForEvm(network))
            .observeOn(MainScheduler.main())
    }

    /**
     * Returns a [FeeOptions] object which contains a "regular" fee
     * option, both listed in Satoshis/byte.
     *
     * @return An [Observable] wrapping a [FeeOptions] object
     */
    val bchFeeOptions: Observable<FeeOptions>
        get() = feeApi.bchFeeOptions
            .onErrorReturnItem(defaultForBch())

    /**
     * Returns a [FeeOptions] object for XLM fees.
     */
    val xlmFeeOptions: Observable<FeeOptions>
        get() = feeApi.xlmFeeOptions
            .onErrorReturnItem(defaultForXlm())
}
