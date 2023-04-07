package com.blockchain.coincore.selfcustody

import com.blockchain.coincore.AddressResolver
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.core.chains.dynamicselfcustody.domain.NonCustodialService
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.domain.wallet.CoinType
import com.blockchain.domain.wallet.PubKeyStyle
import com.blockchain.outcome.map
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet.Companion.DEFAULT_ADDRESS_DESCRIPTOR
import com.blockchain.unifiedcryptowallet.domain.wallet.PublicKey
import com.blockchain.utils.rxSingleOutcome
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.NetworkType
import info.blockchain.wallet.dynamicselfcustody.DynamicHDAccount
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Utils
import org.spongycastle.util.encoders.Hex

class DynamicNonCustodialAccount(
    val payloadManager: PayloadDataManager,
    assetInfo: AssetInfo,
    val coinType: CoinType,
    override val addressResolver: AddressResolver,
    private val nonCustodialService: NonCustodialService,
    override val exchangeRates: ExchangeRatesDataManager,
    override val label: String,
    private val walletPreferences: WalletStatusPrefs
) : CryptoNonCustodialAccount(assetInfo), NetworkWallet {

    private val internalAccount: DynamicHDAccount = payloadManager.getDynamicHdAccount(coinType)
        ?: throw IllegalStateException("Unsupported Coin Configuration!")

    override val receiveAddress: Single<ReceiveAddress>
        get() = rxSingleOutcome { getReceiveAddress() }

    private suspend fun getReceiveAddress() = nonCustodialService.getAddresses(listOf(currency.networkTicker))
        .map {
            it.find {
                it.pubKey == xpubAddress && it.default
            }?.let { nonCustodialDerivedAddress ->
                DynamicNonCustodialAddress(
                    address = nonCustodialDerivedAddress.address,
                    asset = currency
                )
            } ?: throw IllegalStateException("Couldn't derive receive address for ${currency.networkTicker}")
        }

    override val isArchived: Boolean = false

    override val isDefault: Boolean = true

    override fun createTxEngine(target: TransactionTarget, action: AssetAction): TxEngine =
        DynamicOnChainTxEngine(
            nonCustodialService = nonCustodialService,
            walletPreferences = walletPreferences,
            requireSecondPassword = false,
            resolvedAddress = addressResolver.getReceiveAddress(currency, target, action)
        )

    override fun updateLabel(newLabel: String): Completable {
        return Completable.complete()
    }

    override fun archive(): Completable = Completable.complete()

    override fun unarchive(): Completable = Completable.complete()

    override fun setAsDefault(): Completable = Completable.complete()

    override val xpubAddress: String
        get() = if (coinType.network == NetworkType.SOL || coinType.network == NetworkType.XLM) {
            String(Hex.encode(internalAccount.bip39PubKey))
        } else {
            String(Hex.encode(internalAccount.address.pubKey))
        }

    override val hasStaticAddress: Boolean = false

    fun signedRawPreImage(unsignedPreImage: String): String {
        return if (coinType.network == NetworkType.SOL || coinType.network == NetworkType.XLM) {
            Utils.HEX.encode(internalAccount.signWithBip39Key(Utils.HEX.decode(unsignedPreImage)))
        } else {
            getSignature(unsignedPreImage, internalAccount.signingKey.toECKey())
        }
    }

    private fun getSignature(unsignedPreImage: String, signingKey: ECKey): String {
        val hash = Sha256Hash.wrap(unsignedPreImage)
        val resultSignature = signingKey.sign(hash)
        val r = resultSignature.r.toPaddedHexString()
        val s = resultSignature.s.toPaddedHexString()
        val v = "0${signingKey.findRecoveryId(hash, resultSignature)}"
        return r + s + v
    }

    private fun BigInteger.toPaddedHexString(): String {
        val radix = 16 // For digit to character conversion (digit to hexadecimal in this case)
        val desiredLength = 64
        val padChar = '0'
        return toString(radix).padStart(desiredLength, padChar)
    }

    override val index: Int
        get() = 0

    override suspend fun publicKey(): List<PublicKey> =
        listOf(
            PublicKey(
                address = xpubAddress,
                descriptor = DEFAULT_ADDRESS_DESCRIPTOR,
                style = PubKeyStyle.SINGLE,
            )
        )
}
