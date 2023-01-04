package com.blockchain.coincore.eth

import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.AddressResolver
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.TxSourceState
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.core.chains.EvmNetwork
import com.blockchain.core.chains.erc20.data.store.L1BalanceStore
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.core.fees.FeeDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.data.RefreshStrategy
import com.blockchain.domain.wallet.PubKeyStyle
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.store.asObservable
import com.blockchain.store.mapData
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet.Companion.MULTIPLE_ADDRESSES_DESCRIPTOR
import com.blockchain.unifiedcryptowallet.domain.wallet.PublicKey
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import info.blockchain.wallet.ethereum.EthUrls
import info.blockchain.wallet.ethereum.EthereumAccount
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf

/*internal*/ class EthCryptoWalletAccount internal constructor(
    private var jsonAccount: EthereumAccount,
    private val ethDataManager: EthDataManager,
    private val fees: FeeDataManager,
    private val walletPreferences: WalletStatusPrefs,
    override val exchangeRates: ExchangeRatesDataManager,
    private val custodialWalletManager: CustodialWalletManager,
    private val assetCatalogue: AssetCatalogue,
    override val addressResolver: AddressResolver,
    override val l1Network: EvmNetwork
) : MultiChainAccount,
    CryptoNonCustodialAccount(
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
    private val l1BalanceStore: L1BalanceStore by scopedInject()

    override fun getOnChainBalance(): Observable<Money> =
        // Only get the balance for ETH from the node if we are on the Ethereum network
        if (l1Network.networkTicker == currency.networkTicker) {
            // TODO AND-5913 Use result/either and coroutines
            l1BalanceStore.stream(
                FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh).withKey(L1BalanceStore.Key(EthUrls.ETH_NODES))
            ).catch {
                emit(DataResource.Data(BigInteger.ZERO))
            }.mapData { balance ->
                Money.fromMinor(currency, balance)
            }
        } else {
            // TODO get the L2 balance of Eth from the backend
            flowOf(DataResource.Data(Money.fromMajor(currency, BigDecimal.ZERO)))
        }.asObservable()

    override suspend fun publicKey(): List<PublicKey> =
        jsonAccount.publicKey?.let {
            listOf(
                PublicKey(
                    address = it,
                    descriptor = MULTIPLE_ADDRESSES_DESCRIPTOR,
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

    override val activity: Single<ActivitySummaryList>
        get() = ethDataManager.getLatestBlockNumber()
            .flatMap { latestBlock ->
                ethDataManager.getEthTransactions()
                    .map { list ->
                        list.map { transaction ->
                            val isEr20FeeTransaction = isErc20FeeTransaction(transaction.to)
                            EthActivitySummaryItem(
                                ethDataManager,
                                transaction,
                                isEr20FeeTransaction,
                                latestBlock.number.toLong(),
                                exchangeRates,
                                account = this
                            )
                        }
                    }
                    .flatMap {
                        appendTradeActivity(custodialWalletManager, currency, it)
                    }
            }
            .doOnSuccess { setHasTransactions(it.isNotEmpty()) }

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
