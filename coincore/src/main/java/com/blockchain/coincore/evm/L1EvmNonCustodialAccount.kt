package com.blockchain.coincore.evm

import com.blockchain.coincore.AddressResolver
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.TxSourceState
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.core.chains.EvmNetwork
import com.blockchain.core.chains.erc20.Erc20DataManager
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.domain.wallet.PubKeyStyle
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet.Companion.DEFAULT_SINGLE_ACCOUNT_INDEX
import com.blockchain.unifiedcryptowallet.domain.wallet.PublicKey
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single

class L1EvmNonCustodialAccount(
    asset: AssetInfo,
    private val ethDataManager: EthDataManager,
    private val erc20DataManager: Erc20DataManager,
    internal val address: String,
    override val label: String,
    override val exchangeRates: ExchangeRatesDataManager,
    private val walletPreferences: WalletStatusPrefs,
    override val addressResolver: AddressResolver,
    val l1Network: EvmNetwork,
) : CryptoNonCustodialAccount(asset) {

    override val isDefault: Boolean = true // Only one account, so always default

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(
            L1EvmAddress(
                asset = currency,
                address = address,
                label = label
            )
        )

    override val index: Int
        get() = DEFAULT_SINGLE_ACCOUNT_INDEX

    override suspend fun publicKey(): List<PublicKey> =
        ethDataManager.ehtAccount.publicKey?.let {
            listOf(
                PublicKey(
                    address = it,
                    descriptor = NetworkWallet.DEFAULT_ADDRESS_DESCRIPTOR,
                    style = PubKeyStyle.SINGLE
                )
            )
        } ?: throw IllegalStateException(
            "Public key for Eth account hasn't been derived"
        )

    override val sourceState: Single<TxSourceState>
        get() = super.sourceState.flatMap { state ->
            erc20DataManager.hasUnconfirmedTransactions()
                .map { hasUnconfirmed ->
                    if (hasUnconfirmed) {
                        TxSourceState.TRANSACTION_IN_FLIGHT
                    } else {
                        state
                    }
                }
        }

    override fun createTxEngine(target: TransactionTarget, action: AssetAction): TxEngine =
        L1EvmOnChainTxEngine(
            erc20DataManager = erc20DataManager,
            requireSecondPassword = erc20DataManager.requireSecondPassword,
            walletPreferences = walletPreferences,
            resolvedAddress = addressResolver.getReceiveAddress(currency, target, action)
        )
}
