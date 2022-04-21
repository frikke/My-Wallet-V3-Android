package piuk.blockchain.android.ui.interest.data

import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.core.interest.InterestAccountBalance
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.repositories.interest.Eligibility
import com.blockchain.nabu.datamanagers.repositories.interest.IneligibilityReason
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.nabu.service.TierService
import com.blockchain.outcome.Outcome
import com.blockchain.testutils.USD
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import piuk.blockchain.android.ui.interest.data.repository.AssetInterestRepository
import piuk.blockchain.android.ui.interest.domain.model.AssetInterestDetail
import piuk.blockchain.android.ui.interest.domain.model.AssetInterestInfo
import piuk.blockchain.android.ui.interest.domain.model.InterestDetail
import piuk.blockchain.android.ui.interest.domain.repository.AssetInterestService

@ExperimentalCoroutinesApi
class AssetInterestRepositoryTest {
    private val kycTierService = mockk<TierService>()
    private val interestBalance = mockk<InterestBalanceDataManager>()
    private val custodialWalletManager = mockk<CustodialWalletManager>()
    private val exchangeRatesDataManager = mockk<ExchangeRatesDataManager>()
    private val coincore = mockk<Coincore>()
    private val dispatcher = UnconfinedTestDispatcher()

    private val service: AssetInterestService = AssetInterestRepository(
        kycTierService,
        interestBalance,
        custodialWalletManager,
        exchangeRatesDataManager,
        coincore,
        dispatcher
    )

    private val kycTiers = KycTiers.default()
    private val enabledAssets = listOf(CryptoCurrency.BTC, CryptoCurrency.ETHER)

    private val interestAccountBalanceBtc = InterestAccountBalance(
        totalInterest = Money.fromMajor(CryptoCurrency.BTC, 200.toBigDecimal()),
        totalBalance = Money.fromMajor(CryptoCurrency.BTC, 200.toBigDecimal()),
        pendingInterest = mockk(), pendingDeposit = mockk(), lockedBalance = mockk(),
        hasTransactions = mockk(relaxed = true)
    )
    private val interestAccountBalanceEth = InterestAccountBalance(
        totalInterest = Money.fromMajor(CryptoCurrency.ETHER, 300.toBigDecimal()),
        totalBalance = Money.fromMajor(CryptoCurrency.ETHER, 300.toBigDecimal()),
        pendingInterest = mockk(), pendingDeposit = mockk(), lockedBalance = mockk(),
        hasTransactions = mockk(relaxed = true)
    )

    private val exchangeRateBtc = ExchangeRate(1.toBigDecimal(), CryptoCurrency.BTC, USD)
    private val exchangeRateEth = ExchangeRate(1.toBigDecimal(), CryptoCurrency.ETHER, USD)

    private val interestRateBtc: Double = 1.0
    private val interestRateEth: Double = 1.0

    private val eligibilityBtc = Eligibility(true, IneligibilityReason.KYC_TIER)
    private val eligibilityEth = Eligibility(false, IneligibilityReason.REGION)

    private val assetInterestInfoBtc = AssetInterestInfo(
        CryptoCurrency.BTC,
        AssetInterestDetail(
            totalInterest = interestAccountBalanceBtc.totalInterest,
            totalBalance = interestAccountBalanceBtc.totalBalance,
            rate = interestRateBtc,
            eligible = eligibilityBtc.eligible,
            ineligibilityReason = eligibilityBtc.ineligibilityReason,
            totalBalanceFiat = exchangeRateBtc.convert(interestAccountBalanceBtc.totalBalance)
        )
    )

    private val assetInterestInfoBtcNull = AssetInterestInfo(CryptoCurrency.BTC, null)

    private val assetInterestInfoEth = AssetInterestInfo(
        CryptoCurrency.ETHER,
        AssetInterestDetail(
            totalInterest = interestAccountBalanceEth.totalInterest,
            totalBalance = interestAccountBalanceEth.totalBalance,
            rate = interestRateEth,
            eligible = eligibilityEth.eligible,
            ineligibilityReason = eligibilityEth.ineligibilityReason,
            totalBalanceFiat = exchangeRateEth.convert(interestAccountBalanceEth.totalBalance)
        )
    )

    private val accountGroup = mockk<AccountGroup>()

    @Test
    fun `WHEN kycTierService OK, custodialWalletManager OK, THEN Success should be returned`() = runTest {
        every { kycTierService.tiers() } returns Single.just(kycTiers)
        every { custodialWalletManager.getInterestEnabledAssets() } returns Single.just(enabledAssets)

        val expected = InterestDetail(kycTiers, enabledAssets)
        val result = service.getInterestDetail()

        assertTrue { result is Outcome.Success }

        result as Outcome.Success
        assertEquals(expected, result.value)
    }

    @Test
    fun `WHEN kycTierService OK, custodialWalletManager throws, THEN Failure should be returned`() = runTest {
        every { kycTierService.tiers() } returns Single.just(kycTiers)
        every { custodialWalletManager.getInterestEnabledAssets() } throws Throwable("error")

        val result = service.getInterestDetail()
        assertTrue { result is Outcome.Failure }
    }

    @Test
    fun `WHEN kycTierService throws, custodialWalletManager OK, THEN Failure should be returned`() = runTest {
        every { kycTierService.tiers() } throws Throwable("error")
        every { custodialWalletManager.getInterestEnabledAssets() } returns Single.just(enabledAssets)

        val result = service.getInterestDetail()
        assertTrue { result is Outcome.Failure }
    }

    @Test
    fun `WHEN all services OK, THEN Success should be returned`() = runTest {
        every { interestBalance.getBalanceForAsset(CryptoCurrency.BTC) } returns
            Observable.just(interestAccountBalanceBtc)
        every { exchangeRatesDataManager.exchangeRateToUserFiat(CryptoCurrency.BTC) } returns
            Observable.just(exchangeRateBtc)
        every { custodialWalletManager.getInterestAccountRates(CryptoCurrency.BTC) } returns
            Single.just(interestRateBtc)
        every { custodialWalletManager.getInterestEligibilityForAsset(CryptoCurrency.BTC) } returns
            Single.just(eligibilityBtc)

        every { interestBalance.getBalanceForAsset(CryptoCurrency.ETHER) } returns
            Observable.just(interestAccountBalanceEth)
        every { exchangeRatesDataManager.exchangeRateToUserFiat(CryptoCurrency.ETHER) } returns
            Observable.just(exchangeRateEth)
        every { custodialWalletManager.getInterestAccountRates(CryptoCurrency.ETHER) } returns
            Single.just(interestRateEth)
        every { custodialWalletManager.getInterestEligibilityForAsset(CryptoCurrency.ETHER) } returns
            Single.just(eligibilityEth)

        val expected = Outcome.Success(listOf(assetInterestInfoBtc, assetInterestInfoEth))
        val result = service.getAssetsInterestInfo(listOf(CryptoCurrency.BTC, CryptoCurrency.ETHER))
        assertEquals(expected, result)
    }

    @Test
    fun `WHEN services fail for BTC, THEN its detail should be null`() = runTest {
        every { interestBalance.getBalanceForAsset(CryptoCurrency.BTC) } throws Throwable("error")
        every { exchangeRatesDataManager.exchangeRateToUserFiat(CryptoCurrency.BTC) } throws Throwable("error")
        every { custodialWalletManager.getInterestAccountRates(CryptoCurrency.BTC) } throws Throwable("error")
        every { custodialWalletManager.getInterestEligibilityForAsset(CryptoCurrency.BTC) } throws Throwable("error")

        every { interestBalance.getBalanceForAsset(CryptoCurrency.ETHER) } returns
            Observable.just(interestAccountBalanceEth)
        every { exchangeRatesDataManager.exchangeRateToUserFiat(CryptoCurrency.ETHER) } returns
            Observable.just(exchangeRateEth)
        every { custodialWalletManager.getInterestAccountRates(CryptoCurrency.ETHER) } returns
            Single.just(interestRateEth)
        every { custodialWalletManager.getInterestEligibilityForAsset(CryptoCurrency.ETHER) } returns
            Single.just(eligibilityEth)

        val expected = Outcome.Success(listOf(assetInterestInfoBtcNull, assetInterestInfoEth))
        val result = service.getAssetsInterestInfo(listOf(CryptoCurrency.BTC, CryptoCurrency.ETHER))
        assertEquals(expected, result)
    }

    @Test
    fun `WHEN coincore is OK, THEN success should be returned`() = runTest {
        every { coincore[any<AssetInfo>()].accountGroup(any()) } returns Maybe.just(accountGroup)

        val expected = Outcome.Success(accountGroup)
        val result = service.getAccountGroup(CryptoCurrency.BTC, AssetFilter.All)
        assertEquals(expected, result)
    }

    @Test
    fun `WHEN coincore fails, THEN Failure should be returned`() = runTest {
        every { coincore[any<AssetInfo>()].accountGroup(any()) } throws Throwable("error")

        val result = service.getAccountGroup(CryptoCurrency.BTC, AssetFilter.All)
        assertTrue { result is Outcome.Failure }
    }
}
