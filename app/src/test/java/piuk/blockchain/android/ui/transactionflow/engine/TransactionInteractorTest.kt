package piuk.blockchain.android.ui.transactionflow.engine

import com.blockchain.android.testutils.rxInit
import com.blockchain.coincore.AddressFactory
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.fiat.LinkedBanksFactory
import com.blockchain.core.announcements.DismissRecorder
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.PaymentMethodService
import com.blockchain.domain.paymentmethods.model.EligiblePaymentMethodType
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.trade.TradeDataService
import com.blockchain.earn.domain.service.ActiveRewardsService
import com.blockchain.earn.domain.service.StakingService
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.fiatActions.fiatactions.models.LinkablePaymentMethods
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.repositories.swap.CustodialRepository
import com.blockchain.preferences.BankLinkingPrefs
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.TransactionPrefs
import com.blockchain.testutils.EUR
import com.blockchain.testutils.USD
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.FiatCurrency
import io.mockk.mockk
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.transfer.AccountsSorting

class TransactionInteractorTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private val coincore: Coincore = mock()
    private val addressFactory: AddressFactory = mock()
    private val custodialRepository: CustodialRepository = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val bankService: BankService = mock()
    private val paymentMethodService: PaymentMethodService = mock()
    private val currencyPrefs: CurrencyPrefs = mock()
    private val identity: UserIdentity = mock()
    private val defaultAccountSorting: AccountsSorting = mock()
    private val swapSourceAccountsSorting: AccountsSorting = mock()
    private val swapTargetAccountsSorting: AccountsSorting = mock()
    private val linkedBanksFactory: LinkedBanksFactory = mock()
    private val bankLinkingPrefs: BankLinkingPrefs = mock()
    private val dismissRecorder: DismissRecorder = mock()
    private val fiatCurrenciesService: FiatCurrenciesService = mock()
    private val tradeDataService: TradeDataService = mockk()
    private val improvedPaymentUxFF: FeatureFlag = mock()
    private val stakingService: StakingService = mock()
    private val activeRewardsService: ActiveRewardsService = mock()
    private val transactionPrefs: TransactionPrefs = mock()

    private lateinit var subject: TransactionInteractor

    @Before
    fun setUp() {
        subject = TransactionInteractor(
            coincore = coincore,
            addressFactory = addressFactory,
            custodialRepository = custodialRepository,
            custodialWalletManager = custodialWalletManager,
            bankService = bankService,
            paymentMethodService = paymentMethodService,
            currencyPrefs = currencyPrefs,
            identity = identity,
            defaultAccountsSorting = defaultAccountSorting,
            swapSourceAccountsSorting = swapSourceAccountsSorting,
            swapTargetAccountsSorting = swapTargetAccountsSorting,
            linkedBanksFactory = linkedBanksFactory,
            bankLinkingPrefs = bankLinkingPrefs,
            dismissRecorder = dismissRecorder,
            fiatCurrenciesService = fiatCurrenciesService,
            tradeDataService = tradeDataService,
            improvedPaymentUxFF = improvedPaymentUxFF,
            stakingService = stakingService,
            activeRewardsService = activeRewardsService,
            transactionPrefs = transactionPrefs
        )
    }

    @Test
    internal fun `updateFiatDepositOptions - BANK_TRANSFER + BANK_ACCOUNT`() {
        // Arrange
        arrangeEligiblePaymentMethodTypes(
            USD,
            listOf(
                EligiblePaymentMethodType(PaymentMethodType.BANK_TRANSFER, USD),
                EligiblePaymentMethodType(PaymentMethodType.BANK_ACCOUNT, USD)
            )
        )

        // Act
        val result = subject.updateFiatDepositOptions(USD)

        // Assert
        assertFiatDepositOptionsResult(
            result,
            DepositOptionsState.ShowBottomSheet(
                LinkablePaymentMethods(
                    USD,
                    listOf(PaymentMethodType.BANK_TRANSFER, PaymentMethodType.BANK_ACCOUNT)
                )
            )
        )
    }

    @Test
    internal fun `updateFiatDepositOptions - BANK_TRANSFER`() {
        // Arrange
        arrangeEligiblePaymentMethodTypes(
            USD,
            listOf(
                EligiblePaymentMethodType(PaymentMethodType.BANK_TRANSFER, USD),
                EligiblePaymentMethodType(PaymentMethodType.BANK_ACCOUNT, EUR)
            )
        )

        // Act
        val result = subject.updateFiatDepositOptions(USD)

        // Assert
        assertFiatDepositOptionsResult(result, DepositOptionsState.LaunchLinkBank)
    }

    @Test
    internal fun `updateFiatDepositOptions - BANK_ACCOUNT`() {
        // Arrange
        arrangeEligiblePaymentMethodTypes(
            USD,
            listOf(
                EligiblePaymentMethodType(PaymentMethodType.BANK_TRANSFER, EUR),
                EligiblePaymentMethodType(PaymentMethodType.BANK_ACCOUNT, USD)
            )
        )

        // Act
        val result = subject.updateFiatDepositOptions(USD)

        // Assert
        assertFiatDepositOptionsResult(result, DepositOptionsState.LaunchWireTransfer(USD))
    }

    @Test
    internal fun `updateFiatDepositOptions - INVALID Payment Method Type`() {
        // Arrange
        arrangeEligiblePaymentMethodTypes(
            USD,
            listOf(
                EligiblePaymentMethodType(PaymentMethodType.PAYMENT_CARD, USD)
            )
        )

        // Act
        val result = subject.updateFiatDepositOptions(USD)

        // Assert
        assertFiatDepositOptionsResult(result, DepositOptionsState.None)
    }

    @Test
    internal fun `updateFiatDepositOptions - NO Payment Method Types`() {
        // Arrange
        arrangeEligiblePaymentMethodTypes(
            USD,
            emptyList()
        )

        // Act
        val result = subject.updateFiatDepositOptions(USD)

        // Assert
        assertFiatDepositOptionsResult(result, DepositOptionsState.None)
    }

    @Test
    internal fun `updateFiatDepositOptions - INVALID Currency Method Type`() {
        // Arrange
        arrangeEligiblePaymentMethodTypes(
            USD,
            listOf(
                EligiblePaymentMethodType(PaymentMethodType.BANK_TRANSFER, EUR)
            )
        )

        // Act
        val result = subject.updateFiatDepositOptions(USD)

        // Assert
        assertFiatDepositOptionsResult(result, DepositOptionsState.None)
    }

    private fun arrangeEligiblePaymentMethodTypes(
        currency: FiatCurrency,
        eligiblePaymentMethodTypes: List<EligiblePaymentMethodType>
    ) {
        whenever(paymentMethodService.getEligiblePaymentMethodTypes(currency)).thenReturn(
            Single.just(eligiblePaymentMethodTypes)
        )
    }

    private fun assertFiatDepositOptionsResult(
        result: Single<TransactionIntent>,
        expectedDepositOptionsState: DepositOptionsState
    ) {
        result
            .test()
            .assertValue {
                it == TransactionIntent.FiatDepositOptionSelected(expectedDepositOptionsState)
            }
    }
}
