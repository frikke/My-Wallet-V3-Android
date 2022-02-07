package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.coincore.FiatAccount
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.testutils.USD
import com.nhaarman.mockitokotlin2.mock
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import org.mockito.Mock
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

val FIAT_CURRENCY = USD

private val pricesWith24HrBtc = Prices24HrWithDelta(
    previousRate = ExchangeRate(400.toBigDecimal(), CryptoCurrency.BTC, FIAT_CURRENCY),
    currentRate = ExchangeRate(300.toBigDecimal(), CryptoCurrency.BTC, FIAT_CURRENCY),
    delta24h = 0.0
)

val initialBtcState = CryptoAssetState(
    currency = CryptoCurrency.BTC,
    accountBalance = mock {
        on { total }.thenReturn(CryptoValue.zero(CryptoCurrency.BTC))
        on { withdrawable }.thenReturn(CryptoValue.zero(CryptoCurrency.BTC))
        on { pending }.thenReturn(CryptoValue.zero(CryptoCurrency.BTC))
        on { exchangeRate }.thenReturn(
            ExchangeRate(300.toBigDecimal(), FIAT_CURRENCY, CryptoCurrency.BTC)
        )
    },
    prices24HrWithDelta = pricesWith24HrBtc,
    priceTrend = emptyList()
)

val initialEthState = CryptoAssetState(
    currency = CryptoCurrency.ETHER,
    accountBalance = mock {
        on { total }.thenReturn(CryptoValue.zero(CryptoCurrency.ETHER))
        on { withdrawable }.thenReturn(CryptoValue.zero(CryptoCurrency.ETHER))
        on { pending }.thenReturn(CryptoValue.zero(CryptoCurrency.ETHER))
        on { exchangeRate }.thenReturn(
            ExchangeRate(200.toBigDecimal(), FIAT_CURRENCY, CryptoCurrency.ETHER)
        )
    },
    prices24HrWithDelta = mock(),
    priceTrend = emptyList()
)

val initialXlmState = CryptoAssetState(
    currency = CryptoCurrency.XLM,
    accountBalance = mock {
        on { total }.thenReturn(CryptoValue.zero(CryptoCurrency.XLM))
        on { withdrawable }.thenReturn(CryptoValue.zero(CryptoCurrency.XLM))
        on { pending }.thenReturn(CryptoValue.zero(CryptoCurrency.XLM))
        on { exchangeRate }.thenReturn(
            ExchangeRate(100.toBigDecimal(), FIAT_CURRENCY, CryptoCurrency.XLM)
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
        on { withdrawable }.thenReturn(CryptoValue.fromMajor(CryptoCurrency.BTC, 10.toBigDecimal()))
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
        testFiatBalance.currency to
            FiatBalanceInfo(
                account = fiatAccount,
                balance = testFiatBalance,
                userFiat = testFiatBalance
            )
    )
)

val initialState = DashboardState(
    activeAssets = mapOfAssets(
        CryptoCurrency.BTC to initialBtcState,
        CryptoCurrency.ETHER to initialEthState,
        CryptoCurrency.XLM to initialXlmState
    ),
    announcement = null
)
