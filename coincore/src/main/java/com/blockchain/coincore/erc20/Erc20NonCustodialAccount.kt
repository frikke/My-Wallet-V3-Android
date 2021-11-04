package com.blockchain.coincore.erc20

import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.TxSourceState
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.core.chains.erc20.Erc20DataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.WalletStatus
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.atomic.AtomicBoolean
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class Erc20NonCustodialAccount(
    payloadManager: PayloadDataManager,
    asset: AssetInfo,
    private val erc20DataManager: Erc20DataManager,
    internal val address: String,
    private val fees: FeeDataManager,
    override val label: String,
    override val exchangeRates: ExchangeRatesDataManager,
    private val walletPreferences: WalletStatus,
    private val custodialWalletManager: CustodialWalletManager,
    override val baseActions: Set<AssetAction>,
    identity: UserIdentity
) : CryptoNonCustodialAccount(payloadManager, asset, custodialWalletManager, identity) {

    private val hasFunds = AtomicBoolean(false)

    override val isFunded: Boolean
        get() = hasFunds.get()

    override val isDefault: Boolean = true // Only one account, so always default

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(
            Erc20Address(asset, address, label)
        )

    override fun getOnChainBalance(): Observable<Money> =
        erc20DataManager.getErc20Balance(asset)
            .doOnNext { hasFunds.set(it.balance.isPositive) }
            .doOnNext { setHasTransactions(it.hasTransactions) }
            .map { it.balance }

    override val activity: Single<ActivitySummaryList>
        get() {
            val feedTransactions = erc20DataManager.getErc20History(asset)

            return Single.zip(
                feedTransactions,
                erc20DataManager.latestBlockNumber()
            ) { transactions, latestBlockNumber ->
                transactions.map { transaction ->
                    Erc20ActivitySummaryItem(
                        asset,
                        event = transaction,
                        accountHash = address,
                        erc20DataManager = erc20DataManager,
                        exchangeRates = exchangeRates,
                        lastBlockNumber = latestBlockNumber,
                        account = this,
                        supportsDescription = erc20DataManager.supportsErc20TxNote(asset)
                    )
                }
            }.flatMap {
                appendTradeActivity(custodialWalletManager, asset, it)
            }.doOnSuccess {
                setHasTransactions(it.isNotEmpty())
            }
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

    override fun createTxEngine(): TxEngine =
        Erc20OnChainTxEngine(
            erc20DataManager = erc20DataManager,
            feeManager = fees,
            requireSecondPassword = erc20DataManager.requireSecondPassword,
            walletPreferences = walletPreferences
        )
}
