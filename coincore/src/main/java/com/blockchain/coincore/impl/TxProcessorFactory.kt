package com.blockchain.coincore.impl

import com.blockchain.analytics.Analytics
import com.blockchain.bitpay.BitPayDataManager
import com.blockchain.bitpay.BitPayInvoiceTarget
import com.blockchain.bitpay.BitpayTxEngine
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BankAccount
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.coincore.TransactionProcessor
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TransferError
import com.blockchain.coincore.eth.EthCryptoWalletAccount
import com.blockchain.coincore.eth.EthOnChainTxEngine
import com.blockchain.coincore.eth.EthereumSendTransactionTarget
import com.blockchain.coincore.eth.EthereumSignMessageTarget
import com.blockchain.coincore.eth.WalletConnectV2SignMessageTarget
import com.blockchain.coincore.evm.L1EvmOnChainTxEngine
import com.blockchain.coincore.fiat.LinkedBankAccount
import com.blockchain.coincore.impl.txEngine.OnChainTxEngineBase
import com.blockchain.coincore.impl.txEngine.TradingToOnChainTxEngine
import com.blockchain.coincore.impl.txEngine.TransferQuotesEngine
import com.blockchain.coincore.impl.txEngine.active_rewards.ActiveRewardsDepositOnChainTxEngine
import com.blockchain.coincore.impl.txEngine.active_rewards.ActiveRewardsDepositTradingEngine
import com.blockchain.coincore.impl.txEngine.active_rewards.ActiveRewardsWithdrawTradingTxEngine
import com.blockchain.coincore.impl.txEngine.fiat.FiatDepositTxEngine
import com.blockchain.coincore.impl.txEngine.fiat.FiatWithdrawalTxEngine
import com.blockchain.coincore.impl.txEngine.interest.InterestDepositOnChainTxEngine
import com.blockchain.coincore.impl.txEngine.interest.InterestDepositTradingEngine
import com.blockchain.coincore.impl.txEngine.interest.InterestWithdrawOnChainTxEngine
import com.blockchain.coincore.impl.txEngine.interest.InterestWithdrawTradingTxEngine
import com.blockchain.coincore.impl.txEngine.sell.OnChainSellTxEngine
import com.blockchain.coincore.impl.txEngine.sell.TradingSellTxEngine
import com.blockchain.coincore.impl.txEngine.staking.StakingDepositOnChainTxEngine
import com.blockchain.coincore.impl.txEngine.staking.StakingDepositTradingEngine
import com.blockchain.coincore.impl.txEngine.staking.StakingWithdrawTradingTxEngine
import com.blockchain.coincore.impl.txEngine.swap.OnChainSwapTxEngine
import com.blockchain.coincore.impl.txEngine.swap.TradingToTradingSwapTxEngine
import com.blockchain.coincore.impl.txEngine.walletconnect.WalletConnectSignEngine
import com.blockchain.coincore.impl.txEngine.walletconnect.WalletConnectTransactionEngine
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.core.chains.ethereum.EthMessageSigner
import com.blockchain.core.custodial.data.store.TradingStore
import com.blockchain.core.fees.FeeDataManager
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.model.BankPartnerCallbackProvider
import com.blockchain.earn.domain.service.ActiveRewardsService
import com.blockchain.earn.domain.service.InterestService
import com.blockchain.earn.domain.service.StakingService
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.repositories.WithdrawLocksRepository
import com.blockchain.nabu.datamanagers.repositories.swap.SwapTransactionsStore
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.storedatasource.FlushableDataSource
import io.reactivex.rxjava3.core.Single

class TxProcessorFactory(
    private val bitPayManager: BitPayDataManager,
    private val exchangeRates: ExchangeRatesDataManager,
    private val walletManager: CustodialWalletManager,
    private val bankService: BankService,
    private val limitsDataManager: LimitsDataManager,
    private val interestBalanceStore: FlushableDataSource,
    private val interestService: InterestService,
    private val tradingStore: TradingStore,
    private val walletPrefs: WalletStatusPrefs,
    private val ethMessageSigner: EthMessageSigner,
    private val ethDataManager: EthDataManager,
    private val bankPartnerCallbackProvider: BankPartnerCallbackProvider,
    private val quotesEngineFactory: TransferQuotesEngine.Factory,
    private val fees: FeeDataManager,
    private val analytics: Analytics,
    private val withdrawLocksRepository: WithdrawLocksRepository,
    private val userIdentity: UserIdentity,
    private val swapTransactionsStore: SwapTransactionsStore,
    private val plaidFeatureFlag: FeatureFlag,
    private val stakingBalanceStore: FlushableDataSource,
    private val stakingService: StakingService,
    private val activeRewardsBalanceStore: FlushableDataSource,
    private val activeRewardsService: ActiveRewardsService
) {
    fun createProcessor(
        source: BlockchainAccount,
        target: TransactionTarget,
        action: AssetAction
    ): Single<TransactionProcessor> =
        when (source) {
            is CryptoNonCustodialAccount -> createOnChainProcessor(source, target, action)
            is CustodialTradingAccount -> createTradingProcessor(source, target)
            is CustodialInterestAccount -> createInterestWithdrawalProcessor(source, target, action)
            is CustodialActiveRewardsAccount -> createActiveRewardsWithdrawalProcessor(source, target)
            is CustodialStakingAccount -> createStakingWithdrawalProcessor(source, target)
            is BankAccount -> createFiatDepositProcessor(source, target, action)
            is FiatAccount -> createFiatWithdrawalProcessor(source, target, action)
            else -> Single.error(NotImplementedError())
        }

    private fun createInterestWithdrawalProcessor(
        source: CustodialInterestAccount,
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
                            interestBalanceStore = interestBalanceStore,
                            interestService = interestService,
                            tradingStore = tradingStore,
                            walletManager = walletManager
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
                            interestBalanceStore = interestBalanceStore,
                            interestService = interestService,
                            walletManager = walletManager
                        )
                    )
                )
            }
            else -> Single.error(IllegalStateException("$target is not supported yet"))
        }

    private fun createActiveRewardsWithdrawalProcessor(
        source: CustodialActiveRewardsAccount,
        target: TransactionTarget
    ): Single<TransactionProcessor> =
        when (target) {
            is CustodialTradingAccount -> {
                Single.just(
                    TransactionProcessor(
                        exchangeRates = exchangeRates,
                        sourceAccount = source,
                        txTarget = target,
                        engine = ActiveRewardsWithdrawTradingTxEngine(
                            activeRewardsBalanceStore = activeRewardsBalanceStore,
                            activeRewardsService = activeRewardsService,
                            tradingStore = tradingStore,
                            walletManager = walletManager
                        )
                    )
                )
            }
            else -> Single.error(IllegalStateException("$target is not supported yet"))
        }

    private fun createStakingWithdrawalProcessor(
        source: CustodialStakingAccount,
        target: TransactionTarget
    ): Single<TransactionProcessor> =
        when (target) {
            is CustodialTradingAccount -> {
                Single.just(
                    TransactionProcessor(
                        exchangeRates = exchangeRates,
                        sourceAccount = source,
                        txTarget = target,
                        engine = StakingWithdrawTradingTxEngine(
                            stakingBalanceStore = stakingBalanceStore,
                            stakingService = stakingService,
                            tradingStore = tradingStore,
                            walletManager = walletManager
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
                            bankService = bankService,
                            userIdentity = userIdentity,
                            bankPartnerCallbackProvider = bankPartnerCallbackProvider,
                            limitsDataManager = limitsDataManager,
                            withdrawLocksRepository = withdrawLocksRepository,
                            plaidFeatureFlag = plaidFeatureFlag
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
                            limitsDataManager = limitsDataManager,
                            userIdentity = userIdentity
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
        val engine = source.createTxEngine(target, action) as OnChainTxEngineBase

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
            is WalletConnectV2SignMessageTarget,
            is EthereumSignMessageTarget -> Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = WalletConnectSignEngine(
                        assetEngine = if (source is EthCryptoWalletAccount)
                            engine as EthOnChainTxEngine
                        else
                            engine as L1EvmOnChainTxEngine,
                        ethMessageSigner = ethMessageSigner
                    )
                )
            )
            is EthereumSendTransactionTarget -> Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = WalletConnectTransactionEngine(
                        feeManager = fees,
                        ethDataManager = ethDataManager
                    )
                )
            )
            is CustodialInterestAccount ->
                target.receiveAddress
                    .map {
                        TransactionProcessor(
                            exchangeRates = exchangeRates,
                            sourceAccount = source,
                            txTarget = it,
                            engine = InterestDepositOnChainTxEngine(
                                interestBalanceStore = interestBalanceStore,
                                interestService = interestService,
                                walletManager = walletManager,
                                onChainEngine = engine
                            )
                        )
                    }
            is CustodialStakingAccount ->
                target.receiveAddress
                    .map {
                        TransactionProcessor(
                            exchangeRates = exchangeRates,
                            sourceAccount = source,
                            txTarget = it,
                            engine = StakingDepositOnChainTxEngine(
                                stakingBalanceStore = stakingBalanceStore,
                                stakingService = stakingService,
                                onChainEngine = engine
                            )
                        )
                    }
            is CustodialActiveRewardsAccount ->
                target.receiveAddress
                    .map {
                        TransactionProcessor(
                            exchangeRates = exchangeRates,
                            sourceAccount = source,
                            txTarget = it,
                            engine = ActiveRewardsDepositOnChainTxEngine(
                                activeRewardsBalanceStore = activeRewardsBalanceStore,
                                activeRewardsService = activeRewardsService,
                                walletManager = walletManager,
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
                                quotesEngine = quotesEngineFactory.create(),
                                walletManager = walletManager,
                                limitsDataManager = limitsDataManager,
                                userIdentity = userIdentity,
                                engine = engine,
                                swapTransactionsStore = swapTransactionsStore
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
                        tradingStore = tradingStore,
                        quotesEngine = quotesEngineFactory.create(),
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
        target: TransactionTarget
    ) = when (target) {
        is CryptoAddress ->
            Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = TradingToOnChainTxEngine(
                        tradingStore = tradingStore,
                        walletManager = walletManager,
                        userIdentity = userIdentity,
                        limitsDataManager = limitsDataManager
                    )
                )
            )
        is EarnRewardsAccount.Interest ->
            Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = InterestDepositTradingEngine(
                        interestBalanceStore = interestBalanceStore,
                        interestService = interestService,
                        tradingStore = tradingStore,
                        walletManager = walletManager
                    )
                )
            )
        is EarnRewardsAccount.Staking ->
            Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = StakingDepositTradingEngine(
                        stakingBalanceStore = stakingBalanceStore,
                        stakingService = stakingService,
                        tradingStore = tradingStore,
                        walletManager = walletManager
                    )
                )
            )
        is EarnRewardsAccount.Active ->
            Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = ActiveRewardsDepositTradingEngine(
                        activeRewardsBalanceStore = activeRewardsBalanceStore,
                        activeRewardsService = activeRewardsService,
                        tradingStore = tradingStore,
                        walletManager = walletManager
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
                        tradingStore = tradingStore,
                        walletManager = walletManager,
                        limitsDataManager = limitsDataManager,
                        quotesEngine = quotesEngineFactory.create(),
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
                        tradingStore = tradingStore,
                        walletManager = walletManager,
                        limitsDataManager = limitsDataManager,
                        quotesEngine = quotesEngineFactory.create(),
                        userIdentity = userIdentity,
                        swapTransactionsStore = swapTransactionsStore
                    )
                )
            )
        is CryptoAccount ->
            target.receiveAddress
                .map {
                    TransactionProcessor(
                        exchangeRates = exchangeRates,
                        sourceAccount = source,
                        txTarget = it,
                        engine = TradingToOnChainTxEngine(
                            tradingStore = tradingStore,
                            walletManager = walletManager,
                            limitsDataManager = limitsDataManager,
                            userIdentity = userIdentity
                        )
                    )
                }
        else -> Single.error(TransferError("Cannot send custodial crypto to a non-crypto target"))
    }
}
