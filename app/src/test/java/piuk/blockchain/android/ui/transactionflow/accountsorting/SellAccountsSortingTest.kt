package piuk.blockchain.android.ui.transactionflow.accountsorting

import com.blockchain.coincore.Asset
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.core.custodial.domain.model.TradingAccountBalance
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.data.DataResource
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.transfer.DefaultAccountsSorting
import piuk.blockchain.android.ui.transfer.SellAccountsSorting

class SellAccountsSortingTest {

    private val defaultAccountsSorting: DefaultAccountsSorting = mock()
    private val coincore: Coincore = mock()

    private lateinit var subject: SellAccountsSorting

    @Before
    fun setup() {
        subject = SellAccountsSorting(
            coincore = coincore
        )
    }

    @Test
    fun `when flag is on then accounts are ordered by balance first`() {
        val ethMock = mock<AssetInfo> {
            on { networkTicker }.thenReturn("ETH")
            on { precisionDp }.thenReturn(18)
        }

        val btcMock = mock<AssetInfo> {
            on { networkTicker }.thenReturn("BTC")
            on { precisionDp }.thenReturn(8)
        }

        val ethAsset: Asset = mock {
            on { getPricesWith24hDeltaLegacy() }.thenReturn(
                Single.just(
                    Prices24HrWithDelta(
                        1.0,
                        ExchangeRate(
                            BigDecimal.ONE,
                            CryptoCurrency.ETHER,
                            FiatCurrency.Dollars
                        ),
                        ExchangeRate(
                            BigDecimal.ONE,
                            CryptoCurrency.ETHER,
                            FiatCurrency.Dollars
                        )
                    )
                )
            )
        }

        val btcAsset: Asset = mock {
            on { getPricesWith24hDeltaLegacy() }.thenReturn(
                Single.just(
                    Prices24HrWithDelta(
                        1.0,
                        ExchangeRate(
                            BigDecimal.ONE,
                            CryptoCurrency.BTC,
                            FiatCurrency.Dollars
                        ),
                        ExchangeRate(
                            BigDecimal.ONE,
                            CryptoCurrency.BTC,
                            FiatCurrency.Dollars
                        )
                    )
                )
            )
        }

        whenever(coincore[ethMock]).thenReturn(ethAsset)
        whenever(coincore[btcMock]).thenReturn(btcAsset)

        val ethCustodialAccount = CustodialTradingAccount(
            currency = ethMock,
            label = "ethCustodialAccount",
            exchangeRates = mock {
                on { exchangeRateToUserFiatFlow(CryptoCurrency.ETHER) }.thenReturn(
                    flowOf(DataResource.Data(ExchangeRate(BigDecimal.ONE, CryptoCurrency.ETHER, FiatCurrency.Dollars)))
                )
            },
            custodialWalletManager = mock(),
            tradingService = mock {
                on { getBalanceFor(eq(CryptoCurrency.ETHER), any()) }.thenReturn(
                    Observable.just(
                        TradingAccountBalance(
                            total = Money.fromMinor(CryptoCurrency.ETHER, BigInteger.valueOf(ETH_HIGH_BALANCE)),

                            withdrawable = mock(),
                            pending = mock(),
                            hasTransactions = true
                        )
                    )
                )
            },
            identity = mock(),
            walletModeService = mock(),
            kycService = mock()
        )

        val btcCustodialAccount = CustodialTradingAccount(
            currency = btcMock,
            label = "btcCustodialAccount",
            exchangeRates = mock {
                on { exchangeRateToUserFiatFlow(CryptoCurrency.BTC) }.thenReturn(
                    flowOf(DataResource.Data(ExchangeRate(BigDecimal.ONE, CryptoCurrency.BTC, FiatCurrency.Dollars)))
                )
            },
            custodialWalletManager = mock(),
            tradingService = mock {
                on { getBalanceFor(eq(CryptoCurrency.BTC), any()) }.thenReturn(
                    Observable.just(
                        TradingAccountBalance(
                            total = Money.fromMinor(CryptoCurrency.BTC, BigInteger.valueOf(BTC_HIGH_BALANCE)),

                            withdrawable = mock(),
                            pending = mock(),
                            hasTransactions = true
                        )
                    )
                )
            },
            identity = mock(),
            walletModeService = mock(),
            kycService = mock()
        )

        val list = listOf(
            btcCustodialAccount,
            ethCustodialAccount
        )

        val test = subject.sorter().invoke(list).test()
        test.assertValue {
            it[0] == ethCustodialAccount &&
                it[1] == btcCustodialAccount
        }

        verifyNoMoreInteractions(defaultAccountsSorting)
    }

    @Test
    fun `when multiple accounts they are ordered by balance first and grouped by asset`() {
        val ethMock = mock<AssetInfo> {
            on { networkTicker }.thenReturn("ETH")
            on { precisionDp }.thenReturn(18)
        }

        val btcMock = mock<AssetInfo> {
            on { networkTicker }.thenReturn("BTC")
            on { precisionDp }.thenReturn(8)
        }

        val ethAsset: Asset = mock {
            on { getPricesWith24hDeltaLegacy() }.thenReturn(
                Single.just(
                    Prices24HrWithDelta(
                        1.0,
                        ExchangeRate(
                            BigDecimal.ONE,
                            CryptoCurrency.ETHER,
                            FiatCurrency.Dollars
                        ),
                        ExchangeRate(
                            BigDecimal.ONE,
                            CryptoCurrency.ETHER,
                            FiatCurrency.Dollars
                        )
                    )
                )
            )
        }

        val btcAsset: Asset = mock {
            on { getPricesWith24hDeltaLegacy() }.thenReturn(
                Single.just(
                    Prices24HrWithDelta(
                        1.0,
                        ExchangeRate(
                            BigDecimal.ONE,
                            CryptoCurrency.BTC,
                            FiatCurrency.Dollars
                        ),
                        ExchangeRate(
                            BigDecimal.ONE,
                            CryptoCurrency.BTC,
                            FiatCurrency.Dollars
                        )
                    )
                )
            )
        }

        whenever(coincore[ethMock]).thenReturn(ethAsset)
        whenever(coincore[btcMock]).thenReturn(btcAsset)

        val ethCustodialAccount = CustodialTradingAccount(
            currency = ethMock,
            label = "ethCustodialAccount",
            exchangeRates = mock {
                on { exchangeRateToUserFiatFlow(CryptoCurrency.ETHER) }.thenReturn(
                    flowOf(DataResource.Data(ExchangeRate(BigDecimal.ONE, CryptoCurrency.ETHER, FiatCurrency.Dollars)))
                )
            },
            custodialWalletManager = mock(),
            tradingService = mock {
                on { getBalanceFor(eq(CryptoCurrency.ETHER), any()) }.thenReturn(
                    Observable.just(
                        TradingAccountBalance(
                            total = Money.fromMinor(CryptoCurrency.ETHER, BigInteger.valueOf(ETH_HIGH_BALANCE)),
                            withdrawable = mock(),
                            pending = mock(),
                            hasTransactions = true
                        )
                    )
                )
            },
            identity = mock(),
            walletModeService = mock(),
            kycService = mock()
        )

        val ethCustodialAccount2 = CustodialTradingAccount(
            currency = ethMock,
            label = "ethCustodialAccount2",
            exchangeRates = mock {
                on { exchangeRateToUserFiatFlow(CryptoCurrency.ETHER) }.thenReturn(
                    flowOf(DataResource.Data(ExchangeRate(BigDecimal.ONE, CryptoCurrency.ETHER, FiatCurrency.Dollars)))
                )
            },
            custodialWalletManager = mock(),
            tradingService = mock {
                on { getBalanceFor(eq(CryptoCurrency.ETHER), any()) }.thenReturn(
                    Observable.just(
                        TradingAccountBalance(
                            total = Money.fromMinor(CryptoCurrency.ETHER, BigInteger.valueOf(ETH_LOW_BALANCE)),
                            withdrawable = mock(),
                            pending = mock(),
                            hasTransactions = true
                        )
                    )
                )
            },
            identity = mock(),
            walletModeService = mock(),
            kycService = mock()
        )

        val btcCustodialAccount = CustodialTradingAccount(
            currency = btcMock,
            label = "btcCustodialAccount",
            exchangeRates = mock {
                on { exchangeRateToUserFiatFlow(CryptoCurrency.BTC) }.thenReturn(
                    flowOf(DataResource.Data(ExchangeRate(BigDecimal.ONE, CryptoCurrency.BTC, FiatCurrency.Dollars)))
                )
            },
            custodialWalletManager = mock(),
            tradingService = mock {
                on { getBalanceFor(eq(CryptoCurrency.BTC), any()) }.thenReturn(
                    Observable.just(
                        TradingAccountBalance(
                            total = Money.fromMinor(CryptoCurrency.BTC, BigInteger.valueOf(BTC_HIGH_BALANCE)),
                            withdrawable = mock(),
                            pending = mock(),
                            hasTransactions = true
                        )
                    )
                )
            },
            identity = mock(),
            walletModeService = mock(),
            kycService = mock()
        )

        val btcCustodialAccount2 = CustodialTradingAccount(
            currency = btcMock,
            label = "btcCustodialAccount2",
            exchangeRates = mock {
                on { exchangeRateToUserFiatFlow(CryptoCurrency.BTC) }.thenReturn(
                    flowOf(DataResource.Data(ExchangeRate(BigDecimal.ONE, CryptoCurrency.BTC, FiatCurrency.Dollars)))
                )
            },
            custodialWalletManager = mock(),
            tradingService = mock {
                on { getBalanceFor(eq(CryptoCurrency.BTC), any()) }.thenReturn(
                    Observable.just(
                        TradingAccountBalance(
                            total = Money.fromMinor(CryptoCurrency.BTC, BigInteger.valueOf(BTC_LOW_BALANCE)),
                            withdrawable = mock(),
                            pending = mock(),
                            hasTransactions = true
                        )
                    )
                )
            },
            identity = mock(),
            walletModeService = mock(),
            kycService = mock()
        )

        val list = listOf(
            btcCustodialAccount,
            ethCustodialAccount2,
            ethCustodialAccount,
            btcCustodialAccount2
        )

        val test = subject.sorter().invoke(list).test()
        test.assertValue {
            it[0] == ethCustodialAccount &&
                it[1] == ethCustodialAccount2 &&
                it[2] == btcCustodialAccount &&
                it[3] == btcCustodialAccount2
        }

        verifyNoMoreInteractions(defaultAccountsSorting)
    }

    companion object {
        private const val ETH_HIGH_BALANCE: Long = Long.MAX_VALUE
        private const val ETH_LOW_BALANCE = 5432156746350000000L
        private const val BTC_HIGH_BALANCE = 100000000L
        private const val BTC_LOW_BALANCE = 1000000L
    }
}
