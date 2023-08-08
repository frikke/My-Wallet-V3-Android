package com.blockchain.transactions.receive.accounts

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.OneTimeAccountPersistenceService
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.filterByActionAndState
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.data.combineDataResourceFlows
import com.blockchain.data.filter
import com.blockchain.data.map
import com.blockchain.data.mapList
import com.blockchain.data.updateDataWith
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.fiatActions.fiatactions.FiatActionsUseCase
import com.blockchain.image.LogoValue
import com.blockchain.outcome.doOnSuccess
import com.blockchain.presentation.analytics.TxFlowAnalyticsAccountType
import com.blockchain.transactions.common.withId
import com.blockchain.utils.awaitOutcome
import com.blockchain.utils.toFlowDataResource
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.isLayer2Token
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

class ReceiveAccountsViewModel(
    private val walletModeService: WalletModeService,
    private val coincore: Coincore,
    private val assetCatalogue: AssetCatalogue,
    private val oneTimeAccountPersistenceService: OneTimeAccountPersistenceService,
    private val fiatCurrenciesService: FiatCurrenciesService,
    private val fiatActions: FiatActionsUseCase,
) : MviViewModel<
    ReceiveAccountsIntent,
    ReceiveAccountsViewState,
    ReceiveAccountsModelState,
    ReceiveAccountsNavigation,
    ModelConfigArgs.NoArgs
    >(
    ReceiveAccountsModelState()
) {
    private var loadDataJob: Job? = null
    private var accountSelectedJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun ReceiveAccountsModelState.reduce() = ReceiveAccountsViewState(
        accounts = accounts
            .filter { account ->
                with(account.data) {
                    searchTerm.isEmpty() ||
                        currency.networkTicker.contains(searchTerm, true) ||
                        currency.displayTicker.contains(searchTerm, true) ||
                        currency.name.contains(searchTerm, true)
                }
            }
            .mapList { account ->
                with(account.data) {
                    val accountType = toAccountType()

                    val network = (this as? CryptoNonCustodialAccount)?.currency
                        ?.takeIf { it.isLayer2Token }
                        ?.coinNetwork

                    val mainLogo = currency.logo
                    val tagLogo = network?.nativeAssetTicker
                        ?.let { assetCatalogue.fromNetworkTicker(it)?.logo }

                    accountType to ReceiveAccountViewState(
                        id = account.id,
                        icon = tagLogo?.let {
                            LogoValue.SmallTag(main = mainLogo, tag = tagLogo)
                        } ?: LogoValue.SingleIcon(url = mainLogo),
                        name = currency.name,
                        label = when (accountType) {
                            ReceiveAccountType.Fiat -> null
                            ReceiveAccountType.Crypto -> if (this is NonCustodialAccount) {
                                label
                            } else {
                                currency.displayTicker
                            }
                        },
                        network = network?.shortName
                    )
                }
            }
            .map {
                it.groupBy(
                    keySelector = { it.first },
                    valueTransform = { it.second }
                )
            }
    )

    private fun SingleAccount.toAccountType() = when {
        this is FiatAccount -> ReceiveAccountType.Fiat
        else -> ReceiveAccountType.Crypto
    }

    override suspend fun handleIntent(modelState: ReceiveAccountsModelState, intent: ReceiveAccountsIntent) {
        when (intent) {
            ReceiveAccountsIntent.LoadData -> {
                loadData()
            }

            is ReceiveAccountsIntent.AccountSelected -> {
                verifyKycAndNavigate(id = intent.id)
            }

            is ReceiveAccountsIntent.Search -> {
                updateState {
                    copy(
                        searchTerm = intent.term
                    )
                }
            }
        }
    }

    private fun loadData() {
        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch {
            walletModeService.walletMode
                .flatMapLatest {

                    // crypto accounts that support receive
                    val allReceiveAccountsFlow = coincore.allWalletsInMode(it)
                        .flatMap {
                            it.accounts.filterByActionAndState(
                                action = AssetAction.Receive,
                                states = listOf(ActionState.Available, ActionState.LockedForTier)
                            )
                        }
                        .toFlowDataResource()

                    // to get fiat accounts + to sort the list
                    val activeAccountsFlow = coincore.activeWalletsInMode(it)
                        .map { DataResource.Data(it.accounts) }

                    // todo why fiat is not returned by allwallet
                    combineDataResourceFlows(
                        allReceiveAccountsFlow,
                        activeAccountsFlow
                    ) { allReceiveAccounts, activeAccounts ->
                        val fiatAccount = activeAccounts.filterIsInstance<FiatAccount>()
                            .firstOrNull {
                                it.currency.networkTicker == fiatCurrenciesService.selectedTradingCurrency.networkTicker
                            }

                        val sortedCryptoAccounts = allReceiveAccounts.sortedWith(
                            compareBy<SingleAccount> {
                                it.currency.networkTicker !in activeAccounts.map { acc -> acc.currency.networkTicker }
                            }.thenByDescending { it.currency.index }
                                .thenBy { it.currency.displayTicker }
                                .thenBy { !it.isDefault }
                                .thenBy { it.label }
                        )

                        // combine fiat + crypto, reduce will split them
                        listOfNotNull(fiatAccount) + sortedCryptoAccounts
                    }
                }
                .catch {
                    Timber.e(it)
                }
                .collectLatest {
                    updateState {
                        copy(
                            accounts = accounts.updateDataWith(it.mapList { it.withId() })
                        )
                    }
                }
        }
    }

    private fun verifyKycAndNavigate(id: String) {
        val account = (modelState.accounts as? DataResource.Data)?.data?.find { it.id == id }?.data
        check(account != null)

        accountSelectedJob?.cancel()
        accountSelectedJob = viewModelScope.launch {
            account.stateAwareActions
                .awaitOutcome()
                .doOnSuccess { stateAwareActions ->
                    val receiveAction = stateAwareActions.find {
                        val action = when (account.toAccountType()) {
                            ReceiveAccountType.Fiat -> AssetAction.FiatDeposit
                            ReceiveAccountType.Crypto -> AssetAction.Receive
                        }

                        it.action == action
                    }
                    if (receiveAction?.state == ActionState.Available) {
                        when (account.toAccountType()) {
                            ReceiveAccountType.Fiat -> {
                                fiatActions.deposit(
                                    account = account as FiatAccount,
                                    action = AssetAction.FiatDeposit,
                                    shouldLaunchBankLinkTransfer = false,
                                    shouldSkipQuestionnaire = false
                                )
                            }

                            ReceiveAccountType.Crypto -> {
                                oneTimeAccountPersistenceService.saveAccount(account)
                                navigate(
                                    ReceiveAccountsNavigation.Detail(
                                        accountType = TxFlowAnalyticsAccountType.fromAccount(account),
                                        networkTicker = account.currency.networkTicker
                                    )
                                )
                            }
                        }
                    } else {
                        navigate(ReceiveAccountsNavigation.KycUpgrade)
                    }
                }
        }
    }
}
