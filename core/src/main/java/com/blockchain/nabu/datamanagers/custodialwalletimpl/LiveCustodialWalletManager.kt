package com.blockchain.nabu.datamanagers.custodialwalletimpl

import com.blockchain.api.NabuApiExceptionFactory
import com.blockchain.api.paymentmethods.models.SimpleBuyConfirmationAttributes
import com.blockchain.core.TransactionsCache
import com.blockchain.core.TransactionsRequest
import com.blockchain.core.buy.data.toBuySellOrder
import com.blockchain.core.buy.domain.SimpleBuyService
import com.blockchain.core.buy.domain.models.SimpleBuyPair
import com.blockchain.core.payments.cache.PaymentMethodsEligibilityStore
import com.blockchain.core.recurringbuy.domain.RecurringBuy
import com.blockchain.core.recurringbuy.domain.RecurringBuyService
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.KeyedFreshnessStrategy
import com.blockchain.data.onErrorReturn
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.domain.paymentmethods.model.CryptoWithdrawalFeeAndLimit
import com.blockchain.domain.paymentmethods.model.FiatWithdrawalFeeAndLimit
import com.blockchain.domain.paymentmethods.model.PaymentLimits
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.nabu.datamanagers.BankAccount
import com.blockchain.nabu.datamanagers.BuyOrderList
import com.blockchain.nabu.datamanagers.BuySellLimits
import com.blockchain.nabu.datamanagers.BuySellOrder
import com.blockchain.nabu.datamanagers.BuySellPair
import com.blockchain.nabu.datamanagers.CardAttributes
import com.blockchain.nabu.datamanagers.CardPaymentState
import com.blockchain.nabu.datamanagers.CryptoTransaction
import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.nabu.datamanagers.CustodialOrder
import com.blockchain.nabu.datamanagers.CustodialOrderState
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.PaymentAttributes
import com.blockchain.nabu.datamanagers.PaymentCardAcquirer
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.RecurringBuyOrder
import com.blockchain.nabu.datamanagers.SimplifiedDueDiligenceUserState
import com.blockchain.nabu.datamanagers.TransactionErrorMapper
import com.blockchain.nabu.datamanagers.TransactionState
import com.blockchain.nabu.datamanagers.TransactionType
import com.blockchain.nabu.datamanagers.TransferDirection
import com.blockchain.nabu.datamanagers.TransferLimits
import com.blockchain.nabu.datamanagers.repositories.swap.CustodialRepository
import com.blockchain.nabu.datamanagers.repositories.swap.TradeTransactionItem
import com.blockchain.nabu.models.data.WithdrawFeeRequest
import com.blockchain.nabu.models.responses.cards.PaymentCardAcquirerResponse
import com.blockchain.nabu.models.responses.cards.PaymentMethodResponse
import com.blockchain.nabu.models.responses.nabu.State
import com.blockchain.nabu.models.responses.simplebuy.BankAccountResponse
import com.blockchain.nabu.models.responses.simplebuy.BuySellOrderResponse
import com.blockchain.nabu.models.responses.simplebuy.ConfirmOrderRequestBody
import com.blockchain.nabu.models.responses.simplebuy.CustodialWalletOrder
import com.blockchain.nabu.models.responses.simplebuy.PaymentAttributesResponse
import com.blockchain.nabu.models.responses.simplebuy.PaymentStateResponse
import com.blockchain.nabu.models.responses.simplebuy.ProductTransferRequestBody
import com.blockchain.nabu.models.responses.simplebuy.RecurringBuyRequestBody
import com.blockchain.nabu.models.responses.simplebuy.TransactionResponse
import com.blockchain.nabu.models.responses.simplebuy.TransferRequest
import com.blockchain.nabu.models.responses.simplebuy.toRecurringBuyOrder
import com.blockchain.nabu.models.responses.swap.CreateOrderRequest
import com.blockchain.nabu.models.responses.swap.CustodialOrderResponse
import com.blockchain.nabu.service.NabuService
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.asSingle
import com.blockchain.store.getDataOrThrow
import com.blockchain.store.mapData
import com.blockchain.utils.fromIso8601ToUtc
import com.blockchain.utils.toLocalTime
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.flatMapIterable
import java.math.BigInteger
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class LiveCustodialWalletManager(
    private val assetCatalogue: AssetCatalogue,
    private val nabuService: NabuService,
    private val simpleBuyService: SimpleBuyService,
    private val transactionsCache: TransactionsCache,
    private val paymentMethodsEligibilityStore: PaymentMethodsEligibilityStore,
    private val paymentAccountMapperMappers: Map<String, PaymentAccountMapper>,
    private val currencyPrefs: CurrencyPrefs,
    private val custodialRepository: CustodialRepository,
    private val transactionErrorMapper: TransactionErrorMapper,
    private val fiatCurrenciesService: FiatCurrenciesService,
    private val recurringBuyService: RecurringBuyService
) : CustodialWalletManager {

    override val selectedFiatcurrency: FiatCurrency
        get() = currencyPrefs.selectedFiatCurrency

    override fun createOrder(
        custodialWalletOrder: CustodialWalletOrder,
        stateAction: String?,
    ): Single<BuySellOrder> =
        nabuService.createOrder(
            custodialWalletOrder,
            stateAction
        ).map { response ->
            response.toDomainOrThrow()
        }

    override fun createRecurringBuyOrder(
        recurringBuyRequestBody: RecurringBuyRequestBody,
    ): Single<RecurringBuyOrder> =
        nabuService.createRecurringBuyOrder(
            recurringBuyRequestBody
        ).map { response -> response.toRecurringBuyOrder() }

    override fun createWithdrawOrder(amount: Money, bankId: String): Completable =
        nabuService.createWithdrawOrder(
            amount = amount.toBigInteger().toString(),
            currency = amount.currencyCode,
            beneficiaryId = bankId
        )

    override fun fetchFiatWithdrawFeeAndMinLimit(
        fiatCurrency: FiatCurrency,
        product: Product,
        paymentMethodType: PaymentMethodType,
    ): Single<FiatWithdrawalFeeAndLimit> =
        nabuService.fetchWithdrawFeesAndLimits(product.toRequestString(), paymentMethodType.mapToRequest())
            .map { response ->
                val fee = response.fees.firstOrNull { it.symbol == fiatCurrency.networkTicker }?.let {
                    Money.fromMinor(fiatCurrency, it.minorValue.toBigInteger())
                } ?: Money.zero(fiatCurrency)

                val minLimit = response.minAmounts.firstOrNull { it.symbol == fiatCurrency.networkTicker }?.let {
                    Money.fromMinor(fiatCurrency, it.minorValue.toBigInteger())
                } ?: Money.zero(fiatCurrency)

                FiatWithdrawalFeeAndLimit(minLimit, fee)
            }

    private fun PaymentMethodType.mapToRequest(): String =
        when (this) {
            PaymentMethodType.BANK_TRANSFER -> WithdrawFeeRequest.BANK_TRANSFER
            PaymentMethodType.BANK_ACCOUNT -> WithdrawFeeRequest.BANK_ACCOUNT
            else -> throw IllegalStateException("map not specified for $this")
        }

    override fun fetchCryptoWithdrawFeeAndMinLimit(
        asset: AssetInfo,
        product: Product,
    ): Single<CryptoWithdrawalFeeAndLimit> =
        nabuService.fetchWithdrawFeesAndLimits(product.toRequestString(), WithdrawFeeRequest.DEFAULT)
            .map { response ->
                val fee = response.fees.firstOrNull {
                    it.symbol == asset.networkTicker
                }?.minorValue?.toBigInteger() ?: BigInteger.ZERO

                val minLimit = response.minAmounts.firstOrNull {
                    it.symbol == asset.networkTicker
                }?.minorValue?.toBigInteger() ?: BigInteger.ZERO

                CryptoWithdrawalFeeAndLimit(minLimit, fee)
            }

    override fun fetchWithdrawLocksTime(
        paymentMethodType: PaymentMethodType,
        fiatCurrency: FiatCurrency,
    ): Single<BigInteger> =
        nabuService.fetchWithdrawLocksRules(
            paymentMethodType,
            fiatCurrency.networkTicker
        ).flatMap { response ->
            response.rule?.let {
                Single.just(it.lockTime.toBigInteger())
            } ?: Single.just(BigInteger.ZERO)
        }

    override fun getSupportedBuySellCryptoCurrencies(): Single<List<CurrencyPair>> =
        simpleBuyService.getPairs()
            .mapData { response ->
                response.mapNotNull { pair ->
                    pair.toBuySellPair()?.let {
                        CurrencyPair(source = it.cryptoCurrency, destination = it.fiatCurrency)
                    }
                }
            }
            .asSingle()

    private fun SimpleBuyPair.toBuySellPair(): BuySellPair? {
        val crypto = assetCatalogue.fromNetworkTicker(pair.first)
        val fiat = assetCatalogue.fromNetworkTicker(pair.second)

        return if (crypto == null || fiat == null) {
            null
        } else {
            BuySellPair(
                cryptoCurrency = crypto,
                fiatCurrency = fiat,
                buyLimits = BuySellLimits(buyMin.toBigInteger(), buyMax.toBigInteger()),
                sellLimits = BuySellLimits(sellMin.toBigInteger(), sellMax.toBigInteger())
            )
        }
    }

    // todo(othman) refactor with TransactionsStore
    override fun getCustodialCryptoTransactions(
        asset: AssetInfo,
        product: Product,
        type: String?,
    ): Single<List<CryptoTransaction>> =

        transactionsCache.transactions(
            TransactionsRequest(
                product = product.toRequestString(),
                type = type
            )
        ).map { response ->
            response.items.filter {
                assetCatalogue.fromNetworkTicker(
                    it.amount.symbol
                )?.networkTicker == asset.networkTicker
            }.mapNotNull {
                val crypto = assetCatalogue.fromNetworkTicker(it.amount.symbol) ?: return@mapNotNull null
                val state = it.state.toTransactionState() ?: return@mapNotNull null
                val txType = it.type.toTransactionType() ?: return@mapNotNull null

                CryptoTransaction(
                    id = it.id,
                    amount = Money.fromMinor(crypto, it.amountMinor.toBigInteger()),
                    date = it.insertedAt.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
                    state = state,
                    type = txType,
                    fee = it.feeMinor?.let { fee ->
                        Money.fromMinor(crypto, fee.toBigInteger())
                    } ?: Money.zero(crypto),
                    receivingAddress = it.extraAttributes?.beneficiary?.accountRef.orEmpty(),
                    txHash = it.txHash.orEmpty(),
                    currency = currencyPrefs.selectedFiatCurrency,
                    paymentId = it.beneficiaryId
                )
            }
        }

    override fun getBankAccountDetails(currency: FiatCurrency): Single<BankAccount> =
        nabuService.getSimpleBuyBankAccountDetails(currency.networkTicker).map { response ->
            paymentAccountMapperMappers[currency.networkTicker]?.map(response)
                ?: throw IllegalStateException("Not valid Account returned")
        }

    override fun getCustodialAccountAddress(asset: Currency): Single<String> =
        nabuService.getSimpleBuyBankAccountDetails(asset.networkTicker).map { response ->
            response.address
        }

    override fun isCurrencyAvailableForTradingLegacy(assetInfo: AssetInfo): Single<Boolean> {
        val tradingCurrency = fiatCurrenciesService.selectedTradingCurrency
        return simpleBuyService.getPairs().mapData {
            it.firstOrNull { simpleBuyPair ->
                simpleBuyPair.pair.first == assetInfo.networkTicker &&
                    simpleBuyPair.pair.second == tradingCurrency.networkTicker
            } != null
        }
            .asSingle()
            .onErrorReturn { false }
    }

    override fun isCurrencyAvailableForTrading(
        assetInfo: AssetInfo,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<Boolean>> {
        val tradingCurrency = fiatCurrenciesService.selectedTradingCurrency
        return simpleBuyService.getPairs(freshnessStrategy)
            .mapData {
                it.any {
                    it.pair.first == assetInfo.networkTicker && it.pair.second == tradingCurrency.networkTicker
                }
            }
    }

    override fun availableFiatCurrenciesForTrading(assetInfo: AssetInfo): Single<List<FiatCurrency>> =
        simpleBuyService.getPairs().mapData {
            it.mapNotNull { simpleBuyPair ->
                if (simpleBuyPair.pair.first != assetInfo.networkTicker) null
                else assetCatalogue.fiatFromNetworkTicker(simpleBuyPair.pair.second)
            }
        }
            .asSingle()

    override fun isAssetSupportedForSwapLegacy(assetInfo: AssetInfo): Single<Boolean> =
        custodialRepository.getSwapAvailablePairs()
            .map { pairs ->
                assetInfo.networkTicker in pairs.map { it.source.networkTicker }
            }

    override fun isAssetSupportedForSwap(
        assetInfo: AssetInfo,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<Boolean>> {
        return simpleBuyService.getPairs(freshnessStrategy)
            .mapData {
                it.any { buyPair ->
                    buyPair.pair.first == assetInfo.networkTicker
                }
            }.onErrorReturn { false }
    }

    override fun getOutstandingBuyOrders(asset: AssetInfo): Single<BuyOrderList> =
        simpleBuyService.getBuyOrders(
            pendingOnly = true,
            shouldFilterInvalid = true
        )
            .mapData {
                it.filter { order ->
                    (order.target.currency.networkTicker == asset.networkTicker) ||
                        (order.source.currency.networkTicker == asset.networkTicker)
                }
            }
            .asSingle()

    override fun getAllOutstandingBuyOrders(): Single<BuyOrderList> =
        simpleBuyService.getBuyOrders(pendingOnly = true)
            .mapData {
                it.filter { order -> order.type == OrderType.BUY }
            }
            .asSingle()

    override fun getAllOutstandingOrders(): Single<BuyOrderList> =
        simpleBuyService.getBuyOrders(pendingOnly = true)
            .mapData {
                it.filter { order -> order.state != OrderState.UNKNOWN }
            }
            .asSingle()

    override fun getAllOrdersFor(asset: AssetInfo): Single<BuyOrderList> =
        simpleBuyService.getBuyOrders(shouldFilterInvalid = true)
            .mapData { buyOrders ->
                buyOrders.filter { order ->
                    (order.target.currency.networkTicker == asset.networkTicker) ||
                        (order.source.currency.networkTicker == asset.networkTicker)
                }
            }
            .asSingle()

    override fun getBuyOrder(orderId: String): Single<BuySellOrder> =
        simpleBuyService.getBuyOrders()
            .mapData { buyOrders ->
                buyOrders.first { it.id == orderId }
            }
            .asSingle()

    override fun deleteBuyOrder(orderId: String): Completable =
        nabuService.deleteBuyOrder(orderId)

    override fun transferFundsToWallet(amount: CryptoValue, fee: CryptoValue, walletAddress: String): Single<String> =
        nabuService.transferFunds(
            TransferRequest(
                address = walletAddress,
                currency = amount.currency.networkTicker,
                amount = amount.toBigInteger().toString(),
                fee = fee.toBigInteger().toString()
            )
        )

    override fun cancelAllPendingOrders(): Completable {
        return getAllOutstandingOrders().toObservable()
            .flatMapIterable()
            .flatMapCompletable { deleteBuyOrder(it.id) }
    }

    override fun getBankTransferLimits(fiatCurrency: FiatCurrency, onlyEligible: Boolean): Single<PaymentLimits> =
        nabuService.paymentMethods(fiatCurrency.networkTicker, onlyEligible, null).map { methods ->
            methods.filter { method -> method.eligible || !onlyEligible }
        }.map {
            it.filter { response ->
                response.type == PaymentMethodResponse.BANK_TRANSFER && response.currency == fiatCurrency.networkTicker
            }.map { paymentMethod ->
                PaymentLimits(
                    min = paymentMethod.limits.min.toBigInteger(),
                    max = paymentMethod.max().toBigInteger(),
                    currency = fiatCurrency
                )
            }.first()
        }

    private fun PaymentMethodResponse.max(): Long {
        val dailyMax = limits.daily?.available ?: limits.max
        val max = limits.max
        return dailyMax.coerceAtMost(max)
    }

    override fun getRecurringBuyForId(recurringBuyId: String): Single<RecurringBuy> {
        return recurringBuyService.getRecurringBuyForId(id = recurringBuyId).asSingle()
    }

    override fun cancelRecurringBuy(recurringBuyId: String): Completable =
        nabuService.cancelRecurringBuy(recurringBuyId)

    override fun getCardAcquirers(): Single<List<PaymentCardAcquirer>> =
        nabuService.cardAcquirers().map { paymentCardAcquirers ->
            paymentCardAcquirers.map(PaymentCardAcquirerResponse::toPaymentCardAcquirer)
        }

    override fun confirmOrder(
        orderId: String,
        attributes: SimpleBuyConfirmationAttributes?,
        paymentMethodId: String?,
        isBankPartner: Boolean?,
    ): Single<BuySellOrder> =
        nabuService.confirmOrder(
            orderId,
            ConfirmOrderRequestBody(
                paymentMethodId = paymentMethodId,
                attributes = attributes,
                paymentType = if (isBankPartner == true) {
                    PaymentMethodResponse.BANK_TRANSFER
                } else null
            )
        ).map { response ->
            response.toDomainOrThrow()
        }

    private fun BuySellOrderResponse.toDomainOrThrow(): BuySellOrder =
        ux?.let {
            throw NabuApiExceptionFactory.fromServerSideError(it)
        } ?: toBuySellOrder(
            inputCurrency = assetCatalogue.fromNetworkTicker(inputCurrency)!!,
            outputCurrency = assetCatalogue.fromNetworkTicker(outputCurrency)!!
        )

    override fun getSupportedFundsFiats(fiatCurrency: FiatCurrency): Flow<List<FiatCurrency>> {
        val paymentMethodsFlow = paymentMethods(fiatCurrency, true)
        val fiatCurrenciesFlow = fiatCurrenciesService.getTradingCurrenciesFlow()

        return combine(paymentMethodsFlow, fiatCurrenciesFlow) { paymentMethods, fiatCurrencies ->
            paymentMethods.filter { method ->
                method.type.toPaymentMethodType() == PaymentMethodType.FUNDS &&
                    fiatCurrencies.allRecommended.any { it.networkTicker == method.currency } &&
                    method.eligible
            }.mapNotNull {
                it.currency?.let { currency ->
                    assetCatalogue.fromNetworkTicker(currency) as? FiatCurrency
                }
            }
        }
    }

    /**
     * Returns a list of the available payment methods. [shouldFetchSddLimits] if true, then the responded
     * payment methods will contain the limits for SDD user. We use this argument only if we want to get back
     * these limits. To achieve back-words compatibility with the other platforms we had to use
     * a flag called visible (instead of not returning the corresponding payment methods at all.
     * Any payment method with the flag visible=false should be discarded.
     */
    private fun paymentMethods(
        currency: Currency,
        eligibleOnly: Boolean,
        shouldFetchSddLimits: Boolean = false,
    ) = paymentMethodsEligibilityStore.stream(
        KeyedFreshnessStrategy.Cached(
            key = PaymentMethodsEligibilityStore.Key(
                currency.networkTicker,
                eligibleOnly,
                shouldFetchSddLimits
            ),
            forceRefresh = false
        )
    ).mapData {
        it.filter { paymentMethod -> paymentMethod.visible }
    }.getDataOrThrow()

    override fun getExchangeSendAddressFor(asset: AssetInfo): Maybe<String> =
        nabuService.fetchExchangeSendToAddressForCrypto(asset.networkTicker)
            .flatMapMaybe { response ->
                if (response.state == State.ACTIVE) {
                    Maybe.just(response.address)
                } else {
                    Maybe.empty()
                }
            }
            .onErrorComplete()

    override fun isSimplifiedDueDiligenceEligible(): Single<Boolean> =
        nabuService.isSDDEligible().map { response ->
            response.eligible && response.tier == SDD_ELIGIBLE_TIER
        }.onErrorReturn { false }

    override fun fetchSimplifiedDueDiligenceUserState(): Single<SimplifiedDueDiligenceUserState> =
        nabuService.isSDDVerified().map {
            SimplifiedDueDiligenceUserState(
                isVerified = it.verified,
                stateFinalised = it.taskComplete
            )
        }

    override fun createCustodialOrder(
        direction: TransferDirection,
        quoteId: String,
        volume: Money,
        destinationAddress: String?,
        refundAddress: String?,
    ): Single<CustodialOrder> =
        nabuService.createCustodialOrder(
            CreateOrderRequest(
                direction = direction.toString(),
                quoteId = quoteId,
                volume = volume.toBigInteger().toString(),
                destinationAddress = destinationAddress,
                refundAddress = refundAddress
            )
        ).onErrorResumeNext {
            Single.error(transactionErrorMapper.mapToTransactionError(it))
        }.map {
            it.toCustodialOrder() ?: throw IllegalStateException("Invalid order created")
        }

    override fun getProductTransferLimits(
        currency: FiatCurrency,
        product: Product,
        orderDirection: TransferDirection?,
    ): Single<TransferLimits> {
        val side = when (product) {
            Product.BUY,
            Product.SELL -> product.name
            else -> null
        }

        val direction = if (product == Product.TRADE && orderDirection != null) {
            orderDirection.name
        } else null

        return nabuService.fetchProductLimits(
            currency.networkTicker,
            product.toRequestString(),
            side,
            direction
        ).map { response ->
            if (response.maxOrder == null && response.minOrder == null && response.maxPossibleOrder == null) {
                TransferLimits(currency)
            } else {
                TransferLimits(
                    minLimit = Money.fromMinor(currency, response.minOrder?.toBigInteger() ?: BigInteger.ZERO),
                    maxOrder = Money.fromMinor(currency, response.maxOrder?.toBigInteger() ?: BigInteger.ZERO),
                    maxLimit = Money.fromMinor(
                        currency,
                        response.maxPossibleOrder?.toBigInteger() ?: BigInteger.ZERO
                    )
                )
            }
        }
    }

    override fun getCustodialActivityForAsset(
        cryptoCurrency: AssetInfo,
        directions: Set<TransferDirection>,
    ): Single<List<TradeTransactionItem>> =
        custodialRepository.getCustodialActivityForAsset(cryptoCurrency, directions)

    override fun updateOrder(id: String, success: Boolean): Completable =
        nabuService.updateOrder(
            id = id,
            success = success
        )

    override fun createPendingDeposit(
        crypto: AssetInfo,
        address: String,
        hash: String,
        amount: Money,
        product: Product,
    ): Completable =
        nabuService.createDepositTransaction(
            currency = crypto.networkTicker,
            address = address,
            hash = hash,
            amount = amount.toBigInteger().toString(),
            product = product.toRequestString()

        )

    override fun executeCustodialTransfer(amount: Money, origin: Product, destination: Product): Completable =
        nabuService.executeTransfer(
            body = ProductTransferRequestBody(
                amount = amount.toBigInteger().toString(),
                currency = amount.currencyCode,
                origin = origin.toRequestString(),
                destination = destination.toRequestString()
            )
        )

    override fun getSwapTrades(): Single<List<CustodialOrder>> = simpleBuyService.swapOrders().asSingle()

    private fun CustodialOrderResponse.toSwapOrder(): CustodialOrder? {
        return CustodialOrder(
            id = this.id,
            state = this.state.toCustodialOrderState(),
            depositAddress = this.kind.depositAddress,
            createdAt = this.createdAt.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
            inputMoney = Money.fromMinor(
                assetCatalogue.assetInfoFromNetworkTicker(
                    this.pair.toCryptoCurrencyPair()?.source?.networkTicker.toString()
                ) ?: return null,
                this.priceFunnel.inputMoney.toBigInteger()
            ),
            outputMoney = Money.fromMinor(
                assetCatalogue.assetInfoFromNetworkTicker(
                    this.pair.toCryptoCurrencyPair()?.destination?.networkTicker.toString()
                ) ?: return null,
                this.priceFunnel.outputMoney.toBigInteger()
            )
        )
    }

    private fun CustodialOrderResponse.toCustodialOrder(): CustodialOrder? {
        return CustodialOrder(
            id = this.id,
            state = this.state.toCustodialOrderState(),
            depositAddress = this.kind.depositAddress,
            createdAt = this.createdAt.fromIso8601ToUtc() ?: Date(),

            inputMoney = CurrencyPair.fromRawPair(pair, assetCatalogue)?.let {
                Money.fromMinor(it.source, priceFunnel.inputMoney.toBigInteger())
            } ?: return null,

            outputMoney = CurrencyPair.fromRawPair(pair, assetCatalogue)?.let {
                Money.fromMinor(it.source, priceFunnel.outputMoney.toBigInteger())
            } ?: return null,
        )
    }

    private fun String.toCryptoCurrencyPair(): CurrencyPair? {
        return CurrencyPair.fromRawPair(this, assetCatalogue)
    }

    companion object {
        private const val ACH_CURRENCY = "USD"

        @Deprecated("use SddRepository")
        private const val SDD_ELIGIBLE_TIER = 3
    }
}

private fun Product.toRequestString(): String =
    when (this) {
        Product.TRADE -> "SWAP"
        Product.BUY,
        Product.SELL -> "SIMPLEBUY"
        else -> this.toString()
    }

fun String.toTransactionState(): TransactionState? =
    when (this) {
        TransactionResponse.COMPLETE -> TransactionState.COMPLETED
        TransactionResponse.REJECTED,
        TransactionResponse.FAILED,
        -> TransactionState.FAILED
        TransactionResponse.PENDING,
        TransactionResponse.CLEARED,
        TransactionResponse.FRAUD_REVIEW,
        TransactionResponse.MANUAL_REVIEW -> TransactionState.PENDING
        else -> null
    }

fun String.toCustodialOrderState(): CustodialOrderState =
    when (this) {
        CustodialOrderResponse.CREATED -> CustodialOrderState.CREATED
        CustodialOrderResponse.PENDING_CONFIRMATION -> CustodialOrderState.PENDING_CONFIRMATION
        CustodialOrderResponse.PENDING_EXECUTION -> CustodialOrderState.PENDING_EXECUTION
        CustodialOrderResponse.PENDING_DEPOSIT -> CustodialOrderState.PENDING_DEPOSIT
        CustodialOrderResponse.PENDING_LEDGER -> CustodialOrderState.PENDING_LEDGER
        CustodialOrderResponse.FINISH_DEPOSIT -> CustodialOrderState.FINISH_DEPOSIT
        CustodialOrderResponse.PENDING_WITHDRAWAL -> CustodialOrderState.PENDING_WITHDRAWAL
        CustodialOrderResponse.EXPIRED -> CustodialOrderState.EXPIRED
        CustodialOrderResponse.FINISHED -> CustodialOrderState.FINISHED
        CustodialOrderResponse.CANCELED -> CustodialOrderState.CANCELED
        CustodialOrderResponse.FAILED -> CustodialOrderState.FAILED
        else -> CustodialOrderState.UNKNOWN
    }

private fun String.toTransactionType(): TransactionType? =
    when (this) {
        TransactionResponse.DEPOSIT,
        TransactionResponse.CHARGE -> TransactionType.DEPOSIT
        TransactionResponse.WITHDRAWAL -> TransactionType.WITHDRAWAL
        else -> null
    }

enum class OrderType {
    BUY,
    SELL,
    RECURRING_BUY
}

fun PaymentAttributesResponse.toPaymentAttributes(): PaymentAttributes {
    val cardAttributes = when {
        cardProvider != null -> CardAttributes.Provider(
            cardAcquirerName = cardProvider.cardAcquirerName,
            cardAcquirerAccountCode = cardProvider.cardAcquirerAccountCode,
            paymentLink = cardProvider.paymentLink.orEmpty(),
            paymentState = cardProvider.paymentState.toCardPaymentState(),
            clientSecret = cardProvider.clientSecret.orEmpty(),
            publishableApiKey = cardProvider.publishableApiKey.orEmpty()
        )
        everypay != null -> CardAttributes.EveryPay(
            paymentLink = everypay.paymentLink,
            paymentState = everypay.paymentState.toCardPaymentState()
        )
        else -> CardAttributes.Empty
    }
    return PaymentAttributes(
        authorisationUrl = authorisationUrl,
        cardAttributes = cardAttributes
    )
}

fun PaymentStateResponse?.toCardPaymentState() =
    when (this) {
        PaymentStateResponse.WAITING_FOR_3DS_RESPONSE -> CardPaymentState.WAITING_FOR_3DS
        PaymentStateResponse.CONFIRMED_3DS -> CardPaymentState.CONFIRMED_3DS
        PaymentStateResponse.SETTLED -> CardPaymentState.SETTLED
        PaymentStateResponse.VOIDED -> CardPaymentState.VOIDED
        PaymentStateResponse.ABANDONED -> CardPaymentState.ABANDONED
        PaymentStateResponse.FAILED -> CardPaymentState.FAILED
        PaymentStateResponse.INITIAL,
        null -> CardPaymentState.INITIAL
    }

fun String.toPaymentMethodType(): PaymentMethodType =
    when (this) {
        PaymentMethodResponse.PAYMENT_CARD -> PaymentMethodType.PAYMENT_CARD
        PaymentMethodResponse.BANK_TRANSFER -> PaymentMethodType.BANK_TRANSFER
        PaymentMethodResponse.FUNDS -> PaymentMethodType.FUNDS
        else -> PaymentMethodType.UNKNOWN
    }

private fun PaymentCardAcquirerResponse.toPaymentCardAcquirer() =
    PaymentCardAcquirer(
        cardAcquirerName = cardAcquirerName,
        cardAcquirerAccountCodes = cardAcquirerAccountCodes,
        apiKey = apiKey
    )

private fun PaymentMethodResponse.isEligibleCard() =
    eligible && type.toPaymentMethodType() == PaymentMethodType.PAYMENT_CARD

interface PaymentAccountMapper {
    fun map(bankAccountResponse: BankAccountResponse): BankAccount?
}

private data class CustodialFiatBalance(
    val currency: FiatCurrency,
    val available: Boolean,
    val balance: Money,
)
