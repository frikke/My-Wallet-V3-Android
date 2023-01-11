package com.blockchain.coincore.erc20

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.AddressResolver
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.TxSourceState
import com.blockchain.coincore.eth.MultiChainAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.core.chains.EvmNetwork
import com.blockchain.core.chains.erc20.Erc20DataManager
import com.blockchain.core.fees.FeeDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.FreshnessStrategy
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.unifiedcryptowallet.domain.balances.UnifiedBalanceNotFoundException
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet.Companion.DEFAULT_SINGLE_ACCOUNT_INDEX
import com.blockchain.unifiedcryptowallet.domain.wallet.PublicKey
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.atomic.AtomicBoolean

class Erc20NonCustodialAccount(
    asset: AssetInfo,
    private val erc20DataManager: Erc20DataManager,
    internal val address: String,
    private val fees: FeeDataManager,
    override val label: String,
    override val exchangeRates: ExchangeRatesDataManager,
    private val walletPreferences: WalletStatusPrefs,
    override val addressResolver: AddressResolver,
    override val l1Network: EvmNetwork
) : MultiChainAccount, CryptoNonCustodialAccount(asset) {

    private val hasFunds = AtomicBoolean(false)

    override val isFunded: Boolean
        get() = hasFunds.get()

    override val isDefault: Boolean = true // Only one account, so always default

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(
            Erc20Address(currency, address, label)
        )

    override fun getOnChainBalance(): Observable<Money> =
        erc20DataManager.getErc20Balance(currency)
            .doOnNext { hasFunds.set(it.balance.isPositive) }
            .doOnNext { setHasTransactions(it.hasTransactions) }
            .map { it.balance }

    override fun balanceRx(freshnessStrategy: FreshnessStrategy): Observable<AccountBalance> {
        return super.balanceRx(freshnessStrategy).onErrorResumeNext {
            if (it is UnifiedBalanceNotFoundException)
                Observable.just(AccountBalance.zero(currency))
            else Observable.error(it)
        }.doOnNext {
            hasFunds.set(it.total.isPositive)
        }
    }

    override val index: Int
        get() = DEFAULT_SINGLE_ACCOUNT_INDEX

    override suspend fun publicKey(): List<PublicKey> {
        throw IllegalAccessException("Public key of an erc20 cannot be accessed use the L1")
    }

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
        Erc20OnChainTxEngine(
            erc20DataManager = erc20DataManager,
            feeManager = fees,
            requireSecondPassword = erc20DataManager.requireSecondPassword,
            walletPreferences = walletPreferences,
            resolvedAddress = addressResolver.getReceiveAddress(currency, target, action)
        )

    companion object {
        private const val ETH_CHAIN_TX_HISTORY_MULTIPLIER = 1000
    }
}
