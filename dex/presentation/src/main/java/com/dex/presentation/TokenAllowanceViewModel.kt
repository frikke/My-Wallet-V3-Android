package com.dex.presentation

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.Coincore
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import com.blockchain.utils.abbreviate
import com.blockchain.utils.awaitOutcome
import com.dex.presentation.enteramount.AllowanceTxUiData
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CoinNetwork
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.awaitSingleOrNull

class TokenAllowanceViewModel(
    private val assetCatalogue: AssetCatalogue,
    private val coincore: Coincore
) : MviViewModel<
    AllowanceIntent,
    AllowanceViewState,
    AllowanceModelState,
    NavigationEvent,
    ModelConfigArgs.NoArgs
    >(
    initialState = AllowanceModelState()
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun AllowanceModelState.reduce() = AllowanceViewState(
        networkFeeFiat = fiatNetworkFee,
        networkFeeCrypto = cryptoNetworkFee,
        assetInfo = assetInfo,
        nativeAsset = nativeAsset,
        nativeAssetBalanceFiat = balance?.totalFiat?.toStringWithSymbol(),
        nativeAssetBalanceCrypto = balance?.total?.toStringWithSymbol(),
        networkName = network?.name,
        accountLabel = accountLabel,
        receiveAddress = receiveAddress?.abbreviate(
            startLength = ADDRESS_ABBREVIATION_LENGTH,
            endLength = ADDRESS_ABBREVIATION_LENGTH
        ).orEmpty(),
    )

    override suspend fun handleIntent(modelState: AllowanceModelState, intent: AllowanceIntent) {
        when (intent) {
            is AllowanceIntent.FetchAllowanceTxDetails -> {
                val nativeAsset = assetCatalogue.assetInfoFromNetworkTicker(intent.data.networkNativeAssetTicker)
                require(nativeAsset != null)
                updateState {
                    copy(
                        network = nativeAsset.coinNetwork,
                        cryptoNetworkFee = intent.data.feesCrypto,
                        fiatNetworkFee = intent.data.feesFiat,
                        nativeAsset = nativeAsset,
                        assetInfo = assetCatalogue.assetInfoFromNetworkTicker(intent.data.currencyTicker)
                    )
                }
                updateNativeAccountBalance(nativeAsset)
            }
        }
    }

    private fun updateNativeAccountBalance(nativeAsset: AssetInfo) {
        viewModelScope.launch {
            val nativeAccount =
                coincore[nativeAsset].accountGroup().awaitSingleOrNull()?.accounts?.firstOrNull { it.isDefault }

            nativeAccount?.let {
                it.receiveAddress.awaitOutcome().map {
                    updateState {
                        copy(
                            receiveAddress = it.address,
                        )
                    }
                }

                it.balance().map { balance -> Outcome.Success(balance) as Outcome<Exception, AccountBalance> }
                    .catch { thr ->
                        emit(Outcome.Failure(thr as Exception))
                    }
                    .collectLatest { outcome ->
                        (outcome as? Outcome.Success)?.let {
                            updateState {
                                copy(
                                    accountLabel = nativeAccount.label,
                                    balance = outcome.value
                                )
                            }
                        }
                    }
            }
        }
    }
}

data class AllowanceModelState(
    val cryptoNetworkFee: String? = null,
    val fiatNetworkFee: String? = null,
    val nativeAsset: AssetInfo? = null,
    val accountLabel: String? = null,
    val receiveAddress: String? = null,
    val network: CoinNetwork? = null,
    val assetInfo: AssetInfo? = null,
    val balance: AccountBalance? = null
) : ModelState

data class AllowanceViewState(
    val networkFeeFiat: String?,
    val networkFeeCrypto: String?,
    val nativeAssetBalanceCrypto: String?,
    val nativeAssetBalanceFiat: String?,
    val networkName: String?,
    val receiveAddress: String?,
    val accountLabel: String?,
    val nativeAsset: AssetInfo?,
    val assetInfo: AssetInfo?
) : ViewState

sealed class AllowanceIntent : Intent<AllowanceModelState> {
    data class FetchAllowanceTxDetails(val data: AllowanceTxUiData) : AllowanceIntent()
}
private const val ADDRESS_ABBREVIATION_LENGTH = 4
