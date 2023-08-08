package piuk.blockchain.android.ui.transactionflow.flow

import android.content.Context
import android.text.SpannableStringBuilder
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.FeeInfo
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.isLayer2Token
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionFlowCustomiserImpl
import piuk.blockchain.android.urllinks.CHECKOUT_PRICE_EXPLANATION
import piuk.blockchain.android.urllinks.EXCHANGE_SWAP_RATE_EXPLANATION
import piuk.blockchain.android.urllinks.NETWORK_ERC20_EXPLANATION
import piuk.blockchain.android.urllinks.NETWORK_FEE_EXPLANATION
import piuk.blockchain.android.util.StringUtils

class TxConfirmReadOnlyMapperCheckout(
    private val formatters: List<TxOptionsFormatterCheckout>
) {
    fun map(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        return when (property) {
            is TxConfirmationValue.To -> formatters.first { it is ToPropertyFormatter }.format(property)
            is TxConfirmationValue.ToWithNameAndAddress -> formatters.first { it is ToWithNameAndAddressFormatter }
                .format(property)

            is TxConfirmationValue.From -> formatters.first { it is FromPropertyFormatter }.format(property)
            is TxConfirmationValue.ExchangePriceConfirmation ->
                formatters.first { it is ExchangePriceFormatter }.format(property)

            is TxConfirmationValue.NetworkFee -> formatters.first { it is NetworkFormatter }.format(property)
            is TxConfirmationValue.TransactionFee -> formatters.first { it is TransactionFeeFormatter }.format(property)
            is TxConfirmationValue.ProcessingFee -> formatters.first { it is ProcessingFeeFormatter }.format(property)
            is TxConfirmationValue.Sale -> formatters.first { it is SalePropertyFormatter }.format(property)
            is TxConfirmationValue.Total -> formatters.first { it is TotalFormatter }.format(property)
            is TxConfirmationValue.Amount -> formatters.first { it is AmountFormatter }.format(property)
            is TxConfirmationValue.EstimatedCompletion ->
                formatters.first { it is EstimatedCompletionPropertyFormatter }
                    .format(property)

            is TxConfirmationValue.PaymentMethod ->
                formatters.first { it is PaymentMethodPropertyFormatter }.format(property)

            is TxConfirmationValue.DAppInfo ->
                formatters.first { it is DAppInfoPropertyFormatter }.format(property)

            is TxConfirmationValue.Chain ->
                formatters.first { it is ChainPropertyFormatter }.format(property)

            is TxConfirmationValue.SwapExchange ->
                formatters.first { it is SwapExchangeRateFormatter }.format(property)

            is TxConfirmationValue.CompoundNetworkFee -> formatters.first { it is CompoundNetworkFeeFormatter }
                .format(property)

            is TxConfirmationValue.SignEthMessage -> formatters.first { it is SignEthMessagePropertyFormatter }
                .format(property)

            is TxConfirmationValue.AvailableToTrade -> formatters.first { it is AvailableToTradePropertyFormatter }
                .format(property)

            is TxConfirmationValue.AvailableToWithdraw ->
                formatters.first { it is AvailableToWithdrawPropertyFormatter }.format(property)

            is TxConfirmationValue.AchTermsAndConditions ->
                formatters.first { it is ReadMoreDisclaimerPropertyFormatter }.format(property)

            else -> throw IllegalStateException("No formatter found for property: $property")
        }
    }
}

interface TxOptionsFormatterCheckout {
    fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any>
}

enum class ConfirmationPropertyKey {
    LABEL,
    TITLE,
    SUBTITLE,
    LINKED_NOTE,
    IS_IMPORTANT,
    FEE_ITEM_SENDING,
    FEE_ITEM_RECEIVING,
    CTA
}

class ExchangePriceFormatter(
    private val context: Context
) : TxOptionsFormatterCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.ExchangePriceConfirmation)
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(
                com.blockchain.stringResources.R.string.quote_price,
                property.asset.displayTicker
            ),
            ConfirmationPropertyKey.TITLE to property.money.toStringWithSymbol(),
            ConfirmationPropertyKey.LINKED_NOTE to StringUtils.getResolvedStringWithAppendedMappedLearnMore(
                context.resources.getString(com.blockchain.stringResources.R.string.checkout_item_price_note),
                com.blockchain.stringResources.R.string.common_linked_learn_more,
                CHECKOUT_PRICE_EXPLANATION,
                context,
                com.blockchain.common.R.color.blue_600
            )
        )
    }
}

class ToPropertyFormatter(
    private val context: Context,
    private val defaultLabel: DefaultLabels
) : TxOptionsFormatterCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.To)
        return if (property.assetAction == AssetAction.Sell || property.assetAction == AssetAction.FiatDeposit) {
            mapOf(
                ConfirmationPropertyKey.LABEL to context.resources.getString(
                    com.blockchain.stringResources.R.string.checkout_item_deposit_to
                ),
                ConfirmationPropertyKey.TITLE to property.txTarget.label
            )
        } else {
            require(property.sourceAccount is CryptoAccount)
            val asset = (property.sourceAccount as CryptoAccount).currency
            mapOf(
                ConfirmationPropertyKey.LABEL to context.resources.getString(
                    com.blockchain.stringResources.R.string.checkout_item_send_to
                ),
                ConfirmationPropertyKey.TITLE to getLabelForTarget(
                    property.txTarget,
                    defaultLabel.getDefaultNonCustodialWalletLabel(),
                    asset.displayTicker
                )
            )
        }
    }
}

class ToWithNameAndAddressFormatter(
    private val context: Context
) : TxOptionsFormatterCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.ToWithNameAndAddress)
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(
                com.blockchain.stringResources.R.string.common_to
            ),
            ConfirmationPropertyKey.TITLE to property.label,
            ConfirmationPropertyKey.SUBTITLE to property.address
        )
    }
}

class EstimatedCompletionPropertyFormatter(
    private val context: Context
) : TxOptionsFormatterCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(
                com.blockchain.stringResources.R.string.send_confirmation_eta
            ),
            ConfirmationPropertyKey.TITLE to TransactionFlowCustomiserImpl.getEstimatedTransactionCompletionTime()
        )
    }
}

class SalePropertyFormatter(
    private val context: Context
) : TxOptionsFormatterCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.Sale)
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(
                com.blockchain.stringResources.R.string.checkout_item_sale
            ),
            ConfirmationPropertyKey.TITLE to property.exchange.toStringWithSymbol(),
            ConfirmationPropertyKey.SUBTITLE to property.amount.toStringWithSymbol()
        )
    }
}

class PaymentMethodPropertyFormatter(
    private val context: Context
) : TxOptionsFormatterCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.PaymentMethod)
        return mapOf(
            if (property.assetAction == AssetAction.FiatDeposit) {
                ConfirmationPropertyKey.LABEL to context.resources.getString(
                    com.blockchain.stringResources.R.string.payment_method
                )
            } else {
                ConfirmationPropertyKey.LABEL to context.resources.getString(
                    com.blockchain.stringResources.R.string.checkout_item_withdraw_to
                )
            },
            ConfirmationPropertyKey.TITLE to property.paymentTitle,
            if (property.accountType.isNullOrBlank()) {
                ConfirmationPropertyKey.SUBTITLE to context.getString(
                    com.blockchain.stringResources.R.string.checkout_item_account_number,
                    property.paymentSubtitle
                )
            } else {
                ConfirmationPropertyKey.SUBTITLE to property.paymentSubtitle
            }
        )
    }
}

class DAppInfoPropertyFormatter(
    private val context: Context
) : TxOptionsFormatterCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.DAppInfo)
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(com.blockchain.stringResources.R.string.app),
            ConfirmationPropertyKey.TITLE to property.name,
            ConfirmationPropertyKey.SUBTITLE to property.url
        )
    }
}

class ChainPropertyFormatter(
    private val context: Context
) : TxOptionsFormatterCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.Chain)
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(
                com.blockchain.stringResources.R.string.common_chain
            ),
            ConfirmationPropertyKey.TITLE to property.assetInfo.name
        )
    }
}

class SignEthMessagePropertyFormatter(
    private val context: Context
) : TxOptionsFormatterCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.SignEthMessage)
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(
                com.blockchain.stringResources.R.string.message_from_dapp,
                property.dAppName
            ),
            ConfirmationPropertyKey.TITLE to property.message
        )
    }
}

class FromPropertyFormatter(
    private val context: Context,
    private val defaultLabel: DefaultLabels
) : TxOptionsFormatterCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.From)
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(
                com.blockchain.stringResources.R.string.common_from
            ),
            property.sourceAsset?.let {
                ConfirmationPropertyKey.TITLE to getLabel(
                    property.sourceAccount.label,
                    defaultLabel.getDefaultNonCustodialWalletLabel(),
                    it.displayTicker
                )
            } ?: ConfirmationPropertyKey.TITLE to property.sourceAccount.label
        )
    }
}

class TransactionFeeFormatter(
    private val context: Context
) : TxOptionsFormatterCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.TransactionFee)
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(
                com.blockchain.stringResources.R.string.checkout_item_fee_to
            ),
            ConfirmationPropertyKey.TITLE to property.feeAmount.toStringWithSymbol()
        )
    }
}

class NetworkFormatter(
    private val context: Context
) : TxOptionsFormatterCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.NetworkFee)
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(
                com.blockchain.stringResources.R.string.checkout_item_network_fee,
                property.asset.displayTicker
            ),
            ConfirmationPropertyKey.TITLE to property.exchange.toStringWithSymbol(),
            ConfirmationPropertyKey.SUBTITLE to property.feeAmount.toStringWithSymbol(),
            if (property.asset.isLayer2Token) {
                ConfirmationPropertyKey.LINKED_NOTE to StringUtils.getResolvedStringWithAppendedMappedLearnMore(
                    context.resources.getString(com.blockchain.stringResources.R.string.swap_erc_20_tooltip),
                    com.blockchain.stringResources.R.string.common_linked_learn_more,
                    NETWORK_ERC20_EXPLANATION,
                    context,
                    com.blockchain.common.R.color.blue_600
                )
            } else {
                ConfirmationPropertyKey.LINKED_NOTE to StringUtils.getResolvedStringWithAppendedMappedLearnMore(
                    context.resources.getString(
                        com.blockchain.stringResources.R.string.checkout_item_network_fee_note,
                        property.asset.name
                    ),
                    com.blockchain.stringResources.R.string.common_linked_learn_more,
                    NETWORK_FEE_EXPLANATION,
                    context,
                    com.blockchain.common.R.color.blue_600
                )
            }
        )
    }
}

class CompoundNetworkFeeFormatter(
    private val context: Context
) : TxOptionsFormatterCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.CompoundNetworkFee)
        return mapOf(
            ConfirmationPropertyKey.LABEL to buildFeeLabel(property.feeLevel),
            ConfirmationPropertyKey.TITLE to getEstimatedFee(property.sendingFeeInfo, property.receivingFeeInfo),
            ConfirmationPropertyKey.FEE_ITEM_SENDING to property.sendingFeeInfo,
            ConfirmationPropertyKey.FEE_ITEM_RECEIVING to property.receivingFeeInfo,
            ConfirmationPropertyKey.LINKED_NOTE to buildLinkedNoteItem(
                property.sendingFeeInfo,
                property.receivingFeeInfo,
                property.ignoreErc20LinkedNote
            )
        ).filterNotNullValues()
    }

    private fun buildFeeLabel(feeLevel: FeeLevel?): String = when (feeLevel) {
        FeeLevel.Regular -> context.resources.getString(
            com.blockchain.stringResources.R.string.checkout_item_network_fee_level_label,
            context.resources.getString(com.blockchain.stringResources.R.string.fee_options_regular)
        )

        FeeLevel.Priority -> context.resources.getString(
            com.blockchain.stringResources.R.string.checkout_item_network_fee_level_label,
            context.resources.getString(com.blockchain.stringResources.R.string.fee_options_priority)
        )

        FeeLevel.Custom -> context.resources.getString(
            com.blockchain.stringResources.R.string.checkout_item_network_fee_level_label,
            context.resources.getString(com.blockchain.stringResources.R.string.fee_options_custom)
        )

        FeeLevel.None, null -> context.resources.getString(
            com.blockchain.stringResources.R.string.checkout_item_network_fee_label
        )
    }

    private fun buildLinkedNoteItem(
        sendingFeeInfo: FeeInfo?,
        receivingFeeInfo: FeeInfo?,
        ignoreErc20LinkedNote: Boolean
    ): SpannableStringBuilder? {
        return when {
            sendingFeeInfo != null && receivingFeeInfo != null -> getDoubleFeeNote(sendingFeeInfo, receivingFeeInfo)
            sendingFeeInfo != null && receivingFeeInfo == null -> getSingleFeeNote(
                sendingFeeInfo,
                ignoreErc20LinkedNote
            )

            sendingFeeInfo == null && receivingFeeInfo != null -> getSingleFeeNote(
                receivingFeeInfo,
                ignoreErc20LinkedNote
            )

            else -> {
                SpannableStringBuilder().append(
                    context.resources.getString(com.blockchain.stringResources.R.string.checkout_fee_free)
                )
            }
        }
    }

    private fun getDoubleFeeNote(
        sendingFeeInfo: FeeInfo,
        receivingFeeInfo: FeeInfo
    ): SpannableStringBuilder? =
        when {
            !sendingFeeInfo.asset.isLayer2Token &&
                !receivingFeeInfo.asset.isLayer2Token -> {
                StringUtils.getResolvedStringWithAppendedMappedLearnMore(
                    context.resources.getString(
                        com.blockchain.stringResources.R.string.checkout_dual_fee_note,
                        sendingFeeInfo.asset.name,
                        receivingFeeInfo.asset.name
                    ),
                    com.blockchain.stringResources.R.string.checkout_fee_link,
                    NETWORK_FEE_EXPLANATION,
                    context,
                    com.blockchain.common.R.color.blue_600
                )
            }

            sendingFeeInfo.asset.isLayer2Token &&
                !receivingFeeInfo.asset.isLayer2Token -> {
                StringUtils.getResolvedStringWithAppendedMappedLearnMore(
                    context.resources.getString(
                        com.blockchain.stringResources.R.string.checkout_one_erc_20_one_not_fee_note,
                        sendingFeeInfo.asset.name,
                        CryptoCurrency.ETHER.name,
                        receivingFeeInfo.asset.name
                    ),
                    com.blockchain.stringResources.R.string.checkout_fee_link,
                    NETWORK_ERC20_EXPLANATION,
                    context,
                    com.blockchain.common.R.color.blue_600
                )
            }

            !sendingFeeInfo.asset.isLayer2Token &&
                receivingFeeInfo.asset.isLayer2Token -> {
                StringUtils.getResolvedStringWithAppendedMappedLearnMore(
                    context.resources.getString(
                        com.blockchain.stringResources.R.string.checkout_one_erc_20_one_not_fee_note,
                        receivingFeeInfo.asset.name,
                        CryptoCurrency.ETHER.name,
                        sendingFeeInfo.asset.name
                    ),
                    com.blockchain.stringResources.R.string.checkout_fee_link,
                    NETWORK_ERC20_EXPLANATION,
                    context,
                    com.blockchain.common.R.color.blue_600
                )
            }

            sendingFeeInfo.asset.isLayer2Token &&
                receivingFeeInfo.asset.isLayer2Token -> {
                StringUtils.getResolvedStringWithAppendedMappedLearnMore(
                    context.resources.getString(
                        com.blockchain.stringResources.R.string.checkout_both_erc_20_fee_note,
                        sendingFeeInfo.asset.name,
                        sendingFeeInfo.asset.name
                    ),
                    com.blockchain.stringResources.R.string.checkout_fee_link,
                    NETWORK_ERC20_EXPLANATION,
                    context,
                    com.blockchain.common.R.color.blue_600
                )
            }

            else -> {
                null
            }
        }

    private fun getSingleFeeNote(
        item: FeeInfo,
        ignoreErc20LinkedNote: Boolean
    ): SpannableStringBuilder =
        when {
            !item.asset.isLayer2Token || ignoreErc20LinkedNote -> {
                val networkName = item.l1EvmNetwork?.name ?: item.asset.name
                StringUtils.getResolvedStringWithAppendedMappedLearnMore(
                    context.resources.getString(
                        com.blockchain.stringResources.R.string.checkout_one_fee_note,
                        networkName
                    ),
                    com.blockchain.stringResources.R.string.checkout_fee_link,
                    NETWORK_FEE_EXPLANATION,
                    context,
                    com.blockchain.common.R.color.blue_600
                )
            }

            else -> {
                val networkName = item.l1EvmNetwork?.name ?: item.asset.name
                StringUtils.getResolvedStringWithAppendedMappedLearnMore(
                    context.resources.getString(
                        com.blockchain.stringResources.R.string.checkout_one_erc_20_fee_note,
                        item.asset.name,
                        networkName
                    ),
                    com.blockchain.stringResources.R.string.checkout_fee_link,
                    NETWORK_ERC20_EXPLANATION,
                    context,
                    com.blockchain.common.R.color.blue_600
                )
            }
        }

    @Suppress("UNCHECKED_CAST")
    private fun Map<ConfirmationPropertyKey, Any?>.filterNotNullValues(): Map<ConfirmationPropertyKey, Any> =
        this.filterValues { it != null } as Map<ConfirmationPropertyKey, Any>

    private fun getEstimatedFee(sendingFeeInfo: FeeInfo?, receivingFeeInfo: FeeInfo?): String =
        when {
            sendingFeeInfo != null && receivingFeeInfo != null -> {
                val addedFees = sendingFeeInfo.fiatAmount.plus(receivingFeeInfo.fiatAmount)
                context.getString(
                    com.blockchain.stringResources.R.string.checkout_item_network_fee_estimate,
                    addedFees.toStringWithSymbol()
                )
            }

            sendingFeeInfo != null && receivingFeeInfo == null -> {
                context.getString(
                    com.blockchain.stringResources.R.string.checkout_item_network_fee_estimate,
                    sendingFeeInfo.fiatAmount.toStringWithSymbol()
                )
            }

            sendingFeeInfo == null && receivingFeeInfo != null -> {
                context.getString(
                    com.blockchain.stringResources.R.string.checkout_item_network_fee_estimate,
                    receivingFeeInfo.fiatAmount.toStringWithSymbol()
                )
            }

            else -> {
                context.resources.getString(com.blockchain.stringResources.R.string.common_free)
            }
        }
}

class SwapExchangeRateFormatter(private val context: Context) :
    TxOptionsFormatterCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.SwapExchange)
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(
                com.blockchain.stringResources.R.string.exchange_rate
            ),
            ConfirmationPropertyKey.TITLE to property.unitCryptoCurrency.toStringWithSymbol(),
            ConfirmationPropertyKey.SUBTITLE to property.price.toStringWithSymbol(),
            ConfirmationPropertyKey.LINKED_NOTE to StringUtils.getResolvedStringWithAppendedMappedLearnMore(
                context.resources.getString(
                    com.blockchain.stringResources.R.string.checkout_swap_exchange_note,
                    property.price.symbol,
                    property.unitCryptoCurrency.symbol
                ),
                com.blockchain.stringResources.R.string.common_linked_learn_more,
                EXCHANGE_SWAP_RATE_EXPLANATION,
                context,
                com.blockchain.common.R.color.blue_600
            )
        )
    }
}

class TotalFormatter(private val context: Context) : TxOptionsFormatterCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.Total)
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(
                com.blockchain.stringResources.R.string.common_total
            ),
            ConfirmationPropertyKey.TITLE to property.exchange.toStringWithSymbol(),
            ConfirmationPropertyKey.SUBTITLE to property.totalWithFee.toStringWithSymbol(),
            ConfirmationPropertyKey.IS_IMPORTANT to true
        )
    }
}

class ProcessingFeeFormatter(private val context: Context) : TxOptionsFormatterCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.ProcessingFee)
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(
                com.blockchain.stringResources.R.string.processing_fee
            ),
            ConfirmationPropertyKey.TITLE to "~${property.exchangeFee.toStringWithSymbol()}",
            ConfirmationPropertyKey.SUBTITLE to property.feeAmount.toStringWithSymbol(),
            ConfirmationPropertyKey.IS_IMPORTANT to false
        )
    }
}

class AmountFormatter(private val context: Context) : TxOptionsFormatterCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.Amount)
        return mapOf(
            if (property.isImportant) {
                ConfirmationPropertyKey.LABEL to context.resources.getString(
                    com.blockchain.stringResources.R.string.common_total
                )
            } else {
                ConfirmationPropertyKey.LABEL to context.resources.getString(
                    com.blockchain.stringResources.R.string.amount
                )
            },
            ConfirmationPropertyKey.TITLE to property.amount.toStringWithSymbol(),
            ConfirmationPropertyKey.IS_IMPORTANT to property.isImportant
        )
    }
}

class AvailableToTradePropertyFormatter(
    private val context: Context
) : TxOptionsFormatterCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.AvailableToTrade)
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(
                com.blockchain.stringResources.R.string.available_to_trade_checkout
            ),
            ConfirmationPropertyKey.TITLE to property.value
        )
    }
}

class AvailableToWithdrawPropertyFormatter(
    private val context: Context
) : TxOptionsFormatterCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.AvailableToWithdraw)
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(
                com.blockchain.stringResources.R.string.available_to_withdraw_checkout
            ),
            ConfirmationPropertyKey.TITLE to property.value
        )
    }
}

class ReadMoreDisclaimerPropertyFormatter(
    private val context: Context
) : TxOptionsFormatterCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.AchTermsAndConditions)
        return mapOf(
            ConfirmationPropertyKey.CTA to context.resources.getString(
                com.blockchain.stringResources.R.string.coinview_expandable_button
            ),
            ConfirmationPropertyKey.LABEL to property.value
        )
    }
}

fun getLabel(label: String, defaultLabel: String, displayTicker: String): String =
    if (label.isEmpty() || label == defaultLabel) {
        "$displayTicker $defaultLabel"
    } else {
        label
    }

fun getLabelForTarget(target: TransactionTarget, defaultLabel: String, displayTicker: String): String =
    when {
        target is CryptoAddress && target.isDomain -> target.getLabelForDomain()
        else -> getLabel(target.label, defaultLabel, displayTicker)
    }

fun CryptoAddress.getLabelForDomain(): String {
    val numOfCharsToShow = 4
    return "$label (${address.take(numOfCharsToShow)}...${address.takeLast(numOfCharsToShow)})"
}
