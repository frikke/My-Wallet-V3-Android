package com.blockchain.transactions.receive.accounts

import androidx.compose.runtime.Stable
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.image.LogoValue

data class ReceiveAccountsViewState(
    val accounts: DataResource<Map<ReceiveAccountType, List<ReceiveAccountViewState>>>
) : ViewState

@Stable
data class ReceiveAccountViewState(
    val id: String,
    val icon: LogoValue,
    val name: String,
    val label: String?,
    val network: String?
)

enum class ReceiveAccountType {
    Fiat,
    Crypto
}
