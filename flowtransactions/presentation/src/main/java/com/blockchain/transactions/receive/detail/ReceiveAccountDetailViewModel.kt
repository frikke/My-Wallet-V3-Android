package com.blockchain.transactions.receive.detail

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.coincore.NullFiatAccount.currency
import com.blockchain.coincore.OneTimeAccountPersistenceService
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.commonarch.presentation.mvi_v2.EmptyNavEvent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.data.map
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.presentation.analytics.TxFlowAnalyticsAccountType
import com.blockchain.utils.awaitOutcome
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.isLayer2Token
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ReceiveAccountDetailViewModel(
    oneTimeAccountPersistenceService: OneTimeAccountPersistenceService,
    private val assetCatalogue: AssetCatalogue,
) : MviViewModel<
    ReceiveAccountDetailIntent,
    ReceiveAccountDetailViewState,
    ReceiveAccountDetailModelState,
    EmptyNavEvent,
    ModelConfigArgs.NoArgs
    >(
    ReceiveAccountDetailModelState(
        account = oneTimeAccountPersistenceService.getAccount()
    )
) {
    private var loadDataJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun ReceiveAccountDetailModelState.reduce(): ReceiveAccountDetailViewState {
        val network = (account as? CryptoNonCustodialAccount)?.currency
            ?.takeIf { it.isLayer2Token }
            ?.coinNetwork

        val mainLogo = account.currency.logo
        val tagLogo = network?.nativeAssetTicker
            ?.let { assetCatalogue.fromNetworkTicker(it)?.logo }

        return ReceiveAccountDetailViewState(
            assetTicker = account.currency.displayTicker,
            accountLabel = account.label,
            mainLogo = mainLogo,
            tagLogo = tagLogo,
            coinNetwork = network,
            receiveAddress = receiveAddress.map {
                ReceiveAddressViewState(
                    uri = it.toUrl(),
                    address = it.address,
                    memo = it.memo,
                    isRotating = isRotatingAddress
                )
            },
            accountType = TxFlowAnalyticsAccountType.fromAccount(account),
            networkTicker = account.currency.networkTicker
        )
    }

    override suspend fun handleIntent(
        modelState: ReceiveAccountDetailModelState,
        intent: ReceiveAccountDetailIntent
    ) {
        when (intent) {
            ReceiveAccountDetailIntent.LoadData -> {
                loadDataJob?.cancel()
                loadDataJob = viewModelScope.launch {
                    check(modelState.account !is NullCryptoAccount) { "account not set" }

                    modelState.account.receiveAddress
                        .doOnSubscribe {
                            updateState {
                                copy(
                                    receiveAddress = DataResource.Loading
                                )
                            }
                        }
                        .awaitOutcome()
                        .doOnFailure {
                        }
                        .doOnSuccess {
                            updateState {
                                copy(
                                    receiveAddress = DataResource.Data(it as CryptoAddress),
                                    isRotatingAddress = (account as? CryptoAccount)?.hasStaticAddress?.not() ?: false
                                )
                            }
                        }
                }
            }
        }
    }
}
