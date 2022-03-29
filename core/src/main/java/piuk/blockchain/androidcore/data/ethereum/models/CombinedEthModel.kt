package piuk.blockchain.androidcore.data.ethereum.models

import info.blockchain.wallet.ethereum.data.EthAddressResponse
import info.blockchain.wallet.ethereum.data.EthTransaction
import java.math.BigInteger

/**
 * A model that merges the transactions and balances of multiple ETH responses into a single object.
 */
class CombinedEthModel(private val ethAddressResponseMap: Map<String, EthAddressResponse>) {

    fun getTotalBalance(): BigInteger {
        val values = ethAddressResponseMap.values
        return values.sumOf { ethAddressResponse ->
            ethAddressResponse.getBalance() ?: BigInteger.ZERO
        }
    }

    fun getTransactions(): List<EthTransaction> {
        val values = ethAddressResponseMap.values
        val transactions = mutableListOf<EthTransaction>()
        for (it in values) {
            transactions.addAll(it.transactions)
        }
        return transactions.toList()
    }

    /**
     * Main eth account
     */
    private fun getAddressResponse(): EthAddressResponse =
        ethAddressResponseMap.values.first()

    fun getNonce(): BigInteger {
        // Force-unwrap should not be used, but if we don't get a value for nonce, there's no correct choice
        // to fall back to. Web3j is calling into Java so even a null would be fine.
        return BigInteger.valueOf(getAddressResponse().getNonce()!!.toLong())
    }
}
