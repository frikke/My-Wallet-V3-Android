package com.blockchain.coincore

import com.blockchain.coincore.impl.txEngine.PricedQuote
import com.blockchain.core.chains.EvmNetwork
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.Money

data class FeeInfo(
    val feeAmount: Money,
    val fiatAmount: Money,
    val asset: Currency,
    val l1EvmNetwork: EvmNetwork? = null
)

sealed class TxConfirmationValue(open val confirmation: TxConfirmation) {

    data class ExchangePriceConfirmation(val money: Money, val asset: Currency) :
        TxConfirmationValue(TxConfirmation.EXPANDABLE_SIMPLE_READ_ONLY)

    data class From(val sourceAccount: BlockchainAccount, val sourceAsset: Currency? = null) :
        TxConfirmationValue(TxConfirmation.SIMPLE_READ_ONLY)

    data class PaymentMethod(
        val paymentTitle: String,
        val paymentSubtitle: String,
        val accountType: String?,
        val assetAction: AssetAction
    ) : TxConfirmationValue(TxConfirmation.COMPLEX_READ_ONLY)

    data class DAppInfo(
        val name: String,
        val url: String,
    ) : TxConfirmationValue(TxConfirmation.COMPLEX_READ_ONLY)

    data class Chain(val assetInfo: AssetInfo) :
        TxConfirmationValue(TxConfirmation.SIMPLE_READ_ONLY)

    data class SignEthMessage(val message: String, val dAppName: String) :
        TxConfirmationValue(TxConfirmation.EXPANDABLE_SINGLE_VALUE_READ_ONLY)

    data class Sale(val amount: Money, val exchange: Money) :
        TxConfirmationValue(TxConfirmation.COMPLEX_READ_ONLY)

    data class To(
        val txTarget: TransactionTarget,
        val assetAction: AssetAction,
        val sourceAccount: BlockchainAccount? = null
    ) : TxConfirmationValue(TxConfirmation.SIMPLE_READ_ONLY)

    data class ToWithNameAndAddress(
        val address: String,
        val label: String
    ) : TxConfirmationValue(TxConfirmation.COMPLEX_ELLIPSIZED_READ_ONLY)

    data class Total(val totalWithFee: Money, val exchange: Money) :
        TxConfirmationValue(TxConfirmation.COMPLEX_READ_ONLY)

    data class Amount(val amount: Money, val isImportant: Boolean) :
        TxConfirmationValue(TxConfirmation.SIMPLE_READ_ONLY)

    object EstimatedCompletion : TxConfirmationValue(TxConfirmation.SIMPLE_READ_ONLY)

    data class BitPayCountdown(
        val timeRemainingSecs: Long
    ) : TxConfirmationValue(TxConfirmation.INVOICE_COUNTDOWN)

    data class ErrorNotice(val status: ValidationState, val money: Money? = null) :
        TxConfirmationValue(TxConfirmation.ERROR_NOTICE)

    data class Description(val text: String = "") : TxConfirmationValue(TxConfirmation.DESCRIPTION), UserEditable

    data class Memo(val text: String?, val isRequired: Boolean = false, val id: Long?, val editable: Boolean = true) :
        TxConfirmationValue(TxConfirmation.MEMO), UserEditable

    data class NetworkFee(
        val feeAmount: Money,
        val exchange: Money,
        val asset: AssetInfo
    ) : TxConfirmationValue(TxConfirmation.EXPANDABLE_COMPLEX_READ_ONLY)

    data class TransactionFee(
        val feeAmount: Money
    ) : TxConfirmationValue(TxConfirmation.SIMPLE_READ_ONLY)

    data class CompoundNetworkFee(
        val sendingFeeInfo: FeeInfo? = null,
        val receivingFeeInfo: FeeInfo? = null,
        val feeLevel: FeeLevel? = null,
        val ignoreErc20LinkedNote: Boolean = false
    ) : TxConfirmationValue(TxConfirmation.COMPOUND_EXPANDABLE_READ_ONLY)

    data class SwapExchange(
        val unitCryptoCurrency: Money,
        val price: Money,
        val isNewQuote: Boolean
    ) : TxConfirmationValue(TxConfirmation.EXPANDABLE_COMPLEX_READ_ONLY)

    data class TxBooleanConfirmation<T>(
        override val confirmation: TxConfirmation,
        val data: T? = null,
        val value: Boolean = false
    ) : TxConfirmationValue(confirmation), UserEditable

    data class WalletConnectHeader(val dAppName: String, val dAppLogo: String, val dAppUrl: String) :
        TxConfirmationValue(TxConfirmation.HEADER)

    data class QuoteCountDown(
        val pricedQuote: PricedQuote
    ) : TxConfirmationValue(TxConfirmation.QUOTE_COUNTDOWN)
}

interface UserEditable
