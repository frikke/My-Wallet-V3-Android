package com.blockchain.coincore

import com.blockchain.api.services.AddressMappingService
import com.blockchain.api.services.DomainAddressNotFound
import com.blockchain.logging.Logger
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import java.lang.IllegalStateException
import kotlinx.coroutines.rx3.asObservable

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
    val isDomain: Boolean
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
        coincore.allActiveAssets().asObservable().firstOrError().map { it.filterIsInstance<CryptoAsset>() }
            .flattenAsObservable {
                it
            }.flatMapSingle {
                it.parseAddress(address).switchIfEmpty(Single.just(NullAddress))
            }.reduce<Set<ReceiveAddress>>(mutableSetOf()) { set, t2 ->
                val s = set.plus(t2)
                s
            }.onErrorReturn {
                emptySet()
            }.map {
                it.filter { address -> address != NullAddress }.toSet()
            }

    override fun parse(address: String, ccy: AssetInfo): Maybe<ReceiveAddress> =
        isDomainAddress(address)
            .flatMapMaybe { isDomain ->
                if (isDomain) {
                    // Have to exclude the variant selector unicodes as they mess up the json in the request.
                    // For example \ufe0f unicode removes the " of the strings in the json (you can try it by
                    // copy-pasting this unicode into any text editor and inserting into a string)
                    resolveDomainAddress(
                        address = address.filterNot { variantSelectorRange.contains(it) },
                        asset = ccy
                    )
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
                    coincore[asset].parseAddress(resolved, address, true)
                }
            }.onErrorResumeNext(::handleResolutionApiError)

    private fun handleResolutionApiError(t: Throwable): Maybe<ReceiveAddress> =
        when (t) {
            is DomainAddressNotFound -> Maybe.error(TxValidationFailure(ValidationState.INVALID_DOMAIN))
            else -> {
                Logger.e(t, "Failed to resolve domain address")
                throw IllegalStateException(t)
            }
        }

    // When the input contains a '.' it can either be a domain (like name.blockchain) or a BIP21/EIP-681
    // payment link (ethereum:valid1Address/transfer?address=valid2Address&uint256=0.1"). To avoid processing
    // the latter as a domain we look for the absence '?' as well.
    private fun isDomainAddress(address: String): Single<Boolean> =
        Single.just(
            (address.contains('.') && !address.contains('?')) || !alphaNumericAndAscii.matches(address)
        )

    companion object {
        // It's easier to define what is not an emoji than what it is. At the time of writing the only way to tell
        // if the input contains emojis is by checking the unicodes of each character within the range of the
        // currently existing emojis (which keeps expanding every time a new one is created). Instead let's match
        // the input against a regular expression which matches alpha-numerics and characters of ASCII (to include
        // the symbols of the payment protocols like '&'). If the input can't be matched against this formula,
        // we should try to resolve it as a domain given that it can't be an address.
        private val alphaNumericAndAscii = Regex("^[\\p{Alnum}\\p{ASCII}]+")

        // The variant selector range as per:
        // https://en.wikipedia.org/wiki/Variation_Selectors_(Unicode_block)
        private val variantSelectorRange = CharRange('\ufe00', '\ufe0f')
    }
}
