package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.Prices24HrWithDelta
import com.nhaarman.mockitokotlin2.mock
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import org.mockito.Mock
import com.blockchain.coincore.FiatAccount
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

const val FIAT_CURRENCY = "USD"

val TEST_ASSETS = listOf(
    CryptoCurrency.BTC,
    CryptoCurrency.ETHER,
    CryptoCurrency.XLM
)

private val pricesWith24HrBtc = Prices24HrWithDelta(
    previousRate = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, FIAT_CURRENCY, 400.toBigDecimal()),
    currentRate = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, FIAT_CURRENCY, 300.toBigDecimal()),
    delta24h = 0.0
)

val initialBtcState = CryptoAssetState(
    currency = CryptoCurrency.BTC,
    accountBalance = mock {
        on { total }.thenReturn(CryptoValue.zero(CryptoCurrency.BTC))
        on { actionable }.thenReturn(CryptoValue.zero(CryptoCurrency.BTC))
        on { pending }.thenReturn(CryptoValue.zero(CryptoCurrency.BTC))
        on { exchangeRate }.thenReturn(
            ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, FIAT_CURRENCY, 300.toBigDecimal())
        )
    },
    prices24HrWithDelta = pricesWith24HrBtc,
    priceTrend = emptyList()
)

val initialEthState = CryptoAssetState(
    currency = CryptoCurrency.ETHER,
    accountBalance = mock {
        on { total }.thenReturn(CryptoValue.zero(CryptoCurrency.ETHER))
        on { actionable }.thenReturn(CryptoValue.zero(CryptoCurrency.ETHER))
        on { pending }.thenReturn(CryptoValue.zero(CryptoCurrency.ETHER))
        on { exchangeRate }.thenReturn(
            ExchangeRate.CryptoToFiat(CryptoCurrency.ETHER, FIAT_CURRENCY, 200.toBigDecimal())
        )
    },
    prices24HrWithDelta = mock(),
    priceTrend = emptyList()
)

val initialXlmState = CryptoAssetState(
    currency = CryptoCurrency.XLM,
    accountBalance = mock {
        on { total }.thenReturn(CryptoValue.zero(CryptoCurrency.XLM))
        on { actionable }.thenReturn(CryptoValue.zero(CryptoCurrency.XLM))
        on { pending }.thenReturn(CryptoValue.zero(CryptoCurrency.XLM))
        on { exchangeRate }.thenReturn(
            ExchangeRate.CryptoToFiat(CryptoCurrency.XLM, FIAT_CURRENCY, 100.toBigDecimal())
        )
    },
    prices24HrWithDelta = mock(),
    priceTrend = emptyList()
)

val testAnnouncementCard_1 = StandardAnnouncementCard(
    name = "test_1",
    dismissRule = DismissRule.CardOneTime,
    dismissEntry = mock()
)

val testAnnouncementCard_2 = StandardAnnouncementCard(
    name = "test_2",
    dismissRule = DismissRule.CardOneTime,
    dismissEntry = mock()
)

val testBtcState = CryptoAssetState(
    currency = CryptoCurrency.BTC,
    accountBalance = mock {
        on { total }.thenReturn(CryptoValue.fromMajor(CryptoCurrency.BTC, 10.toBigDecimal()))
        on { actionable }.thenReturn(CryptoValue.fromMajor(CryptoCurrency.BTC, 10.toBigDecimal()))
        on { pending }.thenReturn(CryptoValue.fromMajor(CryptoCurrency.BTC, 10.toBigDecimal()))
    },
    prices24HrWithDelta = pricesWith24HrBtc,
    priceTrend = emptyList()
)

val testFiatBalance = FiatValue.fromMajor(FIAT_CURRENCY, 1000.toBigDecimal())

@Mock
private val fiatAccount: FiatAccount = mock()
val fiatAssetState_1 = FiatAssetState()
val fiatAssetState_2 = FiatAssetState(
    mapOf(
        testFiatBalance.currencyCode to
            FiatBalanceInfo(
                account = fiatAccount,
                balance = testFiatBalance,
                userFiat = testFiatBalance
            )
        )
)

val initialState = PortfolioState(
    assets = mapOfAssets(
        CryptoCurrency.BTC to initialBtcState,
        CryptoCurrency.ETHER to initialEthState,
        CryptoCurrency.XLM to initialXlmState
    ),
    announcement = null
)
