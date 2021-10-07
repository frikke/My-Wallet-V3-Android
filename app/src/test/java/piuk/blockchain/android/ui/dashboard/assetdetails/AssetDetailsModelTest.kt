package piuk.blockchain.android.ui.dashboard.assetdetails

import com.blockchain.android.testutils.rxInit
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.AssetAction
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.HistoricalRate
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.nabu.models.data.RecurringBuyFrequency
import com.blockchain.nabu.models.data.RecurringBuyState
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Rule
import org.junit.Test
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.impl.CryptoInterestAccount
import org.junit.Before
import piuk.blockchain.android.ui.dashboard.model.FIAT_CURRENCY
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class AssetDetailsModelTest {

    private val defaultState = AssetDetailsState(
        asset = mock()
    )

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    private val interactor: AssetDetailsInteractor = mock()

    private lateinit var subject: AssetDetailsModel

    @Before
    fun setUp() {
        subject = AssetDetailsModel(
            initialState = defaultState,
            mainScheduler = Schedulers.io(),
            interactor = interactor,
            environmentConfig = environmentConfig,
            crashLogger = mock()
        )
    }

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    @Test
    fun `load asset success`() {
        val assetDisplayMap = mapOf(
            AssetFilter.Custodial to AssetDisplayInfo(
                account = mock(),
                amount = mock(),
                pendingAmount = mock(),
                fiatValue = mock(),
                actions = emptySet()
            )
        )
        val recurringBuy = RecurringBuy(
            id = "123",
            state = RecurringBuyState.ACTIVE,
            recurringBuyFrequency = RecurringBuyFrequency.BI_WEEKLY,
            nextPaymentDate = mock(),
            paymentMethodType = PaymentMethodType.BANK_TRANSFER,
            paymentMethodId = "321",
            amount = FiatValue.zero("EUR"),
            asset = mock(),
            createDate = mock()
        )
        val recurringBuys: List<RecurringBuy> = listOf(
            recurringBuy
        )
        val expectedRecurringBuyMap = mapOf(
            "123" to recurringBuy
        )

        val priceSeries = listOf<HistoricalRate>(mock())
        val asset: CryptoAsset = mock {
            on { asset }.thenReturn(CryptoCurrency.BTC)
        }

        val expectedDeltaDetails = Prices24HrWithDelta(
            previousRate = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, FIAT_CURRENCY, 400.toBigDecimal()),
            currentRate = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, FIAT_CURRENCY, 400.toBigDecimal()),
            delta24h = 0.0
        )

        val timeSpan = HistoricalTimeSpan.DAY

        whenever(interactor.loadAssetDetails(asset)).thenReturn(Single.just(assetDisplayMap))
        whenever(interactor.loadHistoricPrices(asset, timeSpan)).thenReturn(Single.just(priceSeries))
        whenever(interactor.loadRecurringBuysForAsset(asset.asset)).thenReturn(Single.just(recurringBuys))
        whenever(interactor.load24hPriceDelta(asset.asset)).thenReturn(Single.just(expectedDeltaDetails))

        val stateTest = subject.state.test()

        subject.process(LoadAsset(asset))

        stateTest
            .assertValueAt(0) {
                it == defaultState
            }.assertValueAt(1) {
                it == defaultState.copy(asset = asset, assetDisplayMap = mapOf())
            }.assertValueAt(2) {
                it == defaultState.copy(
                    asset = asset,
                    assetDisplayMap = mapOf(),
                    chartLoading = true
                )
            }.assertValueAt(3) {
                it == defaultState.copy(
                    asset = asset,
                    assetDisplayMap = mapOf(),
                    chartData = priceSeries,
                    chartLoading = false
                )
            }.assertValueAt(4) {
                it == defaultState.copy(
                    asset = asset,
                    assetDisplayMap = mapOf(),
                    chartData = priceSeries,
                    chartLoading = false,
                    prices24HrWithDelta = expectedDeltaDetails
                )
            }.assertValueAt(5) {
                val expected = defaultState.copy(
                    asset = asset,
                    chartData = priceSeries,
                    chartLoading = false,
                    assetDisplayMap = assetDisplayMap,
                    prices24HrWithDelta = expectedDeltaDetails
                )
                it == expected
            }.assertValueAt(6) {
                val expected = defaultState.copy(
                    asset = asset,
                    chartData = priceSeries,
                    chartLoading = false,
                    assetDisplayMap = assetDisplayMap,
                    recurringBuys = expectedRecurringBuyMap,
                    prices24HrWithDelta = expectedDeltaDetails
                )
                it == expected
            }

        verify(interactor).loadAssetDetails(asset)
        verify(interactor).loadRecurringBuysForAsset(asset.asset)
        verify(interactor).loadHistoricPrices(asset, timeSpan)
        verify(interactor).load24hPriceDelta(asset.asset)

        verifyNoMoreInteractions(interactor)
    }

    @Test
    fun `given interest account, on show asset actions success the InterestDeposit action should be added`() {
        val interestAccount: CryptoInterestAccount = mock()
        val accountGroup: AccountGroup = mock {
            on { accounts }.thenReturn(listOf(interestAccount))
        }

        whenever(accountGroup.actions).thenReturn(Single.just(emptySet()))

        val stateTest = subject.state.test()

        subject.process(ShowAssetActionsIntent(accountGroup))

        stateTest
            .assertValueAt(0) { it == defaultState }
            .assertValueAt(1) { it.actions.contains(AssetAction.InterestDeposit) }
    }

    @Test
    fun `on show asset actions success the actions should be sorted correctly`() {
        val account: CryptoAccount = mock()
        val accountGroup: AccountGroup = mock {
            on { accounts }.thenReturn(listOf(account))
        }

        val actions = listOf(
            AssetAction.Receive,
            AssetAction.ViewStatement,
            AssetAction.Sell,
            AssetAction.Buy,
            AssetAction.Withdraw,
            AssetAction.ViewActivity,
            AssetAction.Swap,
            AssetAction.FiatDeposit,
            AssetAction.Send,
        ).toSet()
        whenever(accountGroup.actions).thenReturn(Single.just(actions))

        val stateTest = subject.state.test()

        subject.process(ShowAssetActionsIntent(accountGroup))

        stateTest
            .assertValueAt(0, defaultState)
            .assertValueAt(1) {
                val expected = listOf(
                    AssetAction.Buy,
                    AssetAction.Sell,
                    AssetAction.Swap,
                    AssetAction.Send,
                    AssetAction.Receive,
                    AssetAction.FiatDeposit,
                    AssetAction.Withdraw,
                    AssetAction.ViewStatement,
                    AssetAction.ViewActivity,
                )

                it.actions.toList() == expected
            }
    }

}