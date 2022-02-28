package com.blockchain.coincore

import com.blockchain.api.services.AddressMappingService
import com.blockchain.api.services.DomainAddressNotFound
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import java.lang.IllegalStateException
import timber.log.Timber

interface TransactionTarget {
    val label: String
    val onTxCompleted: (TxResult) -> Completable
        get() = { _ ->
            Completable.complete()
        }
}

// An invoice has a fixed amount
interface InvoiceTarget

interface ReceiveAddress : TransactionTarget {
    val address: String
}

object NullAddress : ReceiveAddress {
    override val label: String = ""
    override val address: String = ""
}

interface CryptoTarget : TransactionTarget {
    val asset: AssetInfo
    val memo: String?
        get() = null
}

interface CryptoAddress : CryptoTarget, ReceiveAddress {
    fun toUrl(amount: Money = Money.zero(asset)) = address
    val amount: Money?
        get() = null
}

interface AddressFactory {
    fun parse(address: String): Single<Set<ReceiveAddress>>
    fun parse(address: String, ccy: AssetInfo): Maybe<ReceiveAddress>
}

class AddressFactoryImpl(
    private val coincore: Coincore,
    private val addressResolver: AddressMappingService
) : AddressFactory {

    /** Build the set of possible address for a given input string.
     * If the string is not a valid address for any available tokens, then return
     * an empty set
     **/
    override fun parse(address: String): Single<Set<ReceiveAddress>> =
        Maybe.merge(
            coincore.activeCryptoAssets().map { asset ->
                asset.parseAddress(address)
                    .doOnError { Timber.e("**** ERROR: $asset") }
                    .onErrorComplete()
            }
        ).toList().map { it.toSet() }

    override fun parse(address: String, ccy: AssetInfo): Maybe<ReceiveAddress> =
        isDomainAddress(address)
            .flatMapMaybe { isDomain ->
                if (isDomain) {
                    resolveDomainAddress(address, ccy)
                } else {
                    coincore[ccy].parseAddress(address)
                }
            }

    private fun resolveDomainAddress(address: String, asset: AssetInfo): Maybe<ReceiveAddress> =
        addressResolver.resolveAssetAddress(address, asset.networkTicker)
            .flatMapMaybe { resolved ->
                if (resolved.isEmpty()) {
                    Maybe.error(TxValidationFailure(ValidationState.INVALID_DOMAIN))
                } else {
                    coincore[asset].parseAddress(resolved, address)
                }
            }.onErrorResumeNext(::handleResolutionApiError)

    private fun handleResolutionApiError(t: Throwable): Maybe<ReceiveAddress> =
        when (t) {
            is DomainAddressNotFound -> Maybe.error(TxValidationFailure(ValidationState.INVALID_DOMAIN))
            else -> {
                Timber.e(t, "Failed to resolve domain address")
                throw IllegalStateException(t)
            }
        }

    private fun isDomainAddress(address: String): Single<Boolean> =
        Single.just(address.contains('.') && !address.contains('?'))
}
