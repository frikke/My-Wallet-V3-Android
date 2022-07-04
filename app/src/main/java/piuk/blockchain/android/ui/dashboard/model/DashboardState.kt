package piuk.blockchain.android.ui.dashboard.model

import androidx.annotation.VisibleForTesting
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.domain.paymentmethods.model.FundsLocks
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.balance.percentageDelta
import info.blockchain.balance.total
import java.io.Serializable
import piuk.blockchain.android.domain.usecases.CompletableDashboardOnboardingStep
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.android.ui.dashboard.model.DashboardItem.Companion.FIAT_FUNDS_INDEX
import piuk.blockchain.android.ui.dashboard.model.DashboardItem.Companion.LOCKS_INDEX
import piuk.blockchain.android.ui.dashboard.navigation.DashboardNavigationAction
import piuk.blockchain.android.ui.dashboard.sheets.BackupDetails
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

data class AssetPriceState(
    val assetInfo: AssetInfo,
    val prices: Prices24HrWithDelta? = null,
)

class AssetMap(private val map: Map<AssetInfo, DashboardAsset>) :
    Map<AssetInfo, DashboardAsset> by map {

    override operator fun get(key: AssetInfo): DashboardAsset {
        return map.getOrElse(key) {
            throw IllegalArgumentException("${key.networkTicker} is not a known CryptoCurrency")
        }
    }

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
fun mapOfAssets(vararg pairs: Pair<AssetInfo, BrokerageAsset>) = AssetMap(mapOf(*pairs))

interface DashboardItem {
    val index: Int
    val id: String

    companion object {
        const val ANNOUNCEMENT_INDEX = 0
        const val TOTAL_BALANCE_INDEX = 1
        const val LOCKS_INDEX = 2
        const val FIAT_FUNDS_INDEX = 3
        const val DASHBOARD_CRYPTO_ASSETS = Int.MAX_VALUE
    }
}

data class FiatBalanceInfo(
    val account: FiatAccount,
    val balance: Money = Money.zero(account.currency),
    val userFiat: Money? = null,
    val availableBalance: Money? = null,
)

data class FiatAssetState(
    val fiatAccounts: Map<Currency, FiatBalanceInfo> = emptyMap(),
) : DashboardItem {
    override val index: Int
        get() = FIAT_FUNDS_INDEX
    override val id: String
        get() = fiatAccounts.keys.joinToString()

    fun updateWith(
        currency: Currency,
        balance: FiatValue,
        userFiatBalance: FiatValue,
        availableBalance: Money,
    ): FiatAssetState {
        val newBalanceInfo = fiatAccounts[currency]?.copy(
            balance = balance,
            userFiat = userFiatBalance,
            availableBalance = availableBalance
        )

        return newBalanceInfo?.let {
            val newMap = fiatAccounts.toMutableMap()
            newMap[currency] = it
            FiatAssetState(newMap)
        } ?: this
    }

    fun reset(): FiatAssetState =
        FiatAssetState(fiatAccounts.mapValues { old -> FiatBalanceInfo(old.value.account) })

    val totalBalance: Money?
        get() = if (fiatAccounts.isEmpty()) {
            null
        } else {
            val fiatList = fiatAccounts.values.mapNotNull { it.userFiat }
            if (fiatList.isNotEmpty()) {
                fiatList.total()
            } else {
                null
            }
        }
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
    val activeAssets: AssetMap = AssetMap(emptyMap()), // portfolio-only from here
    val announcement: AnnouncementCard? = null,
    val fiatAssets: FiatAssetState = FiatAssetState(),
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
            activeAssets.values.all { it is BrokerageAsset } -> BrokerageBalanceState(
                isLoading = activeAssets.values.all { it.isLoading },
                fiatBalance = addFiatBalance(cryptoAssetFiatBalances()),
                assetList = activeAssets.values.map { it },
                fiatAssets = fiatAssets,
                delta = delta
            )
            activeAssets.values.all { it is DefiAsset } -> DefiBalanceState(
                isLoading = activeAssets.values.all { it.isLoading },
                fiatBalance = addFiatBalance(cryptoAssetFiatBalances())
            )
            else -> throw IllegalStateException("Active assets should all be Defi or Brokerage")
        }

    private fun cryptoAssetFiatBalances() = activeAssets.values
        .filter { !it.isLoading && it.fiatBalance != null }
        .map { it.fiatBalance!! }
        .ifEmpty { null }?.total()

    private val fiatBalance24h: Money? by unsafeLazy {
        addFiatBalance(cryptoAssetFiatBalances24h())
    }

    private fun cryptoAssetFiatBalances24h() = activeAssets.values
        .filterIsInstance<BrokerageAsset>()
        .filter { !it.isLoading && it.fiatBalance24h != null }
        .map { it.fiatBalance24h!! }
        .ifEmpty { null }?.total()

    private fun addFiatBalance(balance: Money?): Money? {
        val fiatAssetBalance = fiatAssets.totalBalance

        return if (balance != null) {
            if (fiatAssetBalance != null) {
                balance + fiatAssetBalance
            } else {
                balance
            }
        } else {
            fiatAssetBalance
        }
    }

    private val delta: Pair<Money, Double>? by unsafeLazy {
        val current = addFiatBalance(cryptoAssetFiatBalances())
        val old = fiatBalance24h
        if (current != null && old != null) {
            Pair(current - old, current.percentageDelta(old))
        } else {
            null
        }
    }

    operator fun get(currency: AssetInfo): DashboardAsset =
        activeAssets[currency]

    val assetMapKeys = activeAssets.keys
}
