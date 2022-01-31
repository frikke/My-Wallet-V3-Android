package com.blockchain.coincore.eth

import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxResult
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Completable
import org.bouncycastle.util.encoders.Hex

class EthereumSignMessageTarget(
    val dAppAddress: String,
    private val dAppName: String,
    val dAppLogoUrl: String,
    val currency: Currency = CryptoCurrency.ETHER,
    val message: EthSignMessage,
    override val onTxCompleted: (TxResult) -> Completable
) : WalletConnectTarget {
    override val label: String
        get() = dAppName
}

interface WalletConnectTarget : TransactionTarget

data class EthSignMessage(
    val raw: List<String>,
    val type: SignType
) {

    enum class SignType {
        MESSAGE, PERSONAL_MESSAGE, TYPED_MESSAGE
    }

    val readableData: String
        get() = when (type) {
            SignType.MESSAGE,
            SignType.PERSONAL_MESSAGE -> String(Hex.decode(data.removePrefix("0x")))
            else -> data
        }

    /**
     * Raw parameters will always be the message and the addess. Depending on the WCSignType,
     * those parameters can be swapped as description below:
     *
     *  - MESSAGE: `[address, data ]`
     *  - TYPED_MESSAGE: `[address, data]`
     *  - PERSONAL_MESSAGE: `[data, address]`
     *
     *  reference: https://docs.walletconnect.org/json-rpc/ethereum#eth_signtypeddata
     */
    val data
        get() = when (type) {
            SignType.MESSAGE -> raw[1]
            SignType.TYPED_MESSAGE -> raw[1]
            SignType.PERSONAL_MESSAGE -> raw[0]
        }

    val address
        get() = when (type) {
            SignType.MESSAGE -> raw[0]
            SignType.TYPED_MESSAGE -> raw[0]
            SignType.PERSONAL_MESSAGE -> raw[1]
        }
}
