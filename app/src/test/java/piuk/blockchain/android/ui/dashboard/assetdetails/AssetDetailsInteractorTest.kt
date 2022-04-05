package piuk.blockchain.android.ui.dashboard.assetdetails

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.CryptoAsset
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.HistoricalRate
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.testutils.USD
import com.blockchain.testutils.rxInit
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.Locale
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AssetDetailsInteractorTest {

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    private val totalGroup: AccountGroup = mock()
    private val nonCustodialGroup: AccountGroup = mock()
    private val custodialGroup: AccountGroup = mock()
    private val interestGroup: AccountGroup = mock()
    private val interestRate: Double = 5.0

    private val asset: CryptoAsset = mock {
        on { accountGroup(AssetFilter.All) }.thenReturn(Maybe.just(totalGroup))
        on { accountGroup(AssetFilter.NonCustodial) }.thenReturn(Maybe.just(nonCustodialGroup))
        on { accountGroup(AssetFilter.Custodial) }.thenReturn(Maybe.just(custodialGroup))
        on { accountGroup(AssetFilter.Interest) }.thenReturn(Maybe.just(interestGroup))
    }

    private val userIdentity = mock<UserIdentity>()
    private val custodialWalletManager = mock<CustodialWalletManager>()

    private val subject = AssetDetailsInteractor(mock(), mock(), userIdentity, custodialWalletManager, mock())

    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun `cryptoBalance,fiatBalance & interestBalance return the right values`() {
        val currentRate = ExchangeRate(30.toBigDecimal(), TEST_ASSET, TEST_FIAT)

        val prices = Prices24HrWithDelta(
            currentRate = currentRate,
            previousRate = ExchangeRate(15.toBigDecimal(), TEST_ASSET, TEST_FIAT),
            delta24h = 100.0
        )

        val walletBalance = mock<AccountBalance> {
            on { total }.thenReturn(CryptoValue(TEST_ASSET, 2500.toBigInteger()))
            on { withdrawable }.thenReturn(CryptoValue(TEST_ASSET, 2500.toBigInteger()))
            on { pending }.thenReturn(CryptoValue.zero(TEST_ASSET))
            on { exchangeRate }.thenReturn(currentRate)
        }
        val custodialBalance = mock<AccountBalance> {
            on { total }.thenReturn(CryptoValue.zero(TEST_ASSET))
            on { withdrawable }.thenReturn(CryptoValue.zero(TEST_ASSET))
            on { pending }.thenReturn(CryptoValue.zero(TEST_ASSET))
            on { exchangeRate }.thenReturn(currentRate)
        }
        val interestBalance = mock<AccountBalance> {
            on { total }.thenReturn(CryptoValue.zero(TEST_ASSET))
            on { withdrawable }.thenReturn(CryptoValue.zero(TEST_ASSET))
            on { pending }.thenReturn(CryptoValue.zero(TEST_ASSET))
            on { exchangeRate }.thenReturn(currentRate)
        }

        val walletFiat = FiatValue.fromMinor(USD, (2500 * 30).toBigInteger())
        val custodialFiat = FiatValue.fromMinor(USD, 0.toBigInteger())
        val interestFiat = FiatValue.fromMinor(USD, 0.toBigInteger())

        val expectedResult = mapOf(
            AssetFilter.NonCustodial to AssetDisplayInfo(
                nonCustodialGroup,
                walletBalance.total,
                walletBalance.pending,
                walletFiat,
                emptySet()
            ),
            AssetFilter.Custodial to AssetDisplayInfo(
                custodialGroup,
                custodialBalance.total,
                custodialBalance.pending,
                custodialFiat,
                emptySet()
            ),
            AssetFilter.Interest to AssetDisplayInfo(
                interestGroup,
                interestBalance.total,
                interestBalance.pending,
                interestFiat,
                emptySet(),
                interestRate
            )
        )

        whenever(asset.getPricesWith24hDelta()).thenReturn(Single.just(prices))

        whenever(nonCustodialGroup.balance).thenReturn(Observable.just(walletBalance))
        whenever(nonCustodialGroup.isEnabled).thenReturn(Single.just(true))
        whenever(nonCustodialGroup.actions).thenReturn(Single.just(emptySet()))

        whenever(custodialGroup.balance).thenReturn(Observable.just(custodialBalance))
        whenever(custodialGroup.isEnabled).thenReturn(Single.just(true))
        whenever(custodialGroup.actions).thenReturn(Single.just(emptySet()))

        whenever(interestGroup.balance).thenReturn(Observable.just(interestBalance))
        whenever(interestGroup.isEnabled).thenReturn(Single.just(true))
        whenever(interestGroup.actions).thenReturn(Single.just(emptySet()))

        whenever(asset.interestRate()).thenReturn(Single.just(interestRate))

        whenever(custodialGroup.accounts).thenReturn(listOf(mock()))
        whenever(nonCustodialGroup.accounts).thenReturn(listOf(mock()))
        whenever(custodialGroup.isFunded).thenReturn(true)
        whenever(nonCustodialGroup.isFunded).thenReturn(true)

        whenever(interestGroup.accounts).thenReturn(listOf(mock()))
        whenever(interestGroup.isFunded).thenReturn(true)

        subject.loadAssetDetails(asset)
            .test()
            .assertValueCount(1)
            .assertValueAt(0) {
                it == expectedResult
            }
    }

    @Test
    fun `cryptoBalance, fiatBalance & interestBalance are never returned if exchange rate fails`() {
        whenever(asset.getPricesWith24hDelta()).thenReturn(Single.error(Throwable()))

        val nonCustodialGroupBalance: AccountBalance = mock {
            on { total }.thenReturn(CryptoValue(TEST_ASSET, 548621.toBigInteger()))
            on { withdrawable }.thenReturn(CryptoValue.zero(TEST_ASSET))
            on { pending }.thenReturn(CryptoValue.zero(TEST_ASSET))
        }
        val custodialCrypto = AccountBalance.zero(TEST_ASSET)
        val interestCrypto = AccountBalance.zero(TEST_ASSET)

        whenever(nonCustodialGroup.balance).thenReturn(Observable.just(nonCustodialGroupBalance))
        whenever(custodialGroup.balance).thenReturn(Observable.just(custodialCrypto))
        whenever(interestGroup.balance).thenReturn(Observable.just(interestCrypto))
        whenever(asset.interestRate()).thenReturn(Single.just(interestRate))

        subject.loadAssetDetails(asset)
            .test()
            .assertNoValues()
    }

    @Test
    fun `cryptoBalance & fiatBalance never return if interest fails`() {
        val nonCustodialGroupBalance: AccountBalance = mock {
            on { total }.thenReturn(CryptoValue(TEST_ASSET, 548621.toBigInteger()))
            on { withdrawable }.thenReturn(CryptoValue.zero(TEST_ASSET))
            on { pending }.thenReturn(CryptoValue.zero(TEST_ASSET))
        }
        val custodialCrypto = AccountBalance.zero(TEST_ASSET)

        val prices = Prices24HrWithDelta(
            currentRate = ExchangeRate(5647899.toBigDecimal(), TEST_ASSET, TEST_FIAT),
            previousRate = ExchangeRate(564789.toBigDecimal(), TEST_ASSET, TEST_FIAT),
            delta24h = 1000.0
        )

        whenever(asset.getPricesWith24hDelta()).thenReturn(Single.just(prices))
        whenever(asset.accountGroup(AssetFilter.Interest)).thenReturn(Maybe.error(Throwable()))

        whenever(nonCustodialGroup.balance).thenReturn(Observable.just(nonCustodialGroupBalance))
        whenever(custodialGroup.balance).thenReturn(Observable.just(custodialCrypto))
        whenever(asset.interestRate()).thenReturn(Single.just(interestRate))

        subject.loadAssetDetails(asset)
            .test()
            .assertNoValues()
    }

    @Test
    fun `historic prices are returned properly`() {
        val ratesList = listOf(
            HistoricalRate(5556, 2.toDouble()),
            HistoricalRate(587, 22.toDouble()),
            HistoricalRate(6981, 23.toDouble())
        )

        whenever(asset.historicRateSeries(HistoricalTimeSpan.DAY))
            .thenReturn(Single.just(ratesList))

        subject.loadHistoricPrices(asset, HistoricalTimeSpan.DAY)
            .test()
            .assertValue { it == ratesList }
            .assertValueCount(1)
            .assertNoErrors()
    }

    @Test
    fun `when historic prices api returns error, empty list should be returned`() {
        whenever(asset.historicRateSeries(HistoricalTimeSpan.DAY))
            .thenReturn(Single.error(Throwable()))

        subject.loadHistoricPrices(asset, HistoricalTimeSpan.DAY)
            .test()
            .assertValue { it.isEmpty() }
            .assertValueCount(1)
            .assertNoErrors()
    }

    @Test
    fun `CheckBuyStatus then can userCanBuy and isAssetSupportedToBuy`() {
        val asset: AssetInfo = mock {
            on { networkTicker }.thenReturn("BTC")
        }

        whenever(userIdentity.userAccessForFeature(Feature.SimpleBuy)).thenReturn(Single.just(FeatureAccess.Granted()))
        whenever(custodialWalletManager.isCurrencyAvailableForTrading(asset))
            .thenReturn(Single.just(true))

        subject.userCanBuy().test()
        subject.isAssetSupportedToBuy(asset).test()

        verify(userIdentity).userAccessForFeature(Feature.SimpleBuy)
        verify(custodialWalletManager).isCurrencyAvailableForTrading(asset)

        verifyNoMoreInteractions(userIdentity)
        verifyNoMoreInteractions(custodialWalletManager)
    }

    companion object {
        private val TEST_FIAT = USD

        private val TEST_ASSET = object : CryptoCurrency(
            displayTicker = "NOPE",
            networkTicker = "NOPE",
            name = "Not a real thing",
            categories = setOf(AssetCategory.NON_CUSTODIAL, AssetCategory.CUSTODIAL),
            precisionDp = 2,
            requiredConfirmations = 3,
            colour = "000000"
        ) {}
    }
}
