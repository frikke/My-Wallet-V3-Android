package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.Asset
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.commonarch.presentation.mvi.MviIntent
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import java.io.Serializable
import piuk.blockchain.android.domain.usecases.CompletableDashboardOnboardingStep
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

    object GetActiveAssets : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState {
            return oldState.copy(
                activeAssets = AssetMap(mapOf()),
                fiatAssets = FiatAssetState(),
                isLoadingAssets = true
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
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState {
            return oldState
        }
    }

    class UpdateAllAssetsAndBalances(
        private val assetList: List<DashboardAsset>,
        private val fiatAssetList: List<FiatAccount>
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState {
            val fiatState = FiatAssetState(
                fiatAssetList.associateBy(
                    keySelector = { it.currency },
                    valueTransform = { FiatBalanceInfo(it) }
                )
            )
            return oldState.copy(
                activeAssets = AssetMap(
                    assetList.associateBy(
                        keySelector = { it.currency },
                        valueTransform = {
                            it
                        }
                    )
                ),
                fiatAssets = fiatState
            )
        }
    }

    class GetAssetPrice(val asset: AssetInfo) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState = oldState
    }

    class AssetPriceUpdate(
        val asset: AssetInfo,
        val price: ExchangeRate
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState {
            val updatedActiveList = if (oldState.activeAssets.contains(asset)) {
                val oldAsset = oldState.activeAssets[asset]
                val newAsset = updateAsset(oldAsset, price)
                oldState.activeAssets.copy(patchAsset = newAsset)
            } else {
                oldState.activeAssets
            }
            return oldState.copy(
                activeAssets = updatedActiveList
            )
        }

        private fun updateAsset(
            old: DashboardAsset,
            rate: ExchangeRate
        ): DashboardAsset {
            return old.updateExchangeRate(
                rate = rate
            )
        }
    }

    class AssetPriceWithDeltaUpdate(
        val asset: AssetInfo,
        private val prices24HrWithDelta: Prices24HrWithDelta,
        // Only fetch day historical prices for active assets, the ones with balance, to draw the small graph
        val shouldFetchDayHistoricalPrices: Boolean,
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState {
            val oldAsset = oldState.activeAssets[asset]
            val newAsset = updateAsset(oldAsset, prices24HrWithDelta)
            val updatedActiveList = oldState.activeAssets.copy(patchAsset = newAsset)

            return oldState.copy(
                activeAssets = updatedActiveList
            )
        }

        override fun isValidFor(oldState: DashboardState): Boolean = oldState.activeAssets[asset] is BrokerageAsset

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

    class FiatBalanceUpdate(
        private val balance: Money,
        private val fiatBalance: Money,
        private val balanceAvailable: Money,
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState {
            val oldFiatValues = oldState.fiatAssets
            return oldState.copy(
                fiatAssets = oldFiatValues.updateWith(
                    balance.currency,
                    balance as FiatValue,
                    fiatBalance as FiatValue,
                    balanceAvailable
                )
            )
        }
    }

    object ResetNavigation : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState =
            oldState.copy(
                dashboardNavigationAction = null
            )
    }

    class RefreshAllBalancesIntent(private val loadSilently: Boolean) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState {
            val activeAssets = if (loadSilently) oldState.activeAssets else oldState.activeAssets.reset()
            val fiatAssets = if (loadSilently) oldState.fiatAssets else oldState.fiatAssets.reset()
            return oldState.copy(activeAssets = activeAssets, fiatAssets = fiatAssets)
        }
    }

    object ResetDashboardAssets : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState {
            return oldState.copy(activeAssets = oldState.activeAssets.reset(), fiatAssets = oldState.fiatAssets.reset())
        }
    }

    class BalanceUpdate(
        val asset: AssetInfo,
        private val newBalance: AccountBalance
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState {
            val balance = newBalance.total as CryptoValue
            require(asset == balance.currency) {
                throw IllegalStateException("CryptoCurrency mismatch")
            }

            val oldAsset = oldState[asset]
            val newAsset = oldAsset.updateBalance(accountBalance = newBalance)
            val newAssets = oldState.activeAssets.copy(patchAsset = newAsset)

            return oldState.copy(activeAssets = newAssets, isLoadingAssets = false)
        }
    }

    class BalanceUpdateError(
        val asset: AssetInfo,
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState {
            val oldAsset = oldState[asset]
            val newAsset = oldAsset.toErrorState()
            val newAssets = oldState.activeAssets.copy(patchAsset = newAsset)

            return oldState.copy(activeAssets = newAssets)
        }
    }

    class RefreshPrices(
        val asset: DashboardAsset,
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState = oldState
    }

    class PriceHistoryUpdate(
        val asset: AssetInfo,
        private val historicPrices: HistoricalRateList
    ) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState {
            return if (oldState.activeAssets.contains(asset)) {
                val oldAsset = oldState.activeAssets[asset] as? BrokerageAsset ?: throw IllegalStateException(
                    "Historic prices are only supported for brokerage"
                )
                val newAsset = updateAsset(oldAsset, historicPrices)
                oldState.copy(activeAssets = oldState.activeAssets.copy(patchAsset = newAsset))
            } else {
                oldState
            }
        }

        private fun updateAsset(
            old: BrokerageAsset,
            historicPrices: HistoricalRateList
        ): BrokerageAsset {
            val trend = historicPrices.map { it.rate.toFloat() }
            return old.copy(priceTrend = trend)
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
                dashboardNavigationAction = DashboardNavigationAction.BackUpBeforeSend(BackupDetails(account, action))
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

    class RefreshFiatBalances(val fiatAccounts: Map<Currency, FiatBalanceInfo>) : DashboardIntent() {
        override fun reduce(oldState: DashboardState): DashboardState = oldState
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
}
