package com.blockchain.core.fees;

import info.blockchain.wallet.api.FeeApi;
import info.blockchain.wallet.api.data.FeeOptions;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;

public class FeeDataManager {

    private final FeeApi feeApi;

    public FeeDataManager(FeeApi feeApi) {
        this.feeApi = feeApi;
    }

    /**
     * Returns a {@link FeeOptions} object which contains both a "regular" and a "priority" fee
     * option, both listed in Satoshis/byte.
     *
     * @return An {@link Observable} wrapping a {@link FeeOptions} object
     */
    public Observable<FeeOptions> getBtcFeeOptions() {
        return feeApi.getBtcFeeOptions()
            .onErrorReturnItem(FeeOptions.Companion.defaultForBtc())
            .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Returns a {@link FeeOptions} object which contains both a "regular" and a "priority" fee
     * option for Ethereum.
     *
     * @return An {@link Observable} wrapping a {@link FeeOptions} object
     */
    public Observable<FeeOptions> getEthFeeOptions() {
        return feeApi.getEthFeeOptions()
                .onErrorReturnItem(FeeOptions.Companion.defaultForEth())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Returns a {@link FeeOptions} object which contains both a "regular" and a "priority" fee
     * option for ERC20 tokens.
     * @param contractAddress the contract address for ERC20
     *
     * @return An {@link Observable} wrapping a {@link FeeOptions} object
     */
    public Observable<FeeOptions> getErc20FeeOptions(String parentChain, String contractAddress) {
        return feeApi.getEvmFeeOptions(parentChain, contractAddress)
            .onErrorReturnItem(FeeOptions.Companion.defaultForErc20())
            .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Returns a {@link FeeOptions} object which contains both a "regular" and a "priority" fee
     * option for EVM L1 tokens.
     * @param network the ticker of the native token on the blockchain
     *
     * @return An {@link Observable} wrapping a {@link FeeOptions} object
     */
    public Observable<FeeOptions> getEvmFeeOptions(String network) {
        return feeApi.getEvmFeeOptions(network, null)
            .onErrorReturnItem(FeeOptions.Companion.defaultForEvm(network))
            .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Returns a {@link FeeOptions} object which contains a "regular" fee
     * option, both listed in Satoshis/byte.
     *
     * @return An {@link Observable} wrapping a {@link FeeOptions} object
     */
    public Observable<FeeOptions> getBchFeeOptions() {
        return feeApi.getBchFeeOptions()
                .onErrorReturnItem(FeeOptions.Companion.defaultForBch());
    }

    /**
     * Returns a {@link FeeOptions} object for XLM fees.
     */
    public Observable<FeeOptions> getXlmFeeOptions() {
        return feeApi.getXlmFeeOptions()
                .onErrorReturnItem(FeeOptions.Companion.defaultForXlm());
    }


}
