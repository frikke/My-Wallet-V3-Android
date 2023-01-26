package com.blockchain.coincore.eth

import com.blockchain.coincore.AddressResolver
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.TxSourceState
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.core.fees.FeeDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.domain.wallet.PubKeyStyle
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet.Companion.DEFAULT_ADDRESS_DESCRIPTOR
import com.blockchain.unifiedcryptowallet.domain.wallet.PublicKey
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.ethereum.EthereumAccount
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.lang.IllegalStateException

class EthCryptoWalletAccount internal constructor(
    private var jsonAccount: EthereumAccount,
    private val ethDataManager: EthDataManager,
    private val fees: FeeDataManager,
    private val walletPreferences: WalletStatusPrefs,
    override val exchangeRates: ExchangeRatesDataManager,
    private val assetCatalogue: AssetCatalogue,
    override val addressResolver: AddressResolver,
) : CryptoNonCustodialAccount(
    CryptoCurrency.ETHER
) {
    internal val address: String
        get() = jsonAccount.address

    override val label: String
        get() = jsonAccount.label

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(
            EthAddress(
                address = address,
                label = label
            )
        )

    override suspend fun publicKey(): List<PublicKey> =
        jsonAccount.publicKey?.let {
            listOf(
                PublicKey(
                    address = it,
                    descriptor = DEFAULT_ADDRESS_DESCRIPTOR,
                    style = PubKeyStyle.SINGLE
                )
            )
        } ?: throw IllegalStateException("Public key for Eth account hasn't been derived")

    override val index: Int
        get() = NetworkWallet.DEFAULT_SINGLE_ACCOUNT_INDEX

    override fun updateLabel(newLabel: String): Completable {
        require(newLabel.isNotEmpty())
        return ethDataManager.updateAccountLabel(newLabel)
    }

    fun isErc20FeeTransaction(to: String): Boolean =
        assetCatalogue.supportedL2Assets(currency).firstOrNull { erc20 ->
            to.equals(erc20.l2identifier, true)
        } != null

    override val isDefault: Boolean = true // Only one ETH account, so always default

    override val sourceState: Single<TxSourceState>
        get() = super.sourceState.flatMap { state ->
            ethDataManager.isLastTxPending().map { hasUnconfirmed ->
                if (hasUnconfirmed) {
                    TxSourceState.TRANSACTION_IN_FLIGHT
                } else {
                    state
                }
            }
        }

    override fun createTxEngine(target: TransactionTarget, action: AssetAction): TxEngine =
        EthOnChainTxEngine(
            ethDataManager = ethDataManager,
            feeManager = fees,
            requireSecondPassword = ethDataManager.requireSecondPassword,
            walletPreferences = walletPreferences,
            resolvedAddress = addressResolver.getReceiveAddress(currency, target, action)
        )
}
