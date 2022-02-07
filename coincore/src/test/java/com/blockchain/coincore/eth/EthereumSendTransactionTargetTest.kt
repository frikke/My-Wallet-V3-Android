package com.blockchain.coincore.eth

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import org.junit.Assert
import org.junit.Test

class EthereumSendTransactionTargetTest {

    private val subject = EthereumSendTransactionTarget(
        dAppAddress = "dAppAddress",
        dAppName = "dAppName",
        dAppLogoURL = "dAppName",
        method = EthereumSendTransactionTarget.Method.SIGN,
        onTxCompleted = { Completable.complete() },
        onTxCancelled = { Completable.complete() },
        transaction =
        EthereumJsonRpcTransaction(
            data = "0x5ae401dc0000000000000000000000000000000000000000000000000000000061fd5baa00000000000000000" +
                "00000000000000000000000000000000000000000000040000000000000000000000000000000000000000" +
                "00000000000000000000000010000000000000000000000000000000000000000000000000000000000" +
                "00002000000000000000000000000000000000000000000000000000000000000000e404e45aaf0000000000" +
                "00000000000000c02aaa39b223fe8d0a0e5c4f27ead9083c756cc20000000000000000000000007fc66500c84a76ad7e9c93" +
                "437bfc5ac33e2ddae90000000000000000000000000000000000000000000000000000000000000bb80000000000000000" +
                "00000000b614ac2df0485ae90762d7227373b1ba737e0f8c000000000000000000000000000000000000000000000" +
                "00000038d7ea4c680000000000000000000000000000000000000000000000000000032a7c4fe3938a100000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "000000000",
            from = "0xb614ac2df0485ae90762d7227373b1ba737e0f8c",
            gas = "0x61b0a",
            to = "0x61b0a",
            value = "0x38d7ea4c68000",
            nonce = "0x15",
            gasPrice = "0xED05265A"
        )
    )

    @Test
    fun `JsonRpc transaction values should be decoded normally`() {
        println(subject.gasLimit)
        Assert.assertEquals(400138.toBigInteger(), subject.gasLimit)
        Assert.assertEquals(3976537690.toBigInteger(), subject.gasPrice)
        Assert.assertEquals(Money.fromMinor(CryptoCurrency.ETHER, 1000000000000000.toBigInteger()), subject.amount)
        Assert.assertEquals(21.toBigInteger(), subject.nonce)
    }
}
