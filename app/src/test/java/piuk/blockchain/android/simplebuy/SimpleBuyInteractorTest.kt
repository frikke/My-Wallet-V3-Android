package piuk.blockchain.android.simplebuy

import com.blockchain.analytics.Analytics
import com.blockchain.banking.BankPartnerCallbackProvider
import com.blockchain.coincore.Coincore
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.core.limits.TxLimit
import com.blockchain.core.limits.TxLimits
import com.blockchain.core.payments.PaymentsRepository
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.eligibility.model.Region
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.CardService
import com.blockchain.domain.paymentmethods.PaymentMethodService
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.SimpleBuyEligibilityProvider
import com.blockchain.nabu.datamanagers.repositories.WithdrawLocksRepository
import com.blockchain.outcome.Outcome
import com.blockchain.payments.core.CardAcquirer
import com.blockchain.payments.core.CardProcessor
import com.blockchain.preferences.BankLinkingPrefs
import com.blockchain.preferences.OnboardingPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.domain.usecases.CancelOrderUseCase
import piuk.blockchain.android.domain.usecases.GetAvailablePaymentMethodsTypesUseCase

class SimpleBuyInteractorTest {

    private lateinit var subject: SimpleBuyInteractor
    private val kycService: KycService = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val limitsDataManager: LimitsDataManager = mock()
    private val withdrawLocksRepository: WithdrawLocksRepository = mock()
    private val analytics: Analytics = mock()
    private val bankPartnerCallbackProvider: BankPartnerCallbackProvider = mock()
    private val eligibilityProvider: SimpleBuyEligibilityProvider = mock()
    private val exchangeRatesDataManager: ExchangeRatesDataManager = mock()
    private val coincore: Coincore = mock()
    private val userIdentity: UserIdentity = mock()
    private val bankLinkingPrefs: BankLinkingPrefs = mock()
    private val cardProcessors: Map<CardAcquirer, CardProcessor> = mock()
    private val cancelOrderUseCase: CancelOrderUseCase = mock()
    private val getAvailablePaymentMethodsTypesUseCase: GetAvailablePaymentMethodsTypesUseCase = mock()
    private val bankService: BankService = mock()
    private val cardService: CardService = mock()
    private val paymentMethodService: PaymentMethodService = mock()
    private val paymentsRepository: PaymentsRepository = mock()
    private val quickFillButtonsFeatureFlag: FeatureFlag = mock()
    private val cardRejectionCheckFeatureFlag: FeatureFlag = mock()
    private val simpleBuyPrefs: SimpleBuyPrefs = mock()
    private val onboardingPrefs: OnboardingPrefs = mock()
    private val eligibilityService: EligibilityService = mock {
        onBlocking { getStatesList(any(), any()) }.thenReturn(
            Outcome.Success(
                listOf(
                    Region.State("US", "Florida", false, "US-FL"),
                    Region.State("US", "Georgia", false, "US-GA"),
                    Region.State("US", "North Carolina", false, "US-NC")
                )
            )
        )
    }

    @Before
    fun setup() {
        subject = SimpleBuyInteractor(
            kycService = kycService,
            custodialWalletManager = custodialWalletManager,
            limitsDataManager = limitsDataManager,
            withdrawLocksRepository = withdrawLocksRepository,
            analytics = analytics,
            bankPartnerCallbackProvider = bankPartnerCallbackProvider,
            eligibilityProvider = eligibilityProvider,
            exchangeRatesDataManager = exchangeRatesDataManager,
            coincore = coincore,
            userIdentity = userIdentity,
            bankLinkingPrefs = bankLinkingPrefs,
            cardProcessors = cardProcessors,
            cancelOrderUseCase = cancelOrderUseCase,
            getAvailablePaymentMethodsTypesUseCase = getAvailablePaymentMethodsTypesUseCase,
            bankService = bankService,
            cardService = cardService,
            paymentMethodService = paymentMethodService,
            paymentsRepository = paymentsRepository,
            quickFillButtonsFeatureFlag = quickFillButtonsFeatureFlag,
            simpleBuyPrefs = simpleBuyPrefs,
            onboardingPrefs = onboardingPrefs,
            cardRejectionCheckFF = cardRejectionCheckFeatureFlag,
            eligibilityService = eligibilityService,
        )
    }

    @Test
    fun `when no previous buy amount for pair available then default value is returned and quick fill buttons are correct`() {
        whenever(simpleBuyPrefs.getLastAmount(any())).thenReturn("")
        whenever(quickFillButtonsFeatureFlag.enabled).thenReturn(Single.just(true))
        val fiatCurrency = FiatCurrency.Dollars
        val assetCode = "BTC"
        val maxAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(1000))
        val minAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(10))
        val limits = TxLimits(min = TxLimit.Limited(minAmount), max = TxLimit.Limited(maxAmount))
        val defaultAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(50))

        val test = subject.getPrefillAndQuickFillAmounts(limits, assetCode, fiatCurrency).test()
        test.assertValue {
            it.first == defaultAmount &&
                it.second?.buyMaxAmount == maxAmount &&
                it.second!!.quickFillButtons[0] == FiatValue.fromMajor(fiatCurrency, BigDecimal(110)) &&
                it.second!!.quickFillButtons[1] == FiatValue.fromMajor(fiatCurrency, BigDecimal(250)) &&
                it.second!!.quickFillButtons[2] == FiatValue.fromMajor(fiatCurrency, BigDecimal(600))
        }
    }

    @Test
    fun `when no previous buy amount for pair available and default value is lower than min returned and quick fill buttons are correct`() {
        whenever(simpleBuyPrefs.getLastAmount(any())).thenReturn("")
        whenever(quickFillButtonsFeatureFlag.enabled).thenReturn(Single.just(true))
        val fiatCurrency = FiatCurrency.Dollars
        val assetCode = "BTC"
        val maxAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(1000))
        val minAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(100))
        val limits = TxLimits(min = TxLimit.Limited(minAmount), max = TxLimit.Limited(maxAmount))
        val defaultAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(50))

        val prefilledAmount = if (defaultAmount < minAmount) minAmount else defaultAmount

        val test = subject.getPrefillAndQuickFillAmounts(limits, assetCode, fiatCurrency).test()
        test.assertValue {
            it.first == prefilledAmount &&
                it.second?.buyMaxAmount == maxAmount &&
                it.second!!.quickFillButtons[0] == FiatValue.fromMajor(fiatCurrency, BigDecimal(210)) &&
                it.second!!.quickFillButtons[1] == FiatValue.fromMajor(fiatCurrency, BigDecimal(450))
        }
    }

    @Test
    fun `when no previous buy amount for pair available and default value is higher than max returned and quick fill buttons are correct`() {
        whenever(simpleBuyPrefs.getLastAmount(any())).thenReturn("")
        whenever(quickFillButtonsFeatureFlag.enabled).thenReturn(Single.just(true))
        val fiatCurrency = FiatCurrency.Dollars
        val assetCode = "BTC"
        val maxAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(40))
        val minAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(10))
        val limits = TxLimits(min = TxLimit.Limited(minAmount), max = TxLimit.Limited(maxAmount))
        val defaultAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(50))

        val prefilledAmount = if (defaultAmount > maxAmount) maxAmount else defaultAmount

        val test = subject.getPrefillAndQuickFillAmounts(limits, assetCode, fiatCurrency).test()
        test.assertValue {
            it.first == prefilledAmount &&
                it.second?.buyMaxAmount == maxAmount
        }
    }

    @Test
    fun `when previous buy amount available for pair then it is returned and quick fill buttons are correct`() {
        whenever(simpleBuyPrefs.getLastAmount("BTC-USD")).thenReturn("100")
        whenever(quickFillButtonsFeatureFlag.enabled).thenReturn(Single.just(true))
        val fiatCurrency = FiatCurrency.Dollars
        val assetCode = "BTC"
        val maxAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(1000))
        val minAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(80))
        val limits = TxLimits(min = TxLimit.Limited(minAmount), max = TxLimit.Limited(maxAmount))

        val test = subject.getPrefillAndQuickFillAmounts(limits, assetCode, fiatCurrency).test()
        test.assertValue {
            it.first == FiatValue.fromMajor(fiatCurrency, BigDecimal(100)) &&
                it.second?.buyMaxAmount == limits.maxAmount &&
                it.second!!.quickFillButtons[0] == FiatValue.fromMajor(fiatCurrency, BigDecimal(210)) &&
                it.second!!.quickFillButtons[1] == FiatValue.fromMajor(fiatCurrency, BigDecimal(450)) &&
                it.second!!.quickFillButtons[2] == FiatValue.fromMajor(fiatCurrency, BigDecimal(1000))
        }
    }

    @Test
    fun `when first quick fill button over payment limit then none are returned`() {
        whenever(simpleBuyPrefs.getLastAmount("BTC-USD")).thenReturn("100")
        whenever(quickFillButtonsFeatureFlag.enabled).thenReturn(Single.just(true))
        val fiatCurrency = FiatCurrency.Dollars
        val assetCode = "BTC"
        val maxAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(80))
        val minAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(10))
        val limits = TxLimits(min = TxLimit.Limited(minAmount), max = TxLimit.Limited(maxAmount))

        val test = subject.getPrefillAndQuickFillAmounts(limits, assetCode, fiatCurrency).test()
        test.assertValue {
            it.first == maxAmount &&
                it.second?.buyMaxAmount == maxAmount &&
                it.second!!.quickFillButtons.isEmpty()
        }
    }

    @Test
    fun `when second quick fill button over payment limit then last two are not returned`() {
        whenever(simpleBuyPrefs.getLastAmount("BTC-USD")).thenReturn("50")
        whenever(quickFillButtonsFeatureFlag.enabled).thenReturn(Single.just(true))
        val fiatCurrency = FiatCurrency.Dollars
        val assetCode = "BTC"
        val maxAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(300))
        val minAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(100))
        val limits = TxLimits(min = TxLimit.Limited(minAmount), max = TxLimit.Limited(maxAmount))

        val test = subject.getPrefillAndQuickFillAmounts(limits, assetCode, fiatCurrency).test()
        test.assertValue {
            it.first == minAmount &&
                it.second?.buyMaxAmount == maxAmount &&
                it.second!!.quickFillButtons.size == 1 &&
                it.second!!.quickFillButtons[0] == FiatValue.fromMajor(fiatCurrency, BigDecimal(210))
        }
    }

    @Test
    fun `when third quick fill button over payment limit then last is not returned`() {
        whenever(simpleBuyPrefs.getLastAmount("BTC-USD")).thenReturn("50")
        whenever(quickFillButtonsFeatureFlag.enabled).thenReturn(Single.just(true))
        val fiatCurrency = FiatCurrency.Dollars
        val assetCode = "BTC"
        val maxAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(500))
        val minAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(100))
        val limits = TxLimits(min = TxLimit.Limited(minAmount), max = TxLimit.Limited(maxAmount))

        val test = subject.getPrefillAndQuickFillAmounts(limits, assetCode, fiatCurrency).test()
        test.assertValue {
            it.first == limits.minAmount &&
                it.second?.buyMaxAmount == limits.maxAmount &&
                it.second!!.quickFillButtons.size == 2 &&
                it.second!!.quickFillButtons[0] == FiatValue.fromMajor(fiatCurrency, BigDecimal(210)) &&
                it.second!!.quickFillButtons[1] == FiatValue.fromMajor(fiatCurrency, BigDecimal(450))
        }
    }

    @Test
    fun `when feature flag is turned off then no prefill or quick fill data is returned`() {
        whenever(simpleBuyPrefs.getLastAmount("BTC-USD")).thenReturn("100")
        whenever(quickFillButtonsFeatureFlag.enabled).thenReturn(Single.just(false))
        val fiatCurrency = FiatCurrency.Dollars
        val assetCode = "BTC"
        val maxAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(500))
        val minAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(500))
        val limits = TxLimits(min = TxLimit.Limited(minAmount), max = TxLimit.Limited(maxAmount))

        val test = subject.getPrefillAndQuickFillAmounts(limits, assetCode, fiatCurrency).test()
        test.assertValue {
            it.first == FiatValue.fromMajor(fiatCurrency, BigDecimal.ZERO) &&
                it.second == null
        }
    }

    @Test
    fun `when a list of States is requested, check if list is valid`() {
        val test = subject.getListOfStates("US").test()
        test.await().assertValue {
            it.isNotEmpty() &&
                it.size == 3 &&
                it.contains(Region.State("US", "Georgia", false, "US-GA"))
        }
    }
}
