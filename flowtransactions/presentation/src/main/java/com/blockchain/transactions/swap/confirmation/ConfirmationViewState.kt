package com.blockchain.transactions.swap.confirmation

import com.blockchain.api.NabuApiException
import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.domain.common.model.ServerSideUxErrorInfo
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue

data class ConfirmationModelState(
    val isFetchQuoteLoading: Boolean = true,
    val isSubmittingOrderLoading: Boolean = false,

    val sourceAccount: CryptoAccount,
    val targetAccount: CryptoAccount,

    val sourceCryptoAmount: CryptoValue,
    val sourceFiatAmount: FiatValue? = null,

    val targetCryptoAmount: CryptoValue? = null,
    val targetFiatAmount: FiatValue? = null,

    val sourceToTargetExchangeRate: ExchangeRate? = null,

    val quoteRefreshTotalSeconds: Int? = null,
    val quoteRefreshRemainingSeconds: Int? = null,

    val quoteId: String? = null,
    val quoteError: ConfirmationError? = null,
    val createOrderError: ConfirmationError? = null,
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

    val quoteRefreshRemainingPercentage: Float?,
    val quoteRefreshRemainingSeconds: Int?,

    val submitButtonState: ButtonState = ButtonState.Disabled,

    val quoteError: ConfirmationError?,
    val createOrderError: ConfirmationError?,
) : ViewState

sealed class ConfirmationError {
    data class UxError(val error: ServerSideUxErrorInfo) : ConfirmationError()
    data class Error(val error: Exception) : ConfirmationError()
}

internal fun Exception.toConfirmationError(): ConfirmationError {
    val uxError = (this as? NabuApiException)?.getServerSideErrorInfo()
    val error = if (uxError != null) {
        ConfirmationError.UxError(uxError)
    } else {
        ConfirmationError.Error(this)
    }
    return error
}
