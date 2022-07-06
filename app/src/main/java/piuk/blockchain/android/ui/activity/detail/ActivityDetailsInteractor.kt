package piuk.blockchain.android.ui.activity.detail

import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CustodialInterestActivitySummaryItem
import com.blockchain.coincore.CustodialTradingActivitySummaryItem
import com.blockchain.coincore.CustodialTransferActivitySummaryItem
import com.blockchain.coincore.NonCustodialActivitySummaryItem
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.coincore.RecurringBuyActivitySummaryItem
import com.blockchain.coincore.TradeActivitySummaryItem
import com.blockchain.coincore.bch.BchActivitySummaryItem
import com.blockchain.coincore.btc.BtcActivitySummaryItem
import com.blockchain.coincore.erc20.Erc20ActivitySummaryItem
import com.blockchain.coincore.eth.EthActivitySummaryItem
import com.blockchain.coincore.evm.L1EvmActivitySummaryItem
import com.blockchain.coincore.selectFirstAccount
import com.blockchain.coincore.xlm.XlmActivitySummaryItem
import com.blockchain.core.price.historic.HistoricRateFetcher
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.CardService
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.TransactionType
import com.blockchain.nabu.datamanagers.TransferDirection
import com.blockchain.nabu.datamanagers.custodialwalletimpl.OrderType
import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.CurrencyType
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import info.blockchain.balance.asAssetInfoOrThrow
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.text.ParseException
import java.util.Date
import piuk.blockchain.android.R
import piuk.blockchain.android.domain.repositories.AssetActivityRepository
import piuk.blockchain.android.util.StringUtils

class ActivityDetailsInteractor(
    private val currencyPrefs: CurrencyPrefs,
    private val transactionInputOutputMapper: TransactionInOutMapper,
    private val assetActivityRepository: AssetActivityRepository,
    private val custodialWalletManager: CustodialWalletManager,
    private val bankService: BankService,
    private val cardService: CardService,
    private val stringUtils: StringUtils,
    private val coincore: Coincore,
    private val historicRateFetcher: HistoricRateFetcher,
    private val defaultLabels: DefaultLabels
) {

    fun loadCustodialTradingItems(
        summaryItem: CustodialTradingActivitySummaryItem
    ): Single<List<ActivityDetailsType>> {
        val currentTransactionType = summaryItem.type
        val list = mutableListOf(
            TransactionId(summaryItem.txId),
            Created(Date(summaryItem.timeStampMs)),
            HistoricCryptoPrice(
                summaryItem.price,
                if (currentTransactionType == OrderType.BUY) {
                    summaryItem.asset.displayTicker
                } else {
                    summaryItem.fundedFiat.currencyCode
                }
            ),
            if (currentTransactionType == OrderType.BUY)
                BuyPurchaseAmount(summaryItem.fundedFiat)
            else
                SellPurchaseAmount(summaryItem.fundedFiat),
            if (summaryItem.type == OrderType.BUY)
                BuyCryptoWallet(summaryItem.asset)
            else
                SellCryptoWallet(summaryItem.fundedFiat.currency),
            BuyFee(summaryItem.fee)
        )

        return when (summaryItem.paymentMethodType) {
            PaymentMethodType.PAYMENT_CARD -> cardService.getCardDetails(
                summaryItem.paymentMethodId
            )
                .map { paymentMethod ->
                    addPaymentDetailsToList(list, paymentMethod, summaryItem)
                    list.toList()
                }.onErrorReturn {
                    addPaymentDetailsToList(list, null, summaryItem)
                    list.toList()
                }
            PaymentMethodType.BANK_TRANSFER -> bankService.getLinkedBank(
                summaryItem.paymentMethodId
            ).map {
                it.toPaymentMethod()
            }.map { paymentMethod ->
                addPaymentDetailsToList(list, paymentMethod, summaryItem)
                list.toList()
            }.onErrorReturn {
                addPaymentDetailsToList(list, null, summaryItem)
                list.toList()
            }

            else -> {
                list.add(
                    BuyPaymentMethod(
                        PaymentDetails(
                            paymentMethodId = PaymentMethod.FUNDS_PAYMENT_ID,
                            label = summaryItem.fundedFiat.currencyCode
                        )
                    )
                )
                Single.just(list.toList())
            }
        }
    }

    fun deleteRecurringBuy(id: String) = custodialWalletManager.cancelRecurringBuy(id)

    fun loadRecurringBuyItems(
        cacheTransaction: RecurringBuyActivitySummaryItem,
        recurringBuy: RecurringBuy
    ): Single<List<ActivityDetailsType>> {
        val list = mutableListOf(
            TransactionId(cacheTransaction.txId),
            Created(recurringBuy.createDate),
            TotalCostAmount(cacheTransaction.fundedFiat),
            FeeAmount(Money.zero(cacheTransaction.fee.currency)),
            RecurringBuyFrequency(recurringBuy.recurringBuyFrequency, recurringBuy.nextPaymentDate),
            NextPayment(recurringBuy.nextPaymentDate)
        )
        return when (cacheTransaction.paymentMethodType) {
            PaymentMethodType.PAYMENT_CARD -> cardService.getCardDetails(cacheTransaction.paymentMethodId)
                .map { paymentMethod ->
                    addPaymentDetailsToList(list, paymentMethod, cacheTransaction)
                    list.toList()
                }.onErrorReturn {
                    addPaymentDetailsToList(list, null, cacheTransaction)
                    list.toList()
                }
            PaymentMethodType.BANK_TRANSFER -> bankService.getLinkedBank(cacheTransaction.paymentMethodId)
                .map {
                    it.toPaymentMethod()
                }.map { paymentMethod ->
                    addPaymentDetailsToList(list, paymentMethod, cacheTransaction)
                    list.toList()
                }.onErrorReturn {
                    addPaymentDetailsToList(list, null, cacheTransaction)
                    list.toList()
                }
            else -> {
                list.add(
                    BuyPaymentMethod(
                        PaymentDetails(
                            paymentMethodId = PaymentMethod.FUNDS_PAYMENT_ID,
                            label = cacheTransaction.fundedFiat.currencyCode
                        )
                    )
                )
                Single.just(list.toList())
            }
        }
    }

    fun loadCustodialInterestItems(
        summaryItem: CustodialInterestActivitySummaryItem
    ): Single<List<ActivityDetailsType>> {
        val list = mutableListOf(
            TransactionId(summaryItem.txId),
            Created(Date(summaryItem.timeStampMs))
        )
        when (summaryItem.type) {
            TransactionSummary.TransactionType.DEPOSIT -> {
                list.add(
                    getToField(
                        summaryItem.account.label, summaryItem.account.label, summaryItem.asset
                    )
                )
            }
            TransactionSummary.TransactionType.WITHDRAW -> {
                list.add(From(stringUtils.getString(R.string.common_company_name)))
            }
            TransactionSummary.TransactionType.INTEREST_EARNED -> {
                list.add(From(stringUtils.getString(R.string.common_company_name)))
                list.add(
                    getToField(
                        summaryItem.account.label, summaryItem.account.label, summaryItem.asset
                    )
                )
            }
            else -> {
                // do nothing
            }
        }
        return if (summaryItem.type == TransactionSummary.TransactionType.WITHDRAW) {
            coincore.findAccountByAddress(
                summaryItem.account.currency,
                summaryItem.accountRef
            ).map {
                if (it !is NullCryptoAccount) {
                    list.add(getToField(it.label, it.label, summaryItem.asset))
                } else if (summaryItem.accountRef.isNotBlank()) {
                    list.add(To(summaryItem.accountRef))
                }
                list.toList()
            }.toSingle()
        } else {
            Single.just(list.toList())
        }
    }

    fun loadCustodialTransferItems(
        summaryItem: CustodialTransferActivitySummaryItem
    ): Single<List<ActivityDetailsType>> =
        Single.just(
            listOfNotNull(
                TransactionId(summaryItem.txId),
                Created(Date(summaryItem.timeStampMs)),
                when (summaryItem.type) {
                    TransactionType.DEPOSIT -> {
                        To(summaryItem.account.label)
                    }
                    TransactionType.WITHDRAWAL -> {
                        when {
                            summaryItem.recipientAddress.isBlank() -> {
                                null
                            }
                            else -> {
                                To(summaryItem.recipientAddress)
                            }
                        }
                    }
                    else -> null
                },
                when (summaryItem.type) {
                    TransactionType.DEPOSIT -> {
                        when {
                            summaryItem.recipientAddress.isBlank() -> {
                                null
                            }
                            else -> {
                                From(summaryItem.recipientAddress)
                            }
                        }
                    }
                    TransactionType.WITHDRAWAL -> {
                        From(summaryItem.account.label)
                    }
                    else -> null
                },
                Amount(summaryItem.value),
                NetworkFee(summaryItem.fee)
            )
        )

    fun loadSwapItems(
        item: TradeActivitySummaryItem
    ): Single<List<ActivityDetailsType>> {
        val list = mutableListOf(
            TransactionId(item.txId),
            Created(Date(item.timeStampMs)),
            HistoricCryptoPrice(item.price, item.sendingValue.currencyCode),
            getSwapFromField(item),
            TotalCostAmount(item.fiatValue),
            Amount(item.sendingValue)
        )

        return Single.zip(
            item.depositNetworkFee,
            buildReceivingLabel(item)
        ) { depositFee: Money, toItem: To ->
            list.apply {
                add(NetworkFee(depositFee))
                add(toItem)
                add(SwapReceiveAmount(item.receivingValue))
                add(NetworkFee(item.withdrawalNetworkFee))
            }
            list.toList()
        }
    }

    private fun getSwapFromField(tradeActivity: TradeActivitySummaryItem): From {
        return From(
            "${tradeActivity.currencyPair.source.displayTicker} ${tradeActivity.sendingAccount.label}"
        )
    }

    private fun getSellFromField(tradeActivity: TradeActivitySummaryItem): From {
        return From(
            "${tradeActivity.currencyPair.source.displayTicker} ${tradeActivity.sendingAccount.label}"
        )
    }

    private fun getSellToField(tradeActivity: TradeActivitySummaryItem): To {
        return To("${tradeActivity.currencyPair.destination} ${tradeActivity.sendingAccount.label}")
    }

    fun loadSellItems(
        item: TradeActivitySummaryItem
    ): Single<List<ActivityDetailsType>> {
        return item.depositNetworkFee.map { fee ->
            listOf(
                TransactionId(item.txId),
                Created(Date(item.timeStampMs)),
                HistoricCryptoPrice(item.price, item.sendingValue.currencyCode),
                getSellToField(item),
                getSellFromField(item),
                Amount(item.sendingValue),
                NetworkFee(fee),
                SellPurchaseAmount(item.receivingValue)
            )
        }
    }

    private fun getToField(label: String, defaultLabel: String, asset: Currency): To =
        To(
            if (label.isEmpty() || label == defaultLabel) {
                "${asset.displayTicker} $defaultLabel"
            } else {
                label
            }
        )

    private fun buildReceivingLabel(item: TradeActivitySummaryItem): Single<To> {
        require(
            item.currencyPair.source.type == CurrencyType.CRYPTO &&
                item.currencyPair.destination.type == CurrencyType.CRYPTO
        )
        return when (item.direction) {
            TransferDirection.ON_CHAIN -> coincore.findAccountByAddress(
                item.currencyPair.destination.asAssetInfoOrThrow(), item.receivingAddress!!
            ).toSingle().map {
                val defaultLabel = defaultLabels.getDefaultNonCustodialWalletLabel()
                getToField(it.label, defaultLabel, item.currencyPair.destination)
            }
            TransferDirection.INTERNAL,
            TransferDirection.FROM_USERKEY -> coincore[item.currencyPair.destination.asAssetInfoOrThrow()].accountGroup(
                AssetFilter.Trading
            )
                .toSingle()
                .map {
                    val defaultLabel = it.selectFirstAccount().label
                    getToField(defaultLabel, defaultLabel, item.currencyPair.destination)
                }
            TransferDirection.TO_USERKEY -> throw IllegalStateException("TO_USERKEY swap direction not supported")
        }
    }

    private fun addPaymentDetailsToList(
        list: MutableList<ActivityDetailsType>,
        paymentMethod: PaymentMethod?,
        summaryItem: CustodialTradingActivitySummaryItem
    ) {
        paymentMethod?.let {
            list.add(
                BuyPaymentMethod(
                    PaymentDetails(
                        paymentMethodId = it.id,
                        label = it.label(),
                        endDigits = it.endDigits(),
                        accountType = it.accountType(),
                        paymentMethodType = it.type,
                        mobilePaymentType = (it as? PaymentMethod.Card)?.mobilePaymentType
                    )
                )
            )
        } ?: list.add(
            BuyPaymentMethod(
                PaymentDetails(
                    paymentMethodId = summaryItem.paymentMethodId,
                    label = summaryItem.fundedFiat.currencyCode
                )
            )
        )
    }

    private fun addPaymentDetailsToList(
        list: MutableList<ActivityDetailsType>,
        paymentMethod: PaymentMethod?,
        summaryItem: RecurringBuyActivitySummaryItem
    ) {
        paymentMethod?.let {
            list.add(
                BuyPaymentMethod(
                    PaymentDetails(
                        it.id, it.label(), it.endDigits(), it.accountType(), it.type
                    )
                )
            )
        } ?: list.add(
            BuyPaymentMethod(
                PaymentDetails(
                    paymentMethodId = summaryItem.paymentMethodId,
                    label = summaryItem.fundedFiat.currencyCode
                )
            )
        )
    }

    fun loadRecurringBuysById(recurringBuyId: String) =
        custodialWalletManager.getRecurringBuyForId(recurringBuyId)

    fun getCustodialTradingActivityDetails(
        asset: AssetInfo,
        txHash: String
    ): CustodialTradingActivitySummaryItem? =
        assetActivityRepository.findCachedItem(
            asset,
            txHash
        ) as? CustodialTradingActivitySummaryItem

    fun getCustodialInterestActivityDetails(
        asset: AssetInfo,
        txHash: String
    ): CustodialInterestActivitySummaryItem? =
        assetActivityRepository.findCachedItem(
            asset,
            txHash
        ) as? CustodialInterestActivitySummaryItem

    fun getCustodialTransferActivityDetails(
        asset: AssetInfo,
        txHash: String
    ): CustodialTransferActivitySummaryItem? =
        assetActivityRepository.findCachedItem(
            asset,
            txHash
        ) as? CustodialTransferActivitySummaryItem

    fun getTradeActivityDetails(
        asset: AssetInfo,
        txHash: String
    ): TradeActivitySummaryItem? =
        assetActivityRepository.findCachedTradeItem(asset, txHash)

    fun getRecurringBuyTransactionCacheDetails(
        txHash: String
    ): RecurringBuyActivitySummaryItem? =
        assetActivityRepository.findCachedItemById(
            txHash
        ) as? RecurringBuyActivitySummaryItem

    fun getNonCustodialActivityDetails(
        asset: AssetInfo,
        txHash: String
    ): NonCustodialActivitySummaryItem? =
        assetActivityRepository.findCachedItem(
            asset,
            txHash
        ) as? NonCustodialActivitySummaryItem

    fun loadCreationDate(
        activitySummaryItem: ActivitySummaryItem
    ): Date? = try {
        Date(activitySummaryItem.timeStampMs)
    } catch (e: ParseException) {
        null
    }

    fun loadFeeItems(
        item: NonCustodialActivitySummaryItem
    ) = historicRateFetcher.fetch(item.asset, currencyPrefs.selectedFiatCurrency, item.timeStampMs, item.value)
        .flatMap { fiatValue ->
            getTransactionsMapForFeeItems(item, fiatValue)
        }.onErrorResumeNext {
            getTransactionsMapForFeeItems(item, null)
        }

    private fun getTransactionsMapForFeeItems(
        item: NonCustodialActivitySummaryItem,
        fiatValue: Money?
    ) = transactionInputOutputMapper.transformInputAndOutputs(item).map {
        getListOfItemsForFees(item, fiatValue, it)
    }.onErrorReturn {
        getListOfItemsForFees(item, fiatValue, null)
    }

    private fun getListOfItemsForFees(
        item: NonCustodialActivitySummaryItem,
        fiatValue: Money?,
        transactionInOutDetails: TransactionInOutDetails?
    ) = listOfNotNull(
        Amount(item.value),
        HistoricValue(fiatValue, item.transactionType),
        addSingleOrMultipleFromAddresses(transactionInOutDetails),
        addFeeForTransaction(item),
        checkIfShouldAddMemo(item),
        checkIfShouldAddDescription(item),
        Action()
    )

    fun loadReceivedItems(
        item: NonCustodialActivitySummaryItem
    ) = historicRateFetcher.fetch(item.asset, currencyPrefs.selectedFiatCurrency, item.timeStampMs, item.value)
        .flatMap { fiatValue ->
            getTransactionsMapForReceivedItems(item, fiatValue)
        }.onErrorResumeNext {
            getTransactionsMapForReceivedItems(item, null)
        }

    private fun getTransactionsMapForReceivedItems(
        item: NonCustodialActivitySummaryItem,
        fiatValue: Money?
    ) = transactionInputOutputMapper.transformInputAndOutputs(item).map {
        getListOfItemsForReceives(item, fiatValue, it)
    }.onErrorReturn {
        getListOfItemsForReceives(item, fiatValue, null)
    }

    private fun getListOfItemsForReceives(
        item: NonCustodialActivitySummaryItem,
        fiatValue: Money?,
        transactionInOutDetails: TransactionInOutDetails?
    ) = listOfNotNull(
        TransactionId(item.txId),
        HistoricValue(fiatValue, item.transactionType),
        addSingleOrMultipleToAddresses(transactionInOutDetails),
        addSingleOrMultipleFromAddresses(transactionInOutDetails),
        checkIfShouldAddMemo(item),
        Action()
    )

    fun loadTransferItems(
        item: NonCustodialActivitySummaryItem
    ): Single<List<ActivityDetailsType>> =
        historicRateFetcher.fetch(item.asset, currencyPrefs.selectedFiatCurrency, item.timeStampMs, item.value)
            .flatMap { fiatValue ->
                getTransactionsMapForTransferItems(item, fiatValue)
            }.onErrorResumeNext {
                getTransactionsMapForTransferItems(item, null)
            }

    private fun getTransactionsMapForTransferItems(
        item: NonCustodialActivitySummaryItem,
        fiatValue: Money?
    ) = transactionInputOutputMapper.transformInputAndOutputs(item).map {
        getListOfItemsForTransfers(item, fiatValue, it)
    }.onErrorReturn {
        getListOfItemsForTransfers(item, fiatValue, null)
    }

    private fun getListOfItemsForTransfers(
        item: NonCustodialActivitySummaryItem,
        fiatValue: Money?,
        transactionInOutDetails: TransactionInOutDetails?
    ) = listOfNotNull(
        Amount(item.value),
        HistoricValue(fiatValue, item.transactionType),
        addSingleOrMultipleFromAddresses(transactionInOutDetails),
        addSingleOrMultipleToAddresses(transactionInOutDetails),
        checkIfShouldAddMemo(item),
        checkIfShouldAddDescription(item),
        Action()
    )

    fun loadConfirmedSentItems(
        item: NonCustodialActivitySummaryItem
    ) = item.fee.single(item.value).flatMap { cryptoValue ->
        getTotalFiat(item, cryptoValue, currencyPrefs.selectedFiatCurrency)
    }.onErrorResumeNext {
        getTotalFiat(item, null, currencyPrefs.selectedFiatCurrency)
    }

    private fun getTotalFiat(
        item: NonCustodialActivitySummaryItem,
        value: Money?,
        selectedFiatCurrency: FiatCurrency
    ) = historicRateFetcher.fetch(item.asset, selectedFiatCurrency, item.timeStampMs, item.value).flatMap { fiatValue ->
        getTransactionsMapForConfirmedSentItems(value, fiatValue, item)
    }.onErrorResumeNext {
        getTransactionsMapForConfirmedSentItems(value, null, item)
    }

    private fun getTransactionsMapForConfirmedSentItems(
        cryptoValue: Money?,
        fiatValue: Money?,
        item: NonCustodialActivitySummaryItem
    ) = transactionInputOutputMapper.transformInputAndOutputs(item)
        .map {
            getListOfItemsForConfirmedSends(cryptoValue, fiatValue, item, it)
        }.onErrorReturn {
            getListOfItemsForConfirmedSends(cryptoValue, fiatValue, item, null)
        }

    private fun getListOfItemsForConfirmedSends(
        cryptoValue: Money?,
        fiatValue: Money?,
        item: NonCustodialActivitySummaryItem,
        transactionInOutDetails: TransactionInOutDetails?
    ) = listOfNotNull(
        Amount(item.value),
        Fee(cryptoValue),
        HistoricValue(fiatValue, item.transactionType),
        addSingleOrMultipleFromAddresses(transactionInOutDetails),
        addSingleOrMultipleToAddresses(transactionInOutDetails),
        checkIfShouldAddMemo(item),
        checkIfShouldAddDescription(item),
        Action()
    )

    fun loadUnconfirmedSentItems(
        item: NonCustodialActivitySummaryItem
    ): Single<List<ActivityDetailsType>> = item.fee.singleOrError().flatMap { cryptoValue ->
        getTransactionsMapForUnconfirmedSentItems(item, cryptoValue)
    }.onErrorResumeNext {
        getTransactionsMapForUnconfirmedSentItems(item, null)
    }

    private fun getTransactionsMapForUnconfirmedSentItems(
        item: NonCustodialActivitySummaryItem,
        cryptoValue: Money?
    ) = transactionInputOutputMapper.transformInputAndOutputs(item).map {
        getListOfItemsForUnconfirmedSends(item, cryptoValue, it)
    }.onErrorReturn {
        getListOfItemsForUnconfirmedSends(item, cryptoValue, null)
    }

    private fun getListOfItemsForUnconfirmedSends(
        item: NonCustodialActivitySummaryItem,
        cryptoValue: Money?,
        transactionInOutDetails: TransactionInOutDetails?
    ) = listOfNotNull(
        Amount(item.value),
        Fee(cryptoValue),
        addSingleOrMultipleFromAddresses(transactionInOutDetails),
        addSingleOrMultipleToAddresses(transactionInOutDetails),
        checkIfShouldAddDescription(item),
        Action()
    )

    fun updateItemDescription(
        txId: String,
        asset: AssetInfo,
        description: String
    ): Completable {
        return when (
            val activityItem =
                assetActivityRepository.findCachedItem(asset, txId)
        ) {
            is BtcActivitySummaryItem -> activityItem.updateDescription(description)
            is BchActivitySummaryItem -> activityItem.updateDescription(description)
            is EthActivitySummaryItem -> activityItem.updateDescription(description)
            is Erc20ActivitySummaryItem -> activityItem.updateDescription(description)
            is L1EvmActivitySummaryItem -> activityItem.updateDescription(description)
            is XlmActivitySummaryItem -> activityItem.updateDescription(description)
            else -> {
                Completable.error(
                    UnsupportedOperationException(
                        "This type of currency doesn't support descriptions"
                    )
                )
            }
        }
    }

    private fun addFeeForTransaction(item: NonCustodialActivitySummaryItem): FeeForTransaction? {
        return when (item) {
            is EthActivitySummaryItem -> {
                val relatedItem =
                    assetActivityRepository.findCachedItemById(item.ethTransaction.hash)
                relatedItem?.let {
                    FeeForTransaction(
                        item.transactionType,
                        it.value
                    )
                }
            }
            else -> null
        }
    }

    private fun addSingleOrMultipleFromAddresses(
        it: TransactionInOutDetails?
    ) = From(
        when {
            it == null -> null
            it.inputs.size == 1 -> it.inputs[0].address
            else -> it.inputs.toJoinedString()
        }
    )

    private fun addSingleOrMultipleToAddresses(
        it: TransactionInOutDetails?
    ) = To(
        when {
            it == null -> null
            it.outputs.size == 1 -> it.outputs[0].address
            else -> it.outputs.toJoinedString()
        }
    )

    private fun List<TransactionDetailModel>.toJoinedString(): String =
        this.map { o -> o.address }.joinToString("\n")

    private fun checkIfShouldAddDescription(
        item: NonCustodialActivitySummaryItem
    ): Description? = if (item.supportsDescription) {
        Description(item.description)
    } else {
        null
    }

    private fun checkIfShouldAddMemo(
        item: NonCustodialActivitySummaryItem
    ): XlmMemo? = when (item) {
        is XlmActivitySummaryItem -> if (item.xlmMemo.isNotBlank()) XlmMemo(item.xlmMemo) else null
        else -> null
    }
}

private fun PaymentMethod.endDigits(): String? =
    when (this) {
        is PaymentMethod.Bank -> accountEnding
        is PaymentMethod.Card -> endDigits
        else -> null
    }

private fun PaymentMethod.label(): String? =
    when (this) {
        is PaymentMethod.Bank -> bankName
        is PaymentMethod.Card -> uiLabel()
        else -> null
    }

private fun PaymentMethod.accountType(): String? =
    when (this) {
        is PaymentMethod.Bank -> uiAccountType
        else -> null
    }
