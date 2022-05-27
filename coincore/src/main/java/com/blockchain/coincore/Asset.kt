package com.blockchain.coincore

import android.os.Parcelable
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.FeatureAccess
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import kotlinx.parcelize.Parcelize

enum class AssetFilter {
    All,
    NonCustodial,
    Custodial,
    Interest
}

enum class ActionOrigin {
    FROM_SOURCE,
    TO_TARGET
}

enum class AssetAction(
    val origin: ActionOrigin
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
    Withdraw(ActionOrigin.FROM_SOURCE),

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
    Sign(ActionOrigin.FROM_SOURCE)
}

@Parcelize
data class StateAwareAction(
    val state: ActionState,
    val action: AssetAction
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
    predicate: (AssetAction) -> Boolean = { true }
): AssetAction? =
    this.takeIf { it in baseActions && predicate(this) }

internal fun FeatureAccess.Blocked.toActionState(): ActionState = when (val reason = reason) {
    is BlockedReason.InsufficientTier -> ActionState.LockedForTier
    is BlockedReason.Sanctions -> ActionState.LockedDueToSanctions(reason)
    BlockedReason.NotEligible -> ActionState.Unavailable
    is BlockedReason.TooManyInFlightTransactions -> ActionState.Unavailable
}

interface Asset {
    fun accountGroup(filter: AssetFilter = AssetFilter.All): Maybe<AccountGroup>
    fun transactionTargets(account: SingleAccount): Single<SingleAccountList>
    fun parseAddress(address: String, label: String? = null, isDomainAddress: Boolean = false): Maybe<ReceiveAddress>
    fun isValidAddress(address: String): Boolean = false
}

interface CryptoAsset : Asset {

    fun defaultAccount(): Single<SingleAccount>
    fun interestRate(): Single<Double>

    // Fetch exchange rate to user's selected/display fiat
    @Deprecated("Use getPricesWith24hDelta() instead")
    fun exchangeRate(): Single<ExchangeRate>
    fun getPricesWith24hDelta(): Single<Prices24HrWithDelta>
    fun historicRate(epochWhen: Long): Single<ExchangeRate>

    fun historicRateSeries(period: HistoricalTimeSpan): Single<HistoricalRateList>
    fun lastDayTrend(): Single<HistoricalRateList>

    // Temp feature accessors - this will change, but until it's building these have to be somewhere
    val isCustodialOnly: Boolean
    val multiWallet: Boolean
    val assetInfo: AssetInfo
}

internal interface NonCustodialSupport {
    fun initToken(): Completable
}
