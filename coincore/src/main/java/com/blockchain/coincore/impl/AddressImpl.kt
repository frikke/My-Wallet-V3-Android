package com.blockchain.coincore.impl

import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.bch.BchAddress
import com.blockchain.coincore.btc.BtcAddress
import com.blockchain.coincore.custodialonly.DynamicCustodialAddress
import com.blockchain.coincore.erc20.Erc20Address
import com.blockchain.coincore.eth.EthAddress
import com.blockchain.coincore.xlm.XlmAddress
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.isCustodial
import info.blockchain.balance.isLayer2Token
import io.reactivex.rxjava3.core.Completable

internal fun makeExternalAssetAddress(
    asset: AssetInfo,
    address: String,
    label: String = address,
    postTransactions: (TxResult) -> Completable = { Completable.complete() }
): CryptoAddress {
    return when {
        asset == CryptoCurrency.ETHER -> {
            EthAddress(
                address = address,
                label = label,
                onTxCompleted = postTransactions
            )
        }
        asset == CryptoCurrency.BTC -> {
            BtcAddress(
                address = address,
                label = label,
                onTxCompleted = postTransactions
            )
        }
        asset == CryptoCurrency.BCH -> {
            BchAddress(
                address_ = address,
                label = label,
                onTxCompleted = postTransactions
            )
        }
        asset == CryptoCurrency.XLM -> {
            XlmAddress(
                _address = address,
                _label = label,
                onTxCompleted = postTransactions
            )
        }
        asset.isLayer2Token -> {
            Erc20Address(
                asset = asset,
                address = address,
                label = label,
                onTxCompleted = postTransactions
            )
        }
        asset.isCustodial -> {
            DynamicCustodialAddress(
                address = address,
                asset = asset,
                label = label
            )
        }
        else -> throw IllegalArgumentException("External Address not not supported for asset: $asset")
    }
}
