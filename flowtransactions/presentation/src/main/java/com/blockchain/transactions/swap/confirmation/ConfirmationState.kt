package com.blockchain.transactions.swap.confirmation

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.button.ButtonState
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue

data class ConfirmationModelState(
    val isStartingDepositOnChainTxEngine: Boolean = true,
    val isFetchQuoteLoading: Boolean = true,
    val isSubmittingOrderLoading: Boolean = false,

    val targetCryptoAmount: CryptoValue? = null,

    val sourceToTargetExchangeRate: ExchangeRate? = null,
    val sourceToFiatExchangeRate: ExchangeRate? = null,
    val targetToFiatExchangeRate: ExchangeRate? = null,

    val sourceNetworkFeeCryptoAmount: CryptoValue? = null,
    val targetNetworkFeeCryptoAmount: CryptoValue? = null,

    val quoteRefreshTotalSeconds: Int? = null,
    val quoteRefreshRemainingSeconds: Int? = null,

    val quoteId: String? = null,
) : ModelState

data class ConfirmationViewState(
    val isFetchQuoteLoading: Boolean,

    val sourceAsset: AssetInfo,
    val targetAsset: AssetInfo,

    val sourceCryptoAmount: CryptoValue,
    val sourceFiatAmount: FiatValue?,

    val targetCryptoAmount: CryptoValue?,
    val targetFiatAmount: FiatValue?,

    val sourceToTargetExchangeRate: ExchangeRate?,

    val sourceNetworkFeeCryptoAmount: CryptoValue?,
    val sourceNetworkFeeFiatAmount: FiatValue?,
    val targetNetworkFeeCryptoAmount: CryptoValue?,
    val targetNetworkFeeFiatAmount: FiatValue?,

    val quoteRefreshRemainingPercentage: Float?,
    val quoteRefreshRemainingSeconds: Int?,

    val submitButtonState: ButtonState = ButtonState.Disabled,
) : ViewState
