package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.coincore.AccountBalance
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.Prices24HrWithDelta
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import piuk.blockchain.android.ui.dashboard.model.DashboardItem.Companion.TOTAL_BALANCE_INDEX
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

interface DashboardBalance : DashboardItem {
    val isLoading: Boolean
    val fiatBalance: Money?
    override val index: Int
        get() = TOTAL_BALANCE_INDEX

    override val id: String
        get() = javaClass.toString()
}

data class BrokerageBalanceState(
    override val isLoading: Boolean,
    override val fiatBalance: Money?,
    val assetList: List<DashboardAsset>,
    val fiatAssets: FiatAssetState,
    val delta: Pair<Money, Double>?
) : DashboardBalance {

    fun getFundsFiat(fiat: FiatCurrency): Money =
        fiatAssets.totalBalance ?: Money.zero(fiat)
}

data class DefiBalanceState(
    override val isLoading: Boolean,
    override val fiatBalance: Money?
) : DashboardBalance

sealed interface DashboardAsset : DashboardItem {
    fun updateBalance(accountBalance: AccountBalance): DashboardAsset

    fun toErrorState(): DashboardAsset

    fun updateExchangeRate(rate: ExchangeRate): DashboardAsset

    fun updatePrices24HrWithDelta(prices: Prices24HrWithDelta): DashboardAsset

    fun reset(): DashboardAsset

    val currency: AssetInfo
    val isLoading: Boolean
    val accountBalance: AccountBalance?
    val hasBalanceError: Boolean
    val currentRate: ExchangeRate?

    override val index: Int
        get() = DashboardItem.DASHBOARD_CRYPTO_ASSETS

    override val id: String
        get() = javaClass.toString() + currency.networkTicker + currency.l1chainTicker.orEmpty()

    val fiatBalance: Money?
        get() = currentRate?.let { p -> accountBalance?.total?.let { p.convert(it) } }
}

data class DefiAsset(
    override val currency: AssetInfo,
    override val accountBalance: AccountBalance? = null,
    override val hasBalanceError: Boolean = false,
    override val currentRate: ExchangeRate? = null
) : DashboardAsset {
    override fun updateBalance(accountBalance: AccountBalance): DashboardAsset =
        this.copy(accountBalance = accountBalance, hasBalanceError = false)

    override fun toErrorState(): DashboardAsset = this.copy(hasBalanceError = true)

    override fun updateExchangeRate(rate: ExchangeRate): DashboardAsset = this.copy(
        accountBalance = this.accountBalance?.copy(
            exchangeRate = rate
        ),
        currentRate = rate
    )

    override fun updatePrices24HrWithDelta(prices: Prices24HrWithDelta): DashboardAsset {
        throw IllegalStateException("Defi assets don't support 24HrPrices")
    }

    override fun reset(): DefiAsset = DefiAsset(currency)
    override val isLoading: Boolean
        get() = if (hasBalanceError) false
        else accountBalance == null || currentRate == null
}

data class BrokerageAsset(
    override val currency: AssetInfo,
    override val accountBalance: AccountBalance? = null,
    val prices24HrWithDelta: Prices24HrWithDelta? = null,
    val priceTrend: List<Float> = emptyList(),
    override val hasBalanceError: Boolean = false
) : DashboardAsset {

    override val currentRate: ExchangeRate?
        get() = prices24HrWithDelta?.currentRate

    val fiatBalance24h: Money? by unsafeLazy {
        prices24HrWithDelta?.previousRate?.let { p -> accountBalance?.total?.let { p.convert(it) } }
    }

    val priceDelta: Double by unsafeLazy {
        prices24HrWithDelta?.delta24h ?: Double.NaN
    }

    override val isLoading: Boolean by unsafeLazy {
        if (hasBalanceError)
            false
        else accountBalance == null || prices24HrWithDelta == null
    }

    override fun updateBalance(accountBalance: AccountBalance): DashboardAsset =
        this.copy(accountBalance = accountBalance, hasBalanceError = false)

    override fun toErrorState(): DashboardAsset = this.copy(hasBalanceError = true)

    override fun updateExchangeRate(rate: ExchangeRate): DashboardAsset = this.copy(
        accountBalance = this.accountBalance?.copy(
            exchangeRate = rate
        ),
        prices24HrWithDelta = this.prices24HrWithDelta?.copy(currentRate = rate)
    )

    override fun updatePrices24HrWithDelta(prices: Prices24HrWithDelta) = this.copy(
        accountBalance = this.accountBalance?.copy(
            exchangeRate = prices.currentRate
        ),
        prices24HrWithDelta = prices
    )

    override fun reset(): BrokerageAsset = BrokerageAsset(currency)
}
