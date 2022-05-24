package piuk.blockchain.android.ui.dashboard.model

import androidx.annotation.VisibleForTesting
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.core.chains.erc20.isErc20
import com.blockchain.core.payments.model.FundsLocks
import com.blockchain.core.price.Prices24HrWithDelta
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.balance.percentageDelta
import info.blockchain.balance.total
import java.io.Serializable
import piuk.blockchain.android.domain.usecases.CompletableDashboardOnboardingStep
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.android.ui.dashboard.navigation.DashboardNavigationAction
import piuk.blockchain.android.ui.dashboard.sheets.BackupDetails
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

data class AssetPriceState(
    val assetInfo: AssetInfo,
    val prices: Prices24HrWithDelta? = null
)

class AssetMap(private val map: Map<AssetInfo, CryptoAssetState>) :
    Map<AssetInfo, CryptoAssetState> by map {
    override operator fun get(key: AssetInfo): CryptoAssetState {
        return map.getOrElse(key) {
            throw IllegalArgumentException("$key is not a known CryptoCurrency")
        }
    }

    // TODO: This is horrendously inefficient. Fix it!
    fun copy(): AssetMap {
        val assets = toMutableMap()
        return AssetMap(assets)
    }

    fun copy(patchBalance: AccountBalance): AssetMap {
        val assets = toMutableMap()
        // CURRENCY HERE
        val balance = patchBalance.total as CryptoValue
        val value = get(balance.currency).copy(accountBalance = patchBalance)
        assets[balance.currency] = value
        return AssetMap(assets)
    }

    fun copy(patchAsset: CryptoAssetState): AssetMap {
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
fun mapOfAssets(vararg pairs: Pair<AssetInfo, CryptoAssetState>) = AssetMap(mapOf(*pairs))

interface DashboardItem

interface BalanceState : DashboardItem {
    val isLoading: Boolean
    val fiatBalance: Money?
    val delta: Pair<Money, Double>?
    operator fun get(currency: AssetInfo): CryptoAssetState
    val assetList: List<CryptoAssetState>
    fun getFundsFiat(fiat: FiatCurrency): Money
}

data class FiatBalanceInfo(
    val account: FiatAccount,
    val balance: Money = Money.zero(account.currency),
    val userFiat: Money? = null,
    val availableBalance: Money? = null
)

data class FiatAssetState(
    val fiatAccounts: Map<Currency, FiatBalanceInfo> = emptyMap()
) : DashboardItem {

    fun updateWith(
        currency: Currency,
        balance: FiatValue,
        userFiatBalance: FiatValue,
        availableBalance: Money
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

data class CryptoAssetState(
    val currency: AssetInfo,
    val accountBalance: AccountBalance? = null,
    val prices24HrWithDelta: Prices24HrWithDelta? = null,
    val priceTrend: List<Float> = emptyList(),
    val hasBalanceError: Boolean = false,
    val hasCustodialBalance: Boolean = false,
) : DashboardItem {
    val fiatBalance: Money? by unsafeLazy {
        prices24HrWithDelta?.currentRate?.let { p -> accountBalance?.total?.let { p.convert(it) } }
    }

    val fiatBalance24h: Money? by unsafeLazy {
        prices24HrWithDelta?.previousRate?.let { p -> accountBalance?.total?.let { p.convert(it) } }
    }

    val priceDelta: Double by unsafeLazy {
        prices24HrWithDelta?.delta24h ?: Double.NaN
    }

    val isLoading: Boolean by unsafeLazy {
        if (hasBalanceError)
            false
        else accountBalance == null || prices24HrWithDelta == null
    }

    fun reset(): CryptoAssetState = CryptoAssetState(currency)
}

data class Locks(
    val fundsLocks: FundsLocks? = null
) : DashboardItem, Serializable

sealed class DashboardOnboardingState {
    object Hidden : DashboardOnboardingState()
    data class Visible(val steps: List<CompletableDashboardOnboardingStep>) : DashboardOnboardingState()
}

data class DashboardState(
    val availablePrices: Map<AssetInfo, AssetPriceState> = emptyMap(),
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
    val showedAppRating: Boolean = false
) : MviState, BalanceState {
    val availableAssets = availablePrices.keys.toList()

    // If ALL the assets are refreshing, then report true. Else false
    override val isLoading: Boolean by unsafeLazy {
        activeAssets.values.all { it.isLoading }
    }

    override val fiatBalance: Money? by unsafeLazy {
        addFiatBalance(cryptoAssetFiatBalances())
    }

    private fun cryptoAssetFiatBalances() = activeAssets.values
        .filter { !it.isLoading && it.fiatBalance != null }
        .map { it.fiatBalance!! }
        .ifEmpty { null }?.total()

    private val fiatBalance24h: Money? by unsafeLazy {
        addFiatBalance(cryptoAssetFiatBalances24h())
    }

    private fun cryptoAssetFiatBalances24h() = activeAssets.values
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

    override val delta: Pair<Money, Double>? by unsafeLazy {
        val current = fiatBalance
        val old = fiatBalance24h

        if (current != null && old != null) {
            Pair(current - old, current.percentageDelta(old))
        } else {
            null
        }
    }

    override val assetList: List<CryptoAssetState> = activeAssets.values.toList()

    override operator fun get(currency: AssetInfo): CryptoAssetState =
        activeAssets[currency]

    override fun getFundsFiat(fiat: FiatCurrency): Money =
        fiatAssets.totalBalance ?: Money.zero(fiat)

    val assetMapKeys = activeAssets.keys

    val erc20Assets = assetMapKeys.filter { it.isErc20() }
}
