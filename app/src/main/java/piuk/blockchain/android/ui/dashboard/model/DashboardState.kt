package piuk.blockchain.android.ui.dashboard.model

import androidx.annotation.VisibleForTesting
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.domain.paymentmethods.model.FundsLocks
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.CurrencyType
import info.blockchain.balance.Money
import info.blockchain.balance.percentageDelta
import info.blockchain.balance.total
import java.io.Serializable
import piuk.blockchain.android.domain.usecases.CompletableDashboardOnboardingStep
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.android.ui.dashboard.model.DashboardItem.Companion.DASHBOARD_FIAT_ASSETS
import piuk.blockchain.android.ui.dashboard.model.DashboardItem.Companion.LOCKS_INDEX
import piuk.blockchain.android.ui.dashboard.navigation.DashboardNavigationAction
import piuk.blockchain.android.ui.dashboard.sheets.BackupDetails
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class AssetMap(private val map: Map<Currency, DashboardAsset>) :
    Map<Currency, DashboardAsset> by map {

    override operator fun get(key: Currency): DashboardAsset {
        return map.getOrElse(key) {
            throw IllegalArgumentException("${key.networkTicker} is not a known Currency")
        }
    }

    fun getOrNull(key: Currency): DashboardAsset? = map[key]

    fun copy(patchBalance: AccountBalance): AssetMap {
        val assets = toMutableMap()
        // CURRENCY HERE
        val balance = patchBalance.total as CryptoValue
        val value = get(balance.currency).updateBalance(accountBalance = patchBalance)
        assets[balance.currency] = value
        return AssetMap(assets)
    }

    fun copy(patchAsset: DashboardAsset): AssetMap {
        val assets = toMutableMap()
        assets[patchAsset.currency] = patchAsset
        return AssetMap(assets)
    }

    fun reset(): AssetMap {
        val assets = toMutableMap()
        map.values.forEach { assets[it.currency] = it.reset() }
        return AssetMap(assets)
    }

    fun contains(asset: AssetInfo): Boolean = map[asset] != null
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
fun mapOfAssets(vararg pairs: Pair<AssetInfo, BrokerageCryptoAsset>) = AssetMap(mapOf(*pairs))

interface DashboardItem {
    val index: Int
    val id: String

    companion object {
        const val ANNOUNCEMENT_INDEX = 0
        const val TOTAL_BALANCE_INDEX = 1
        const val LOCKS_INDEX = 2
        const val DASHBOARD_FIAT_ASSETS = 3
        const val DASHBOARD_CRYPTO_ASSETS = Int.MAX_VALUE
    }
}

data class FiatBalanceInfo(
    val funds: List<BrokearageFiatAsset>,
) : DashboardItem {
    override val index: Int
        get() = DASHBOARD_FIAT_ASSETS
    override val id: String
        get() = funds.joinToString { it.id }

    val isSingleCurrency: Boolean
        get() = funds.size == 1
}

data class Locks(
    val fundsLocks: FundsLocks? = null,
) : DashboardItem, Serializable {
    override val index: Int
        get() = LOCKS_INDEX
    override val id: String
        get() = javaClass.name
}

sealed class DashboardOnboardingState {
    object Hidden : DashboardOnboardingState()
    data class Visible(val steps: List<CompletableDashboardOnboardingStep>) : DashboardOnboardingState()
}

interface DashboardBalanceStateHost {
    val dashboardBalance: DashboardBalance?
}

data class DashboardState(
    val dashboardNavigationAction: DashboardNavigationAction? = null,
    val selectedAsset: AssetInfo? = null,
    val filterBy: String = "",
    val isSwipingToRefresh: Boolean = false,
    val activeAssets: AssetMap = AssetMap(emptyMap()), // portfolio-only from here
    val announcement: AnnouncementCard? = null,
    val selectedFiatAccount: FiatAccount? = null,
    val selectedCryptoAccount: SingleAccount? = null,
    val backupSheetDetails: BackupDetails? = null,
    val linkablePaymentMethodsForAction: LinkablePaymentMethodsForAction? = null,
    val hasLongCallInProgress: Boolean = false,
    val isLoadingAssets: Boolean = true,
    val locks: Locks = Locks(),
    val onboardingState: DashboardOnboardingState = DashboardOnboardingState.Hidden,
    val canPotentiallyTransactWithBanks: Boolean = true,
    val showedAppRating: Boolean = false,
    val referralSuccessData: Pair<String, String>? = null
) : MviState, DashboardBalanceStateHost {

    override val dashboardBalance: DashboardBalance?
        get() = when {
            activeAssets.isEmpty() -> null
            activeAssets.values.all { it is BrokerageDashboardAsset } -> BrokerageBalanceState(
                isLoading = activeAssets.values.all { it.isUILoading },
                fiatBalance = totalFiatAndCryptoBalance(),
                assetList = activeAssets.values.map { it },
                delta = delta
            )
            activeAssets.values.all { it is DefiAsset } -> DefiBalanceState(
                isLoading = activeAssets.values.all { it.isUILoading },
                fiatBalance = cryptoAssetsFiatBalances()
            )
            else -> throw IllegalStateException("Active assets should all be Defi or Brokerage")
        }

    private fun totalFiatAndCryptoBalance(): Money? {
        val fiatBalance = fiatAssetsFiatBalance()
        val cryptoBalance = cryptoAssetsFiatBalances()
        return when {
            fiatBalance != null && cryptoBalance != null -> fiatBalance + cryptoBalance
            fiatBalance != null -> fiatBalance
            cryptoBalance != null -> cryptoBalance
            else -> null
        }
    }

    private fun cryptoAssetsFiatBalances() = activeAssets.values
        .filter { !it.isUILoading && it.fiatBalance != null && it.currency.type == CurrencyType.CRYPTO }
        .map { it.fiatBalance ?: Money.zero(it.currency) }
        .ifEmpty { null }?.total()

    private fun cryptoAssetsFiatBalances24hAgo() = activeAssets.values
        .filter { !it.isUILoading && it.fiatBalance != null && it.currency.type == CurrencyType.CRYPTO }
        .filterIsInstance<BrokerageCryptoAsset>()
        .map { it.fiatBalance24h ?: Money.zero(it.currency) }
        .ifEmpty { null }?.total()

    private fun fiatAssetsFiatBalance() = activeAssets.values
        .filter { !it.isUILoading && it.fiatBalance != null && it.currency.type == CurrencyType.FIAT }
        .mapNotNull { it.fiatBalance }
        .ifEmpty { null }?.total()

    val fiatDashboardAssets: List<BrokearageFiatAsset>
        get() = activeAssets.values.filterIsInstance<BrokearageFiatAsset>()

    /**
     * The idea here is that
     * - When in Defi mode we display all the non custodial coins regardless the balance
     * - When in Brokerage or Universal we display only assets with balances
     */
    val displayableAssets: List<DashboardAsset>
        get() {
            if (activeAssets.isEmpty()) return emptyList()
            if (activeAssets.all { it.value is DefiAsset }) return activeAssets.values.toList()
            if (activeAssets.all { it.value is BrokerageDashboardAsset }) return activeAssets.values.filter {
                it.accountBalance?.total?.isPositive ?: false
            }
            throw IllegalStateException("State is not valid ${activeAssets.values.map { it.currency }}")
        }

    /**
     * States:
     * - Assets: Is set when there is at least one available fiat or crypto asset to display
     * - Empty : When no Assets and all fetching operations have finished
     * - Loading: When dashboard is loading assets and no assets have been loaded yet.
     */
    val uiState: DashboardUIState
        get() = when {
            displayableAssets.isNotEmpty() -> DashboardUIState.ASSETS
            !isLoadingAssets && displayableAssets.isEmpty() -> DashboardUIState.EMPTY
            isLoadingAssets -> DashboardUIState.LOADING
            else -> throw IllegalStateException(
                "State is undefined for loading: $isLoadingAssets --  ${activeAssets.size} assets --" +
                    " in state Loading ${activeAssets.values.map { it.isUILoading }} -- active" +
                    " currencies: ${activeAssets.values.map { it.currency.networkTicker }}}"
            )
        }

    private val delta: Pair<Money, Double>? by unsafeLazy {
        val current = cryptoAssetsFiatBalances() ?: return@unsafeLazy null
        val old = cryptoAssetsFiatBalances24hAgo() ?: return@unsafeLazy null
        Pair(current - old, current.percentageDelta(old))
    }

    operator fun get(currency: Currency): DashboardAsset =
        activeAssets[currency]

    fun containsDashboardAssetInValidState(dashboardAsset: DashboardAsset): Boolean {
        return activeAssets.values.firstOrNull {
            it.id == dashboardAsset.id
        }?.let {
            it.fiatBalance?.isPositive == true
        } ?: false
    }
}

enum class DashboardUIState {
    LOADING,
    ASSETS,
    EMPTY
}
