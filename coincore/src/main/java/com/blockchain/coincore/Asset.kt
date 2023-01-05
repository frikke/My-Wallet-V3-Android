package com.blockchain.coincore

import android.os.Parcelable
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.FeatureAccess
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.ExchangeRate
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize

enum class AssetFilter {
    All,
    NonCustodial,
    Trading,
    Interest,
    Staking,
    Custodial // Trading + Interest + Staking (Accounts held by Blockchain.com)
}

fun WalletMode.defaultFilter(): AssetFilter =
    when (this) {
        WalletMode.NON_CUSTODIAL -> AssetFilter.NonCustodial
        WalletMode.CUSTODIAL -> AssetFilter.Custodial
    }

enum class ActionOrigin {
    FROM_SOURCE,
    TO_TARGET
}

enum class AssetAction(
    val origin: ActionOrigin,
) {
    // Display account activity
    ViewActivity(ActionOrigin.FROM_SOURCE),

    // View account statement
    ViewStatement(ActionOrigin.FROM_SOURCE),

    // Transfer from account to account for the same crypto asset
    Send(ActionOrigin.FROM_SOURCE),

    // Transfer from account to account for different crypto assets
    Swap(ActionOrigin.FROM_SOURCE),

    // Crypto to fiat
    Sell(ActionOrigin.FROM_SOURCE),

    // Fiat to crypto
    Buy(ActionOrigin.TO_TARGET),

    // Fiat to external
    FiatWithdraw(ActionOrigin.FROM_SOURCE),

    // Receive crypto to crypto
    Receive(ActionOrigin.TO_TARGET),

    // From a source account to a defined Target
    // Deposit(ActionOrigin.TO_TARGET), // TODO: Not yet implemented
    // TODO Currently these final few are defined on the source and munged in the UI. FIXME
    // There may be still be merit in defining these separately for crypto and fiat, as we
    // do with the flavours of SEND
    // Send TO interest account. Really a send?
    // We should have a Generic WITHDRAW and DEPOSIT to include those
    InterestDeposit(ActionOrigin.FROM_SOURCE),

    // Interest TO any crypto of same asset. Really a send?
    InterestWithdraw(ActionOrigin.FROM_SOURCE),

    // External fiat to custodial fiat
    FiatDeposit(ActionOrigin.FROM_SOURCE),

    // Receive crypto to crypto
    Sign(ActionOrigin.FROM_SOURCE),

    // Receive to a Staking account
    StakingDeposit(ActionOrigin.FROM_SOURCE);
}

@Parcelize
data class StateAwareAction(
    val state: ActionState,
    val action: AssetAction,
) : java.io.Serializable, Parcelable

sealed class ActionState : Parcelable {
    @Parcelize
    object Available : ActionState()

    @Parcelize
    object LockedForBalance : ActionState()

    @Parcelize
    object LockedForTier : ActionState()

    @Parcelize
    data class LockedDueToSanctions(val reason: BlockedReason.Sanctions) : ActionState()

    @Parcelize
    object LockedDueToAvailability : ActionState()

    @Parcelize
    object Unavailable : ActionState()
}

typealias AvailableActions = Set<AssetAction>

internal inline fun AssetAction.takeEnabledIf(
    baseActions: AvailableActions,
    predicate: (AssetAction) -> Boolean = { true },
): AssetAction? =
    this.takeIf { it in baseActions && predicate(this) }

internal fun FeatureAccess.Blocked.toActionState(): ActionState = when (val reason = reason) {
    is BlockedReason.InsufficientTier -> ActionState.LockedForTier
    is BlockedReason.Sanctions -> ActionState.LockedDueToSanctions(reason)
    is BlockedReason.NotEligible -> ActionState.Unavailable
    is BlockedReason.TooManyInFlightTransactions -> ActionState.Unavailable
    is BlockedReason.ShouldAcknowledgeStakingWithdrawal -> ActionState.Unavailable
}

interface Asset {
    val currency: Currency
    fun defaultAccount(filter: AssetFilter): Single<SingleAccount>
    fun accountGroup(filter: AssetFilter = AssetFilter.All): Maybe<AccountGroup>
    fun transactionTargets(account: SingleAccount): Single<SingleAccountList>
    fun parseAddress(address: String, label: String? = null, isDomainAddress: Boolean = false): Maybe<ReceiveAddress>
    fun isValidAddress(address: String): Boolean = false
    fun lastDayTrend(): Flow<DataResource<HistoricalRateList>>

    // Fetch exchange rate to user's selected/display fiat
    @Deprecated("Use getPricesWith24hDelta() instead")
    fun exchangeRate(): Single<ExchangeRate>

    @Deprecated("use flow")
    fun getPricesWith24hDeltaLegacy(): Single<Prices24HrWithDelta>
    fun historicRate(epochWhen: Long): Single<ExchangeRate>

    // flow
    fun getPricesWith24hDelta(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<Prices24HrWithDelta>>

    fun historicRateSeries(
        period: HistoricalTimeSpan,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<HistoricalRateList>>
}

interface CryptoAsset : Asset {
    override val currency: AssetInfo
    fun interestRate(): Single<Double>
    fun stakingRate(): Single<Double>
}

interface MultipleWalletsAsset {
    val currency: AssetInfo
    fun createWalletFromLabel(label: String, secondPassword: String?): Single<out SingleAccount>
    fun createWalletFromAddress(address: String): Completable
    fun importWalletFromKey(
        keyData: String,
        keyFormat: String,
        keyPassword: String? = null, // Required for BIP38 format keys
        walletSecondPassword: String? = null,
    ): Single<out SingleAccount>
}

internal interface NonCustodialSupport {
    fun initToken(): Completable
}
