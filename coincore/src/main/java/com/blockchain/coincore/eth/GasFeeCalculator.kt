package com.blockchain.coincore.eth

import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import java.math.BigInteger
import org.web3j.utils.Convert

class GasFeeCalculator {
    fun fee(nativeAsset: Currency, gasPriceWei: BigInteger, gasLimit: BigInteger): Money {
        val gasPriceInGwei = Convert.fromWei(
            gasPriceWei.toBigDecimal(),
            Convert.Unit.GWEI
        )

        val feeInWei = Convert.toWei(
            gasPriceInGwei.multiply(gasLimit.toBigDecimal()),
            Convert.Unit.GWEI
        ).toBigInteger()

        return Money.fromMinor(
            nativeAsset,
            feeInWei
        )
    }
}
