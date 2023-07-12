package com.blockchain.transactions.swap.confirmation

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.button.ButtonState
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue

data class SwapConfirmationModelState(
    val isStartingDepositOnChainTxEngine: Boolean = false,
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

    val quoteId: String? = null
) : ModelState

data class SwapConfirmationViewState(
    val isFetchQuoteLoading: Boolean,

    val sourceAsset: AssetInfo,
    val sourceNativeAssetIconUrl: String?,
    val sourceAssetDescription: String,
    val sourceAmount: AmountViewState,
    val sourceNetworkFee: AmountViewState?,
    val sourceSubtotal: AmountViewState?,

    val targetAsset: AssetInfo,
    val targetNativeAssetIconUrl: String?,
    val targetAssetDescription: String,
    val targetAmount: AmountViewState?,
    val targetNetworkFee: AmountViewState?,
    val targetNetAmount: AmountViewState?,

    val sourceToTargetExchangeRate: ExchangeRate?,

    val quoteRefreshRemainingPercentage: Float?,
    val quoteRefreshRemainingSeconds: Int?,

    val submitButtonState: ButtonState = ButtonState.Disabled
) : ViewState

data class AmountViewState(
    val cryptoValue: CryptoValue?,
    val fiatValue: FiatValue
)
