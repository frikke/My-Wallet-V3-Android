package piuk.blockchain.android.ui.transactionflow.analytics

import com.blockchain.analytics.AnalyticsEvent
import com.blockchain.analytics.events.AnalyticsNames
import com.blockchain.analytics.events.LaunchOrigin
import info.blockchain.balance.Currency
import info.blockchain.balance.CurrencyType
import info.blockchain.balance.Money
import java.io.Serializable
import java.math.BigDecimal
import piuk.blockchain.android.simplebuy.AmountType
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics.Companion.constructMap

sealed class SwapAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {

    object VerifyNowClicked : SwapAnalyticsEvents("swap_kyc_verify_clicked")
    object TrendingPairClicked : SwapAnalyticsEvents("swap_suggested_pair_clicked")
    object NewSwapClicked : SwapAnalyticsEvents("swap_new_clicked")
    object FromPickerSeen : SwapAnalyticsEvents("swap_from_picker_seen")
    object FromAccountSelected : SwapAnalyticsEvents("swap_from_account_clicked")
    object ToPickerSeen : SwapAnalyticsEvents("swap_to_picker_seen")
    object SwapTargetAddressSheet : SwapAnalyticsEvents("swap_pair_locked_seen")
    object SwapEnterAmount : SwapAnalyticsEvents("swap_amount_screen_seen")
    object SwapConfirmSeen : SwapAnalyticsEvents("swap_checkout_shown")
    object SwapSilverLimitSheet : SwapAnalyticsEvents("swap_silver_limit_screen_seen")
    object SwapSilverLimitSheetCta : SwapAnalyticsEvents("swap_silver_limit_upgrade_click")
    object CancelTransaction : SwapAnalyticsEvents("swap_checkout_cancel")
    object SwapTabItemClick : SwapAnalyticsEvents("swap_tab_item_click")
    data class SwapConfirmPair(
        val asset: Currency,
        val target: String
    ) : SwapAnalyticsEvents("swap_pair_locked_confirm", params = constructMap(asset = asset, target = target))

    data class EnterAmountCtaClick(
        val source: Currency,
        val target: String
    ) : SwapAnalyticsEvents("swap_amount_screen_confirm", params = constructMap(source, target))

    data class SwapConfirmCta(
        val source: Currency,
        val target: String
    ) : SwapAnalyticsEvents("swap_checkout_confirm", params = constructMap(source, target))

    data class TransactionSuccess(
        val asset: Currency,
        val source: String,
        val target: String
    ) : SwapAnalyticsEvents(
        "swap_checkout_success",
        params = constructMap(
            asset = asset,
            target = target,
            source = source
        )
    )

    data class TransactionFailed(
        val asset: Currency,
        val target: String?,
        val source: String?,
        val error: String
    ) : SwapAnalyticsEvents(
        "swap_checkout_error",
        params = constructMap(
            asset = asset,
            target = target,
            source = source,
            error = error
        )
    )

    object SwapViewedEvent :
        SwapAnalyticsEvents(AnalyticsNames.SWAP_FROM_WALLET_PAGE_VIEWED.eventName, params = emptyMap())

    class SwapMaxAmountClicked(
        private val sourceCurrency: String,
        private val sourceAccountType: TxFlowAnalyticsAccountType,
        private val targetCurrency: String,
        private val targetAccountType: TxFlowAnalyticsAccountType
    ) : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.SWAP_MAX_CLICKED.eventName
        override val params: Map<String, Serializable>
            get() = mapOf(
                "amount_currency" to sourceCurrency,
                "input_currency" to sourceCurrency,
                "input_type" to sourceAccountType.name,
                "output_currency" to targetCurrency,
                "output_type" to targetAccountType.name
            )
    }

    class SwapFromSelected(
        private val currency: String,
        private val accountType: TxFlowAnalyticsAccountType
    ) : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.SWAP_FROM_WALLET_PAGE_CLICKED.eventName
        override val params: Map<String, Serializable>
            get() = mapOf(
                "input_currency" to currency,
                "input_type" to accountType.name
            )
    }

    class SwapAmountEntered(
        private val amount: Money,
        private val inputAccountType: TxFlowAnalyticsAccountType,
        private val outputCurrency: String,
        private val outputAccountType: TxFlowAnalyticsAccountType
    ) : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.SWAP_AMOUNT_SCREEN_NEXT_CLICKED.eventName
        override val params: Map<String, Serializable>
            get() = mapOf(
                "input_amount" to amount.toBigDecimal(),
                "input_currency" to amount.currencyCode,
                "input_type" to inputAccountType.name,
                "output_currency" to outputCurrency,
                "output_type" to outputAccountType.name
            )
    }

    class SwapAccountsSelected(
        private val inputCurrency: Currency,
        private val outputCurrency: Currency,
        private val sourceAccountType: TxFlowAnalyticsAccountType,
        private val targetAccountType: TxFlowAnalyticsAccountType,
        private val werePreselected: Boolean
    ) : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.SWAP_ACCOUNTS_SELECTED.eventName
        override val params: Map<String, Serializable>
            get() = mapOf(
                "input_currency" to inputCurrency.networkTicker,
                "input_type" to sourceAccountType.name,
                "output_type" to targetAccountType.name,
                "was_suggested" to werePreselected,
                "output_currency" to outputCurrency.networkTicker
            )
    }

    class SwapTargetAccountSelected(private val currency: String, private val account: TxFlowAnalyticsAccountType) :
        AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.SWAP_RECEIVE_WALLET_PAGE_CLICKED.eventName
        override val params: Map<String, Serializable>
            get() = mapOf(
                "output_currency" to currency,
                "output_type" to account.name
            )
    }

    class OnChainSwapRequested(
        private val exchangeRate: BigDecimal,
        private val amount: Money,
        private val inputNetworkFee: Money,
        private val outputNetworkFee: Money,
        private val outputAmount: Money
    ) : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.SWAP_REQUESTED.eventName
        override val params: Map<String, Serializable>
            get() = mapOf(
                "exchange_rate" to exchangeRate,
                "input_amount" to amount.toBigDecimal(),
                "input_currency" to amount.currencyCode,
                "input_type" to TxFlowAnalyticsAccountType.USERKEY.name,
                "output_type" to TxFlowAnalyticsAccountType.USERKEY.name,
                "network_fee_input_amount" to inputNetworkFee.toBigDecimal(),
                "network_fee_input_currency" to inputNetworkFee.currencyCode,
                "network_fee_output_amount" to outputNetworkFee.toBigDecimal(),
                "network_fee_output_currency" to outputNetworkFee.currencyCode,
                "output_amount" to outputAmount.toBigDecimal(),
                "output_currency" to outputAmount.currencyCode
            )
    }

    class SwapClickedEvent(override val origin: LaunchOrigin) :
        AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.SWAP_CLICKED.eventName
        override val params: Map<String, Serializable>
            get() = mapOf()
    }

    object SwapFromWalletPageViewedEvent : AnalyticsEvent {
        override val event: String = AnalyticsNames.SWAP_FROM_WALLET_PAGE_VIEWED.eventName
        override val params: Map<String, Serializable> = emptyMap()
    }

    class SwapFiatCryptoSwitcherClickedEvent(private val newInput: Currency) : AnalyticsEvent {
        override val event: String = AnalyticsNames.SWAP_FIAT_CRYPTO_CLICKED.eventName
        override val params: Map<String, String> = mapOf(
            "switch_to" to if (newInput.type == CurrencyType.FIAT) "FIAT" else "CRYPTO"
        )
    }

    object SwapAmountBackScreenClickedEvent : AnalyticsEvent {
        override val event: String = AnalyticsNames.SWAP_AMOUNT_SCREEN_BACK_CLICKED.eventName
        override val params: Map<String, Serializable> = emptyMap()
    }

    class SwapQuickFillButtonClicked(
        amount: String,
        amountType: AmountType,
        currency: String
    ) : AnalyticsEvent {
        override val event: String = AnalyticsNames.SWAP_QUICK_FILL_CLICKED.eventName
        override val params: Map<String, String> = mapOf(
            "action" to "BUY",
            "amount" to amount,
            "amount_type" to amountType.name,
            "currency" to currency
        )
    }

    object SwapCheckoutScreenViewedEvent : AnalyticsEvent {
        override val event: String = AnalyticsNames.SWAP_CHECKOUT_VIEWED.eventName
        override val params: Map<String, Serializable> = emptyMap()
    }

    object SwapPriceTooltipClickedEvent : AnalyticsEvent {
        override val event: String = AnalyticsNames.SWAP_PRICE_TOOLTIP_CLICKED.eventName
        override val params: Map<String, Serializable> = emptyMap()
    }

    object SwapCheckoutNetworkFeesClickedEvent : AnalyticsEvent {
        override val event: String = AnalyticsNames.SWAP_CHECKOUT_NETWORK_FEES_CLICKED.eventName
        override val params: Map<String, Serializable> = emptyMap()
    }

    object SwapCheckoutScreenSubmittedEvent : AnalyticsEvent {
        override val event: String = AnalyticsNames.SWAP_CHECKOUT_SCREEN_SUBMITTED.eventName
        override val params: Map<String, Serializable> = emptyMap()
    }

    object SwapCheckoutScreenBackClickedEvent : AnalyticsEvent {
        override val event: String = AnalyticsNames.SWAP_CHECKOUT_SCREEN_BACK_CLICKED.eventName
        override val params: Map<String, Serializable> = emptyMap()
    }

    object SwapTriggeredEvent : AnalyticsEvent {
        override val event: String = AnalyticsNames.SWAP_STATE_TRIGGERED.eventName
        override val params: Map<String, Serializable> = emptyMap()
    }

    object FabSwapClickedEvent : AnalyticsEvent {
        override val event: String = AnalyticsNames.FAB_SWAP_CLICKED.eventName
        override val params: Map<String, Serializable> = emptyMap()
    }

    object CoinViewSwapClickedEvent : AnalyticsEvent {
        override val event: String = AnalyticsNames.COIN_VIEW_SWAP_CLICKED.eventName
        override val params: Map<String, Serializable> = emptyMap()
    }

    object CoinViewAccountSwapClickedEvent : AnalyticsEvent {
        override val event: String = AnalyticsNames.COIN_VIEW_ACCOUNT_SWAP_CLICKED.eventName
        override val params: Map<String, Serializable> = emptyMap()
    }
}
