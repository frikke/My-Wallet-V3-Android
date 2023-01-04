package com.blockchain.core.buy.data

import com.blockchain.core.buy.data.dataresources.BuyOrdersStore
import com.blockchain.core.buy.data.dataresources.BuyPairsStore
import com.blockchain.core.buy.data.dataresources.SimpleBuyEligibilityStore
import com.blockchain.core.buy.data.dataresources.TransactionsStore
import com.blockchain.core.buy.domain.SimpleBuyService
import com.blockchain.core.buy.domain.models.SimpleBuyEligibility
import com.blockchain.core.buy.domain.models.SimpleBuyPair
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.data.KeyedFreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.extensions.safeLet
import com.blockchain.nabu.datamanagers.ApprovalErrorStatus
import com.blockchain.nabu.datamanagers.BuyOrderList
import com.blockchain.nabu.datamanagers.BuySellLimits
import com.blockchain.nabu.datamanagers.BuySellOrder
import com.blockchain.nabu.datamanagers.BuySellPair
import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.nabu.datamanagers.CustodialOrder
import com.blockchain.nabu.datamanagers.FiatTransaction
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.TransactionType
import com.blockchain.nabu.datamanagers.custodialwalletimpl.OrderType
import com.blockchain.nabu.datamanagers.custodialwalletimpl.toCustodialOrderState
import com.blockchain.nabu.datamanagers.custodialwalletimpl.toPaymentAttributes
import com.blockchain.nabu.datamanagers.custodialwalletimpl.toPaymentMethodType
import com.blockchain.nabu.datamanagers.custodialwalletimpl.toTransactionState
import com.blockchain.nabu.datamanagers.repositories.swap.SwapTransactionsStore
import com.blockchain.nabu.models.responses.simplebuy.BuySellOrderResponse
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyEligibilityDto
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyPairDto
import com.blockchain.nabu.models.responses.simplebuy.TransactionResponse
import com.blockchain.nabu.models.responses.swap.CustodialOrderResponse
import com.blockchain.store.mapData
import com.blockchain.utils.fromIso8601ToUtc
import com.blockchain.utils.toLocalTime
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import java.util.Date
import kotlinx.coroutines.flow.Flow

class SimpleBuyRepository(
    private val simpleBuyEligibilityStore: SimpleBuyEligibilityStore,
    private val buyPairsStore: BuyPairsStore,
    private val buyOrdersStore: BuyOrdersStore,
    private val swapOrdersStore: SwapTransactionsStore,
    private val transactionsStore: TransactionsStore,
    private val assetCatalogue: AssetCatalogue
) : SimpleBuyService {

    override fun getEligibility(
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<SimpleBuyEligibility>> {
        return simpleBuyEligibilityStore.stream(freshnessStrategy)
            .mapData { it.toDomain() }
    }

    override fun isEligible(freshnessStrategy: FreshnessStrategy): Flow<DataResource<Boolean>> {
        return getEligibility(freshnessStrategy).mapData { it.simpleBuyTradingEligible }
    }

    override fun getPairs(freshnessStrategy: FreshnessStrategy): Flow<DataResource<List<SimpleBuyPair>>> {
        return buyPairsStore.stream(freshnessStrategy).mapData {
            it.pairs.map { it.toDomain() }
        }
    }

    override fun getSupportedBuySellCryptoCurrencies(
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<List<CurrencyPair>>> {
        return buyPairsStore.stream(freshnessStrategy).mapData { response ->
            response.pairs.mapNotNull { pair ->
                pair.toBuySellPair()?.let {
                    CurrencyPair(source = it.cryptoCurrency, destination = it.fiatCurrency)
                }
            }
        }
    }

    private fun SimpleBuyPairDto.toBuySellPair(): BuySellPair? {
        val parts = pair.split("-")
        val crypto = parts.getOrNull(0)?.let {
            assetCatalogue.fromNetworkTicker(it)
        }
        val fiat = parts.getOrNull(1)?.let {
            assetCatalogue.fromNetworkTicker(it)
        }

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

    override fun getBuyOrders(pendingOnly: Boolean, shouldFilterInvalid: Boolean): Flow<DataResource<BuyOrderList>> {
        return buyOrdersStore.stream(
            request = KeyedFreshnessStrategy.Cached(
                key = BuyOrdersStore.Key(pendingOnly = pendingOnly),
                refreshStrategy = RefreshStrategy.ForceRefresh
            )
        ).mapData {
            if (shouldFilterInvalid) {
                it.filterNot { order ->
                    order.processingErrorType == BuySellOrderResponse.ISSUER_PROCESSING_ERROR ||
                        order.paymentError == BuySellOrderResponse.APPROVAL_ERROR_REJECTED ||
                        order.paymentError == BuySellOrderResponse.APPROVAL_ERROR_EXPIRED ||
                        order.state == BuySellOrderResponse.EXPIRED
                }
            } else {
                it
            }
                .mapNotNull { order ->
                    safeLet(
                        assetCatalogue.fromNetworkTicker(order.inputCurrency),
                        assetCatalogue.fromNetworkTicker(order.outputCurrency)
                    ) { inputCurrency, outputCurrency ->
                        order.toBuySellOrder(inputCurrency, outputCurrency)
                    }
                }
        }
    }

    override fun swapOrders(): Flow<DataResource<List<CustodialOrder>>> {
        return swapOrdersStore.stream(FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh))
            .mapData { response ->
                response.mapNotNull { orderResp ->
                    val currencyPair = CurrencyPair.fromRawPair(orderResp.pair, assetCatalogue)
                    currencyPair?.let {
                        safeLet(
                            assetCatalogue.assetInfoFromNetworkTicker(
                                currencyPair.source.networkTicker
                            ),
                            assetCatalogue.assetInfoFromNetworkTicker(
                                currencyPair.destination.networkTicker
                            )
                        ) { inputCurrency, outputCurrency ->
                            orderResp.toSwapOrder(
                                inputCurrency = inputCurrency,
                                outputCurrency = outputCurrency
                            )
                        }
                    }
                }
            }
    }

    override fun getFiatTransactions(
        freshnessStrategy: FreshnessStrategy,
        fiatCurrency: FiatCurrency,
        product: Product,
        type: String?
    ): Flow<DataResource<List<FiatTransaction>>> {
        return transactionsStore.stream(
            freshnessStrategy.withKey(
                TransactionsStore.Key(product = product, type = type)
            )
        ).mapData { response ->
            response.items.filter {
                assetCatalogue.fromNetworkTicker(
                    it.amount.symbol
                )?.networkTicker == fiatCurrency.networkTicker
            }.filterNot {
                it.hasCardOrBankFailure()
            }.mapNotNull {
                val state = it.state.toTransactionState() ?: return@mapNotNull null
                val txType = it.type.toTransactionType() ?: return@mapNotNull null
                FiatTransaction(
                    id = it.id,
                    amount = Money.fromMinor(fiatCurrency, it.amountMinor.toBigInteger()) as FiatValue,
                    date = it.insertedAt.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
                    state = state,
                    type = txType,
                    paymentId = it.beneficiaryId
                )
            }
        }
    }
}

private fun SimpleBuyEligibilityDto.toDomain() = run {
    SimpleBuyEligibility(
        eligible = eligible,
        simpleBuyTradingEligible = simpleBuyTradingEligible,
        pendingDepositSimpleBuyTrades = pendingDepositSimpleBuyTrades,
        maxPendingDepositSimpleBuyTrades = maxPendingDepositSimpleBuyTrades
    )
}

private fun SimpleBuyPairDto.toDomain() = run {
    SimpleBuyPair(
        pair = pair.split("-").run { Pair(first(), last()) },
        buyMin = buyMin,
        buyMax = buyMax,
        sellMin = sellMin,
        sellMax = sellMax
    )
}

fun BuySellOrderResponse.toBuySellOrder(inputCurrency: Currency, outputCurrency: Currency): BuySellOrder {
    return BuySellOrder(
        id = id,
        pair = pair,
        source = Money.fromMinor(inputCurrency, inputQuantity.toBigInteger()),
        target = Money.fromMinor(outputCurrency, outputQuantity.toBigInteger()),
        state = state.toLocalState(),
        expires = expiresAt.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
        updated = updatedAt.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
        created = insertedAt.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
        fee = fee?.let {
            Money.fromMinor(inputCurrency, it.toBigInteger())
        },
        paymentMethodId = paymentMethodId ?: (
            when (paymentType.toPaymentMethodType()) {
                PaymentMethodType.FUNDS -> PaymentMethod.FUNDS_PAYMENT_ID
                PaymentMethodType.BANK_TRANSFER -> PaymentMethod.UNDEFINED_BANK_TRANSFER_PAYMENT_ID
                else -> PaymentMethod.UNDEFINED_CARD_PAYMENT_ID
            }
            ),
        paymentMethodType = paymentType.toPaymentMethodType(),
        price = price?.let {
            Money.fromMinor(inputCurrency, it.toBigInteger())
        },
        orderValue = Money.fromMinor(outputCurrency, outputQuantity.toBigInteger()),
        attributes = attributes?.toPaymentAttributes(),
        type = type(),
        paymentError = paymentError?.toApprovalError() ?: ApprovalErrorStatus.None,
        depositPaymentId = depositPaymentId.orEmpty(),
        approvalErrorStatus = attributes?.error?.toApprovalError() ?: ApprovalErrorStatus.None,
        failureReason = failureReason,
        recurringBuyId = recurringBuyId
    )
}

private fun String.toLocalState(): OrderState =
    when (this) {
        BuySellOrderResponse.PENDING_DEPOSIT -> OrderState.AWAITING_FUNDS
        BuySellOrderResponse.FINISHED -> OrderState.FINISHED
        BuySellOrderResponse.PENDING_CONFIRMATION -> OrderState.PENDING_CONFIRMATION
        BuySellOrderResponse.PENDING_EXECUTION,
        BuySellOrderResponse.DEPOSIT_MATCHED -> OrderState.PENDING_EXECUTION
        BuySellOrderResponse.FAILED,
        BuySellOrderResponse.EXPIRED -> OrderState.FAILED
        BuySellOrderResponse.CANCELED -> OrderState.CANCELED
        else -> OrderState.UNKNOWN
    }

private fun String.toApprovalError(): ApprovalErrorStatus =
    when (this) {
        // Card create errors
        BuySellOrderResponse.CARD_CREATE_DUPLICATE -> ApprovalErrorStatus.CardDuplicate
        BuySellOrderResponse.CARD_CREATE_FAILED -> ApprovalErrorStatus.CardCreateFailed
        BuySellOrderResponse.CARD_CREATE_ABANDONED -> ApprovalErrorStatus.CardCreateAbandoned
        BuySellOrderResponse.CARD_CREATE_EXPIRED -> ApprovalErrorStatus.CardCreateExpired
        BuySellOrderResponse.CARD_CREATE_BANK_DECLINED -> ApprovalErrorStatus.CardCreateBankDeclined
        BuySellOrderResponse.CARD_CREATE_DEBIT_ONLY -> ApprovalErrorStatus.CardCreateDebitOnly
        BuySellOrderResponse.CARD_CREATE_NO_TOKEN -> ApprovalErrorStatus.CardCreateNoToken
        // Card payment errors
        BuySellOrderResponse.CARD_PAYMENT_NOT_SUPPORTED -> ApprovalErrorStatus.CardPaymentNotSupported
        BuySellOrderResponse.CARD_PAYMENT_FAILED -> ApprovalErrorStatus.CardPaymentFailed
        BuySellOrderResponse.CARD_PAYMENT_ABANDONED -> ApprovalErrorStatus.CardCreateAbandoned // map will change
        BuySellOrderResponse.CARD_PAYMENT_EXPIRED -> ApprovalErrorStatus.CardCreateExpired // map will change
        BuySellOrderResponse.CARD_PAYMENT_INSUFFICIENT_FUNDS -> ApprovalErrorStatus.InsufficientFunds
        BuySellOrderResponse.CARD_PAYMENT_DEBIT_ONLY -> ApprovalErrorStatus.CardPaymentDebitOnly
        BuySellOrderResponse.CARD_PAYMENT_BLOCKCHAIN_DECLINED -> ApprovalErrorStatus.CardBlockchainDecline
        BuySellOrderResponse.CARD_PAYMENT_ACQUIRER_DECLINED -> ApprovalErrorStatus.CardAcquirerDecline
        // Bank transfer payment errors
        BuySellOrderResponse.APPROVAL_ERROR_INVALID,
        BuySellOrderResponse.APPROVAL_ERROR_ACCOUNT_INVALID -> ApprovalErrorStatus.Invalid
        BuySellOrderResponse.APPROVAL_ERROR_FAILED -> ApprovalErrorStatus.Failed
        BuySellOrderResponse.APPROVAL_ERROR_DECLINED -> ApprovalErrorStatus.Declined
        BuySellOrderResponse.APPROVAL_ERROR_REJECTED -> ApprovalErrorStatus.Rejected
        BuySellOrderResponse.APPROVAL_ERROR_EXPIRED -> ApprovalErrorStatus.Expired
        BuySellOrderResponse.APPROVAL_ERROR_EXCEEDED -> ApprovalErrorStatus.LimitedExceeded
        BuySellOrderResponse.APPROVAL_ERROR_FAILED_INTERNAL -> ApprovalErrorStatus.FailedInternal
        BuySellOrderResponse.APPROVAL_ERROR_INSUFFICIENT_FUNDS -> ApprovalErrorStatus.InsufficientFunds
        else -> ApprovalErrorStatus.Undefined(this)
    }

private fun BuySellOrderResponse.type() =
    when {
        side == "BUY" && this.recurringBuyId != null -> OrderType.RECURRING_BUY
        side == "BUY" -> OrderType.BUY
        side == "SELL" -> OrderType.SELL
        else -> throw IllegalStateException("Unsupported order type")
    }

private fun CustodialOrderResponse.toSwapOrder(inputCurrency: Currency, outputCurrency: Currency): CustodialOrder {
    return CustodialOrder(
        id = this.id,
        state = this.state.toCustodialOrderState(),
        depositAddress = this.kind.depositAddress,
        createdAt = this.createdAt.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
        inputMoney = Money.fromMinor(
            inputCurrency,
            this.priceFunnel.inputMoney.toBigInteger()
        ),
        outputMoney = Money.fromMinor(
            outputCurrency,
            this.priceFunnel.outputMoney.toBigInteger()
        )
    )
}

private fun TransactionResponse.hasCardOrBankFailure() =
    error?.let { error ->
        listOf(
            TransactionResponse.CARD_PAYMENT_ABANDONED,
            TransactionResponse.CARD_PAYMENT_EXPIRED,
            TransactionResponse.CARD_PAYMENT_FAILED,
            TransactionResponse.BANK_TRANSFER_PAYMENT_REJECTED,
            TransactionResponse.BANK_TRANSFER_PAYMENT_EXPIRED
        ).contains(error)
    } ?: false

private fun String.toTransactionType(): TransactionType? =
    when (this) {
        TransactionResponse.DEPOSIT,
        TransactionResponse.CHARGE -> TransactionType.DEPOSIT
        TransactionResponse.WITHDRAWAL -> TransactionType.WITHDRAWAL
        else -> null
    }
