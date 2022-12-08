package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.Asset
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.commonarch.presentation.mvi.MviIntent
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.domain.onboarding.CompletableDashboardOnboardingStep
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import java.io.Serializable
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.android.ui.dashboard.navigation.DashboardNavigationAction
import piuk.blockchain.android.ui.dashboard.sheets.BackupDetails

// todo Ideally we want to map this at the coincore layer to some new object, so that the dashboard doesn't have a dependency on core. Since there are a couple of others that are just passed through, though, this can be for later.
sealed class DashboardIntent : MviIntent<DashboardState> {
    object VerifyAppRating : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState = oldState
    }

    object ShowAppRating : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState =
            oldState.copy(
                dashboardNavigationAction = DashboardNavigationAction.AppRating,
                showedAppRating = true
            )

        override fun isValidFor(oldState: DashboardState): Boolean = oldState.showedAppRating.not()
    }

    data class GetActiveAssets(
        private val loadSilently: Boolean
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState {
            val activeAssets = if (loadSilently) oldState.activeAssets else oldState.activeAssets.reset()
            return oldState.copy(
                activeAssets = activeAssets,
                isLoadingAssets = !loadSilently || activeAssets.isEmpty()
            )
        }
    }

    object ClearActiveFlow : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState =
            oldState.copy(
                dashboardNavigationAction = null,
                selectedAsset = null
            )
    }

    class UpdateActiveAssets(
        val assetList: List<Asset>,
        val walletMode: WalletMode,
        val totalDisplayBalanceFFEnabled: Boolean,
        val assetDisplayBalanceFFEnabled: Boolean,
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState {
            return oldState
        }
    }

    class UpdateAllAssetsAndBalances(
        val assetList: List<DashboardAsset>,
        val walletMode: WalletMode
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState {

            return oldState.copy(
                activeAssets = AssetMap(
                    assetList.associateBy(
                        keySelector = { it.currency },
                        valueTransform = {
                            if (oldState.containsDashboardAssetInValidState(it)) {
                                oldState.activeAssets.getValue(it.currency)
                            } else {
                                it
                            }
                        }
                    )
                )
            )
        }
    }

    object NoActiveAssets : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState = oldState.copy(isLoadingAssets = false)
    }

    class AssetsPriceWithDeltaUpdate(
        val pricedAssets: Map<AssetInfo, Prices24HrWithDelta>,
        // Only fetch day historical prices for active assets, the ones with balance, to draw the small graph
        val shouldFetchDayHistoricalPrices: Boolean,
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState {

            val newAssets = pricedAssets.map {
                val oldAsset = oldState.activeAssets[it.key]
                updateAsset(oldAsset, it.value)
            }

            return oldState.copy(
                activeAssets = oldState.activeAssets.copy(newAssets)
            )
        }

        override fun isValidFor(oldState: DashboardState): Boolean =
            pricedAssets.all { oldState.activeAssets[it.key] is BrokerageCryptoAsset }

        private fun updateAsset(
            old: DashboardAsset,
            prices24HrWithDelta: Prices24HrWithDelta,
        ): DashboardAsset {
            return old.updatePrices24HrWithDelta(
                prices24HrWithDelta
            )
        }
    }

    class UpdateNavigationAction(
        val action: DashboardNavigationAction,
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState = oldState.copy(
            dashboardNavigationAction = action
        )
    }

    class FilterAssets(private val searchString: String) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState =
            oldState.copy(
                filterBy = searchString
            )
    }

    object ResetNavigation : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState =
            oldState.copy(
                dashboardNavigationAction = null
            )
    }

    object ResetDashboardAssets : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState {
            return oldState.copy(activeAssets = oldState.activeAssets.reset())
        }
    }

    class BalanceUpdateForAssets(
        val models: List<BalanceUpdateModel>
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState {
            val newBalancedAssets = models.map {
                if (it.hasError)
                    return@map oldState[it.currency].toErrorState()
                else {
                    val oldAsset = oldState[it.currency]
                    val newAsset = oldAsset.updateBalance(accountBalance = it.balance)
                        .updateExchangeRate(it.balance.exchangeRate)
                        .updateFetchingBalanceState(false)
                        .shouldAssetShow(it.shouldShow)
                    newAsset
                }
            }
            return oldState.copy(
                activeAssets = oldState.activeAssets.copy(newBalancedAssets),
                isLoadingAssets = false,
                isSwipingToRefresh = false
            )
        }
    }

    class BalanceUpdate(
        val asset: Currency,
        private val newBalance: AccountBalance,
        private val shouldAssetShow: Boolean
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState {
            val balance = newBalance.total
            require(asset == balance.currency) {
                throw IllegalStateException("CryptoCurrency mismatch")
            }

            val oldAsset = oldState[asset]
            val newAsset = oldAsset.updateBalance(accountBalance = newBalance)
                .updateExchangeRate(newBalance.exchangeRate)
                .updateFetchingBalanceState(false)
                .shouldAssetShow(shouldAssetShow)
            val newAssets = oldState.activeAssets.copy(patchAsset = newAsset)

            return oldState.copy(
                activeAssets = newAssets,
                isLoadingAssets = false,
                isSwipingToRefresh = oldState.isSwipingToRefresh && newAssets.values.any {
                    it.isFetchingBalance
                }
            ).also {
                println(
                    "Got balance and should show ${it.isSwipingToRefresh} --- ${
                    newAssets.values.filter {
                        it.isFetchingBalance
                    }.map { it.currency.networkTicker }
                    }"
                )
            }
        }

        override fun isValidFor(oldState: DashboardState): Boolean {
            return oldState.activeAssets.contains(asset)
        }
    }

    class BalanceUpdateError(
        val asset: Currency,
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState {
            val oldAsset = oldState[asset]
            val newAsset = oldAsset.toErrorState()
            val newAssets = oldState.activeAssets.copy(patchAsset = newAsset)

            return oldState.copy(
                activeAssets = newAssets,
                isSwipingToRefresh = oldState.isSwipingToRefresh && newAssets.values.any {
                    it.isFetchingBalance
                }
            )
        }
    }

    class BalanceFetching(
        private val asset: Currency,
        private val isFetching: Boolean,
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState {
            val oldAsset = oldState[asset]
            val newAsset = oldAsset.updateFetchingBalanceState(isFetching)
            val newAssets = oldState.activeAssets.copy(patchAsset = newAsset)
            return oldState.copy(
                activeAssets = newAssets,
            )
        }

        override fun isValidFor(oldState: DashboardState): Boolean {
            return asset in oldState.activeAssets
        }
    }

    class RefreshPrices(
        val assets: List<DashboardAsset>,
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState = oldState
    }

    class PriceHistoryUpdate(
        private val historicPricedAssets: Map<AssetInfo, HistoricalRateList>
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState {
            val assets = historicPricedAssets.mapNotNull {
                if (oldState.activeAssets.contains(it.key)) {
                    val oldAsset =
                        oldState.activeAssets[it.key] as? BrokerageCryptoAsset ?: throw IllegalStateException(
                            "Historic prices are only supported for brokerage"
                        )
                    updateAsset(oldAsset, it.value)
                } else null
            }

            return if (assets.isNotEmpty()) {
                oldState.copy(activeAssets = oldState.activeAssets.copy(assets))
            } else
                oldState
        }

        private fun updateAsset(
            old: BrokerageCryptoAsset,
            historicPrices: HistoricalRateList
        ): BrokerageCryptoAsset {
            val trend = historicPrices.map { it.rate.toFloat() }
            return old.copy(priceTrend = trend)
        }

        override fun isValidFor(oldState: DashboardState): Boolean {
            return historicPricedAssets.all {
                oldState.activeAssets.getOrNull(it.key)?.let { asset ->
                    asset is BrokerageCryptoAsset
                } ?: false
            }
        }
    }

    class ShowAnnouncement(private val card: AnnouncementCard) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState {
            return oldState.copy(announcement = card)
        }
    }

    object JoinNftWaitlist : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState = oldState
    }

    object ClearAnnouncement : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState {
            return oldState.copy(announcement = null)
        }
    }

    class ShowFiatAssetDetails(
        private val fiatAccount: FiatAccount,
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState =
            oldState.copy(
                dashboardNavigationAction = DashboardNavigationAction.FiatFundsDetails(fiatAccount),
            )
    }

    data class ShowBankLinkingSheet(
        private val fiatAccount: FiatAccount? = null,
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState =
            oldState.copy(
                dashboardNavigationAction = DashboardNavigationAction.LinkOrDeposit(fiatAccount),
            )
    }

    data class ShowBankLinkingWithAlias(
        private val fiatAccount: FiatAccount? = null,
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState =
            oldState.copy(
                dashboardNavigationAction = DashboardNavigationAction.LinkWithAlias(fiatAccount),
            )
    }

    object UpdateDepositButton : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState = oldState
    }

    data class SetDepositVisibility(val showDeposit: Boolean) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState {
            return oldState.copy(
                canPotentiallyTransactWithBanks = showDeposit
            )
        }
    }

    data class ShowLinkablePaymentMethodsSheet(
        private val fiatAccount: FiatAccount,
        private val paymentMethodsForAction: LinkablePaymentMethodsForAction,
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState =
            oldState.copy(
                dashboardNavigationAction = DashboardNavigationAction.PaymentMethods(paymentMethodsForAction),
                selectedFiatAccount = fiatAccount
            )
    }

    class ShowPortfolioSheet(
        private val dashboardNavigationAction: DashboardNavigationAction,
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState =
            // Custody sheet isn't displayed via this intent, so filter it out
            oldState.copy(
                dashboardNavigationAction = dashboardNavigationAction,
                selectedFiatAccount = null
            )
    }

    class CancelSimpleBuyOrder(
        val orderId: String,
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState = oldState
    }

    class CheckBackupStatus(
        val account: SingleAccount,
        val action: AssetAction,
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState = oldState
    }

    class ShowBackupSheet(
        private val account: SingleAccount,
        private val action: AssetAction,
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState =
            oldState.copy(
                dashboardNavigationAction = DashboardNavigationAction.BackUpBeforeSend(
                    BackupDetails(account, action)
                )
            )
    }

    class UpdateSelectedCryptoAccount(
        private val cryptoAccount: CryptoAccount,
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState =
            oldState.copy(
                selectedCryptoAccount = cryptoAccount,
                selectedAsset = cryptoAccount.currency
            )
    }

    data class StartBankTransferFlow(val currency: String = "", val action: AssetAction) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState =
            oldState.copy(
                dashboardNavigationAction = null,
                backupSheetDetails = null
            )
    }

    data class LaunchBankTransferFlow(
        val account: SingleAccount,
        val action: AssetAction,
        val shouldLaunchBankLinkTransfer: Boolean,
        val shouldSkipQuestionnaire: Boolean = false
    ) : DashboardIntent(), Serializable {
        override fun reduce(oldState: DashboardState): DashboardState =
            oldState.copy(
                dashboardNavigationAction = null,
                backupSheetDetails = null
            )
    }

    object LongCallStarted : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState =
            oldState.copy(
                hasLongCallInProgress = true
            )
    }

    object LongCallEnded : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState =
            oldState.copy(
                hasLongCallInProgress = false
            )
    }

    data class LaunchBankLinkFlow(
        val linkBankTransfer: LinkBankTransfer,
        val fiatAccount: FiatAccount,
        val assetAction: AssetAction,
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState =
            oldState.copy(
                dashboardNavigationAction = DashboardNavigationAction.LinkBankWithPartner(
                    linkBankTransfer = linkBankTransfer,
                    fiatAccount = fiatAccount,
                    assetAction = assetAction
                ),
                backupSheetDetails = null
            )
    }

    object LoadFundsLocked : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState = oldState
    }

    class FundsLocksLoaded(
        private val fundsLocks: FundsLocks?,
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState =
            oldState.copy(
                locks = Locks(fundsLocks)
            )
    }

    object CheckCowboysFlow : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState = oldState
    }

    object CowboysReferralCardClosed : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState = oldState.copy(
            dashboardCowboysState = DashboardCowboysState.Hidden
        )
    }

    class UpdateCowboysViewState(val cowboysState: DashboardCowboysState) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState =
            oldState.copy(dashboardCowboysState = cowboysState)
    }

    object FetchOnboardingSteps : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState = oldState
    }

    class FetchOnboardingStepsSuccess(
        private val onboardingState: DashboardOnboardingState,
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState = oldState.copy(
            onboardingState = onboardingState
        )
    }

    data class LaunchDashboardOnboarding(val initialSteps: List<CompletableDashboardOnboardingStep>) :
        DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState = oldState.copy(
            dashboardNavigationAction = DashboardNavigationAction.DashboardOnboarding(initialSteps)
        )
    }

    object FetchReferralSuccess : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState = oldState.copy()
    }

    data class ShowReferralSuccess(val referralSuccessData: Pair<String, String>) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState = oldState.copy(
            referralSuccessData = referralSuccessData
        )
    }

    object DismissReferralSuccess : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState = oldState.copy(
            referralSuccessData = null
        )
    }

    object OnSwipeToRefresh : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState = oldState.copy(
            isSwipingToRefresh = true
        )
    }

    object LoadStakingFlag : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState = oldState.copy()
    }

    object DisposePricesAndBalances : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState = oldState.copy()
    }

    class UpdateStakingFlag(private val isStakingEnabled: Boolean) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState =
            oldState.copy(isStakingEnabled = isStakingEnabled)
    }
}
