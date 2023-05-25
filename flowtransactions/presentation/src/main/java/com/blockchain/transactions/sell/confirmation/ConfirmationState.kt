package com.blockchain.transactions.sell.confirmation

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.button.ButtonState
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue

data class ConfirmationModelState(
    val isStartingDepositOnChainTxEngine: Boolean = false,
    val isFetchQuoteLoading: Boolean = true,
    val isSubmittingOrderLoading: Boolean = false,

    val targetFiatAmount: FiatValue? = null,

    val sourceToTargetExchangeRate: ExchangeRate? = null,

    val sourceNetworkFeeCryptoAmount: CryptoValue? = null,

    val quoteRefreshTotalSeconds: Int? = null,
    val quoteRefreshRemainingSeconds: Int? = null,

    val quoteId: String? = null
) : ModelState

data class ConfirmationViewState(
    val isFetchQuoteLoading: Boolean,

    val sourceAsset: AssetInfo,
    val targetAsset: FiatCurrency,

    val sourceCryptoAmount: CryptoValue,
    val targetFiatAmount: FiatValue?,

    val sourceToTargetExchangeRate: ExchangeRate?,

    val sourceNetworkFeeFiatAmount: FiatValue?,

    val totalFiatAmount: FiatValue?,
    val totalCryptoAmount: CryptoValue?,

    val quoteRefreshRemainingPercentage: Float?,
    val quoteRefreshRemainingSeconds: Int?,

    val submitButtonState: ButtonState = ButtonState.Disabled
) : ViewState
