package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.FiatAccount
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.utils.unsafeLazy
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import piuk.blockchain.android.ui.dashboard.model.DashboardItem.Companion.DASHBOARD_FIAT_ASSETS
import piuk.blockchain.android.ui.dashboard.model.DashboardItem.Companion.TOTAL_BALANCE_INDEX

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
    val delta: Pair<Money, Double>?
) : DashboardBalance

data class DefiBalanceState(
    override val isLoading: Boolean,
    override val fiatBalance: Money?
) : DashboardBalance

sealed interface DashboardAsset : DashboardItem {
    fun updateBalance(accountBalance: AccountBalance): DashboardAsset

    fun toErrorState(): DashboardAsset
    fun updateFetchingBalanceState(isFetching: Boolean): DashboardAsset

    fun updateExchangeRate(rate: ExchangeRate): DashboardAsset

    fun updatePrices24HrWithDelta(prices: Prices24HrWithDelta): DashboardAsset

    fun reset(): DashboardAsset

    val currency: Currency

    val isUILoading: Boolean
    val isFetchingBalance: Boolean
    val accountBalance: AccountBalance?
    val hasBalanceError: Boolean
    val currentRate: ExchangeRate?

    override val index: Int
        get() = DashboardItem.DASHBOARD_CRYPTO_ASSETS

    override val id: String
        get() = javaClass.toString() + currency.networkTicker + (currency as? AssetInfo)?.l1chainTicker.orEmpty()

    val fiatBalance: Money?
        get() = currentRate?.let { p -> accountBalance?.total?.let { p.convert(it) } }
}

data class DefiAsset(
    override val currency: AssetInfo,
    override val accountBalance: AccountBalance? = null,
    override val hasBalanceError: Boolean = false,
    override val isFetchingBalance: Boolean = false,
    override val currentRate: ExchangeRate? = null
) : DashboardAsset {
    override fun updateBalance(accountBalance: AccountBalance): DashboardAsset =
        this.copy(accountBalance = accountBalance, hasBalanceError = false, isFetchingBalance = false)

    override fun toErrorState(): DashboardAsset = this.copy(hasBalanceError = true)
    override fun updateFetchingBalanceState(isFetching: Boolean): DashboardAsset =
        this.copy(isFetchingBalance = isFetching)

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
    override val isUILoading: Boolean
        get() = if (hasBalanceError) false
        else accountBalance == null || currentRate == null
}

sealed interface BrokerageDashboardAsset : DashboardAsset {
    override val currency: Currency
}

data class BrokerageCryptoAsset(
    override val currency: AssetInfo,
    override val accountBalance: AccountBalance? = null,
    val prices24HrWithDelta: Prices24HrWithDelta? = null,
    val priceTrend: List<Float> = emptyList(),
    override val hasBalanceError: Boolean = false,
    override val isFetchingBalance: Boolean = false,
) : BrokerageDashboardAsset {

    override val currentRate: ExchangeRate?
        get() = prices24HrWithDelta?.currentRate

    val fiatBalance24h: Money? by unsafeLazy {
        prices24HrWithDelta?.previousRate?.let { p -> accountBalance?.total?.let { p.convert(it) } }
    }

    val priceDelta: Double by unsafeLazy {
        prices24HrWithDelta?.delta24h ?: Double.NaN
    }

    override fun updateFetchingBalanceState(isFetching: Boolean): DashboardAsset =
        this.copy(isFetchingBalance = isFetching)
    override val isUILoading: Boolean by unsafeLazy {
        if (hasBalanceError)
            false
        else accountBalance == null || prices24HrWithDelta == null
    }

    override fun updateBalance(accountBalance: AccountBalance): DashboardAsset =
        this.copy(accountBalance = accountBalance, hasBalanceError = false, isFetchingBalance = false)

    override fun toErrorState(): DashboardAsset = this.copy(hasBalanceError = true, isFetchingBalance = false)

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

    override fun reset(): BrokerageCryptoAsset = BrokerageCryptoAsset(currency)
}

data class BrokearageFiatAsset(
    override val currency: Currency,
    override val accountBalance: AccountBalance? = null,
    override val currentRate: ExchangeRate? = null,
    override val isUILoading: Boolean = false,
    override val isFetchingBalance: Boolean = false,
    override val hasBalanceError: Boolean = false,
    /**
     * unfortunately, we need to know this cause click is coupled with this.
     */
    val fiatAccount: FiatAccount
) : BrokerageDashboardAsset {

    override val index: Int
        get() = DASHBOARD_FIAT_ASSETS
    override val id: String
        get() = super.id

    override fun updateBalance(accountBalance: AccountBalance): DashboardAsset =
        this.copy(accountBalance = accountBalance, hasBalanceError = false, isFetchingBalance = false)

    override fun updateFetchingBalanceState(isFetching: Boolean): DashboardAsset =
        this.copy(isFetchingBalance = isFetching)

    override fun toErrorState(): DashboardAsset =
        this.copy(hasBalanceError = true, isFetchingBalance = false)

    override fun updateExchangeRate(rate: ExchangeRate): DashboardAsset = this.copy(
        accountBalance = this.accountBalance?.copy(
            exchangeRate = rate
        ),
        currentRate = rate
    )

    override fun updatePrices24HrWithDelta(prices: Prices24HrWithDelta): DashboardAsset {
        throw IllegalStateException("Action not supported")
    }

    override fun reset(): DashboardAsset = BrokearageFiatAsset(currency = currency, fiatAccount = fiatAccount)
}
