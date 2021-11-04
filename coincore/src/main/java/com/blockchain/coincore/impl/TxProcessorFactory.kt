package com.blockchain.coincore.impl

import com.blockchain.banking.BankPartnerCallbackProvider
import com.blockchain.bitpay.BitPayDataManager
import com.blockchain.bitpay.BitPayInvoiceTarget
import com.blockchain.bitpay.BitpayTxEngine
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.WalletStatus
import io.reactivex.rxjava3.core.Single
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BankAccount
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.coincore.TransactionProcessor
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TransferError
import com.blockchain.coincore.fiat.LinkedBankAccount
import com.blockchain.coincore.impl.txEngine.FiatDepositTxEngine
import com.blockchain.coincore.impl.txEngine.FiatWithdrawalTxEngine
import com.blockchain.coincore.impl.txEngine.OnChainTxEngineBase
import com.blockchain.coincore.impl.txEngine.TradingToOnChainTxEngine
import com.blockchain.coincore.impl.txEngine.TransferQuotesEngine
import com.blockchain.coincore.impl.txEngine.interest.InterestDepositOnChainTxEngine
import com.blockchain.coincore.impl.txEngine.interest.InterestDepositTradingEngine
import com.blockchain.coincore.impl.txEngine.interest.InterestWithdrawOnChainTxEngine
import com.blockchain.coincore.impl.txEngine.interest.InterestWithdrawTradingTxEngine
import com.blockchain.coincore.impl.txEngine.sell.OnChainSellTxEngine
import com.blockchain.coincore.impl.txEngine.sell.TradingSellTxEngine
import com.blockchain.coincore.impl.txEngine.swap.OnChainSwapTxEngine
import com.blockchain.coincore.impl.txEngine.swap.TradingToTradingSwapTxEngine
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.repositories.WithdrawLocksRepository

class TxProcessorFactory(
    private val bitPayManager: BitPayDataManager,
    private val exchangeRates: ExchangeRatesDataManager,
    private val walletManager: CustodialWalletManager,
    private val limitsDataManager: LimitsDataManager,
    private val interestBalances: InterestBalanceDataManager,
    private val walletPrefs: WalletStatus,
    private val bankPartnerCallbackProvider: BankPartnerCallbackProvider,
    private val quotesEngine: TransferQuotesEngine,
    private val analytics: Analytics,
    private val withdrawLocksRepository: WithdrawLocksRepository,
    private val userIdentity: UserIdentity
) {
    fun createProcessor(
        source: BlockchainAccount,
        target: TransactionTarget,
        action: AssetAction
    ): Single<TransactionProcessor> =
        when (source) {
            is CryptoNonCustodialAccount -> createOnChainProcessor(source, target, action)
            is CustodialTradingAccount -> createTradingProcessor(source, target, action)
            is CryptoInterestAccount -> createInterestWithdrawalProcessor(source, target, action)
            is BankAccount -> createFiatDepositProcessor(source, target, action)
            is FiatAccount -> createFiatWithdrawalProcessor(source, target, action)
            else -> Single.error(NotImplementedError())
        }

    private fun createInterestWithdrawalProcessor(
        source: CryptoInterestAccount,
        target: TransactionTarget,
        action: AssetAction
    ): Single<TransactionProcessor> =
        when (target) {
            is CustodialTradingAccount -> {
                Single.just(
                    TransactionProcessor(
                        exchangeRates = exchangeRates,
                        sourceAccount = source,
                        txTarget = target,
                        engine = InterestWithdrawTradingTxEngine(
                            walletManager = walletManager,
                            interestBalances = interestBalances
                        )
                    )
                )
            }
            is CryptoNonCustodialAccount -> {
                Single.just(
                    TransactionProcessor(
                        exchangeRates = exchangeRates,
                        sourceAccount = source,
                        txTarget = target,
                        engine = InterestWithdrawOnChainTxEngine(
                            walletManager = walletManager,
                            interestBalances = interestBalances
                        )
                    )
                )
            }
            else -> Single.error(IllegalStateException("$target is not supported yet"))
        }

    private fun createFiatDepositProcessor(
        source: BlockchainAccount,
        target: TransactionTarget,
        action: AssetAction
    ): Single<TransactionProcessor> =
        when (target) {
            is FiatAccount -> {
                Single.just(
                    TransactionProcessor(
                        exchangeRates = exchangeRates,
                        sourceAccount = source,
                        txTarget = target,
                        engine = FiatDepositTxEngine(
                            walletManager = walletManager,
                            userIdentity = userIdentity,
                            bankPartnerCallbackProvider = bankPartnerCallbackProvider,
                            limitsDataManager = limitsDataManager,
                            withdrawLocksRepository = withdrawLocksRepository
                        )
                    )
                )
            }
            else -> {
                Single.error(IllegalStateException("not supported yet"))
            }
        }

    private fun createFiatWithdrawalProcessor(
        source: BlockchainAccount,
        target: TransactionTarget,
        action: AssetAction
    ): Single<TransactionProcessor> =
        when (target) {
            is LinkedBankAccount -> {
                Single.just(
                    TransactionProcessor(
                        exchangeRates = exchangeRates,
                        sourceAccount = source,
                        txTarget = target,
                        engine = FiatWithdrawalTxEngine(
                            walletManager = walletManager,
                            limitsDataManager = limitsDataManager
                        )
                    )
                )
            }
            else -> {
                Single.error(IllegalStateException("not supported yet"))
            }
        }

    private fun createOnChainProcessor(
        source: CryptoNonCustodialAccount,
        target: TransactionTarget,
        action: AssetAction
    ): Single<TransactionProcessor> {
        val engine = source.createTxEngine() as OnChainTxEngineBase

        return when (target) {
            is BitPayInvoiceTarget -> Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = BitpayTxEngine(
                        bitPayDataManager = bitPayManager,
                        walletPrefs = walletPrefs,
                        assetEngine = engine,
                        analytics = analytics
                    )
                )
            )
            is CryptoInterestAccount ->
                target.receiveAddress
                    .map {
                        TransactionProcessor(
                            exchangeRates = exchangeRates,
                            sourceAccount = source,
                            txTarget = it,
                            engine = InterestDepositOnChainTxEngine(
                                walletManager = walletManager,
                                interestBalances = interestBalances,
                                onChainEngine = engine
                            )
                        )
                    }
            is CryptoAddress -> Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = engine
                )
            )
            is CryptoAccount ->
                if (action != AssetAction.Swap) {
                    target.receiveAddress.map {
                        TransactionProcessor(
                            exchangeRates = exchangeRates,
                            sourceAccount = source,
                            txTarget = it,
                            engine = engine
                        )
                    }
                } else {
                    Single.just(
                        TransactionProcessor(
                            exchangeRates = exchangeRates,
                            sourceAccount = source,
                            txTarget = target,
                            engine = OnChainSwapTxEngine(
                                quotesEngine = quotesEngine,
                                walletManager = walletManager,
                                limitsDataManager = limitsDataManager,
                                userIdentity = userIdentity,
                                engine = engine
                            )
                        )
                    )
                }
            is FiatAccount -> Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = OnChainSellTxEngine(
                        quotesEngine = quotesEngine,
                        walletManager = walletManager,
                        limitsDataManager = limitsDataManager,
                        userIdentity = userIdentity,
                        engine = engine
                    )
                )
            )
            else -> Single.error(TransferError("Cannot send non-custodial crypto to a non-crypto target"))
        }
    }

    private fun createTradingProcessor(
        source: CustodialTradingAccount,
        target: TransactionTarget,
        action: AssetAction
    ) = when (target) {
        is CryptoAddress ->
            Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = TradingToOnChainTxEngine(
                        walletManager = walletManager,
                        limitsDataManager = limitsDataManager,
                        isNoteSupported = source.isNoteSupported
                    )
                )
            )
        is InterestAccount ->
            Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = InterestDepositTradingEngine(
                        walletManager = walletManager,
                        interestBalances = interestBalances
                    )
                )
            )
        is FiatAccount ->
            Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = TradingSellTxEngine(
                        walletManager = walletManager,
                        limitsDataManager = limitsDataManager,
                        quotesEngine = quotesEngine,
                        userIdentity = userIdentity
                    )
                )
            )
        is TradingAccount ->
            Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = TradingToTradingSwapTxEngine(
                        walletManager = walletManager,
                        limitsDataManager = limitsDataManager,
                        quotesEngine = quotesEngine,
                        userIdentity = userIdentity
                    )
                )
            )
        is CryptoAccount -> target.receiveAddress
            .map {
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = it,
                    engine = TradingToOnChainTxEngine(
                        walletManager = walletManager,
                        limitsDataManager = limitsDataManager,
                        isNoteSupported = source.isNoteSupported
                    )
                )
            }
        else -> Single.error(TransferError("Cannot send custodial crypto to a non-crypto target"))
    }
}
