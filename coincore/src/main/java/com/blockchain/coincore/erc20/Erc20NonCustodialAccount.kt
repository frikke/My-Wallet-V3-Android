package com.blockchain.coincore.erc20

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.AddressResolver
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.TxSourceState
import com.blockchain.coincore.eth.L2NonCustodialAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.core.chains.erc20.Erc20DataManager
import com.blockchain.core.fees.FeeDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.FreshnessStrategy
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.unifiedcryptowallet.domain.balances.UnifiedBalanceNotFoundException
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet.Companion.DEFAULT_SINGLE_ACCOUNT_INDEX
import com.blockchain.unifiedcryptowallet.domain.wallet.PublicKey
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CoinNetwork
import info.blockchain.balance.ExchangeRate
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

class Erc20NonCustodialAccount(
    asset: AssetInfo,
    private val erc20DataManager: Erc20DataManager,
    internal val address: String,
    private val fees: FeeDataManager,
    override val label: String,
    private val currencyPrefs: CurrencyPrefs,
    override val exchangeRates: ExchangeRatesDataManager,
    private val walletPreferences: WalletStatusPrefs,
    override val addressResolver: AddressResolver,
    override val l1Network: CoinNetwork
) : L2NonCustodialAccount, CryptoNonCustodialAccount(asset) {

    override val isDefault: Boolean = true // Only one account, so always default

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(
            Erc20Address(currency, address, label)
        )

    override fun balanceRx(freshnessStrategy: FreshnessStrategy): Observable<AccountBalance> {
        return super.balanceRx(freshnessStrategy).onErrorResumeNext {
            if (it is UnifiedBalanceNotFoundException) {
                Observable.just(
                    AccountBalance.zero(
                        currency = currency,
                        exchangeRate = ExchangeRate.zeroRateExchangeRate(
                            from = currency,
                            to = currencyPrefs.selectedFiatCurrency
                        )
                    )
                )
            } else Observable.error(it)
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
}
