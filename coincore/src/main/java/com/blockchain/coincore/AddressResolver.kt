package com.blockchain.coincore

import com.blockchain.bitpay.BitPayInvoiceTarget
import com.blockchain.coincore.impl.CustodialInterestAccount
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Single

interface AddressResolver {
    fun getReceiveAddress(currency: Currency, target: TransactionTarget, action: AssetAction): Single<String>
}

internal class IdentityAddressResolver : AddressResolver {
    override fun getReceiveAddress(
        currency: Currency,
        target: TransactionTarget,
        action: AssetAction
    ): Single<String> {
        return when (target) {
            is BitPayInvoiceTarget -> Single.just(target.address)
            is CustodialInterestAccount -> target.receiveAddress.map { it.address }
            is CryptoAddress -> Single.just(target.address)
            is CryptoAccount -> target.receiveAddress.map { it.address }
            is FiatAccount -> target.receiveAddress.map { it.address }
            else -> Single.error(TransferError("Cannot send non-custodial crypto to a non-crypto target"))
        }
    }
}
