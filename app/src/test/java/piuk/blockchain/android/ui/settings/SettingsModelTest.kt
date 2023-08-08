package piuk.blockchain.android.ui.settings

import com.blockchain.android.testutils.rxInit
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.domain.paymentmethods.model.CardStatus
import com.blockchain.domain.paymentmethods.model.CardType
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.MobilePaymentType
import com.blockchain.domain.paymentmethods.model.Partner
import com.blockchain.domain.paymentmethods.model.PaymentLimits
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.nabu.BasicProfileInfo
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.math.BigInteger
import java.util.Date
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.domain.usecases.AvailablePaymentMethodType
import piuk.blockchain.android.domain.usecases.LinkAccess

class SettingsModelTest {
    private lateinit var model: SettingsModel

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    private val interactor: SettingsInteractor = mock()

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = SettingsModel(
            initialState = SettingsState(),
            mainScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            remoteLogger = mock(),
            interactor = interactor,
            themeService = mock()
        )
    }

    @Test
    fun `checkContactSupportEligibility silver user`() {
        val userInformation = mock<BasicProfileInfo>()
        whenever(userInformation.email).thenReturn("paco@gmail.com")

        whenever(interactor.getSupportEligibilityAndBasicInfo()).thenReturn(
            Single.just(UserDetails(KycTier.SILVER, userInformation, ReferralInfo.NotAvailable))
        )

        val testState = model.state.test()
        model.process(SettingsIntent.LoadHeaderInformation)

        testState
            .assertValueAt(0) {
                it == SettingsState()
            }.assertValueAt(1) {
                it == SettingsState(
                    basicProfileInfo = userInformation,
                    tier = KycTier.SILVER
                )
            }
    }

    @Test
    fun `checkContactSupportEligibility is gold user`() {
        val userInformation = mock<BasicProfileInfo>()
        whenever(userInformation.email).thenReturn("paco@gmail.com")

        whenever(interactor.getSupportEligibilityAndBasicInfo())
            .thenReturn(Single.just(UserDetails(KycTier.GOLD, userInformation, ReferralInfo.NotAvailable)))

        val testState = model.state.test()
        model.process(SettingsIntent.LoadHeaderInformation)

        testState
            .assertValueAt(0) {
                it == SettingsState()
            }.assertValueAt(1) {
                it == SettingsState(
                    basicProfileInfo = userInformation,
                    tier = KycTier.GOLD
                )
            }
    }

    @Test
    fun `checkContactSupportEligibility throws error`() {
        whenever(interactor.getSupportEligibilityAndBasicInfo()).thenReturn(Single.error { Throwable() })

        val testState = model.state.test()
        model.process(SettingsIntent.LoadHeaderInformation)

        testState
            .assertValueAt(0) { it == SettingsState() }
    }

    @Test
    fun `loadPaymentMethods works`() {
        val paymentDetails: PaymentMethods = mock()
        whenever(interactor.getExistingPaymentMethods()).thenReturn(Single.just(paymentDetails))
        whenever(interactor.canPayWithBind()).thenReturn(Single.just(true))

        val testState = model.state.test()
        model.process(SettingsIntent.LoadPaymentMethods)

        testState
            .assertValueAt(0) {
                it == SettingsState()
            }.assertValueAt(1) {
                it == SettingsState(
                    paymentMethodInfo = paymentDetails,
                    canPayWithBind = true
                )
            }
    }

    @Test
    fun `removing a card should fetch and update available payment methods`() {
        val nullLimits = PaymentLimits(BigInteger.ZERO, BigInteger.ZERO, USD)
        val initialPaymentMethods = PaymentMethods(
            availablePaymentMethodTypes = emptyList(),
            linkedBanks = emptyList(),
            linkedCards = listOf(
                PaymentMethod.Card(
                    "id",
                    nullLimits,
                    "",
                    "",
                    Partner.CARDPROVIDER,
                    Date(),
                    CardType.AMEX,
                    CardStatus.ACTIVE,
                    MobilePaymentType.GOOGLE_PAY,
                    true
                )
            )
        )
        model = SettingsModel(
            initialState = SettingsState(paymentMethodInfo = initialPaymentMethods),
            mainScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            remoteLogger = mock(),
            interactor = interactor,
            themeService = mock()
        )

        val expectedAvailable = listOf(
            AvailablePaymentMethodType(
                true,
                LinkAccess.BLOCKED,
                USD,
                PaymentMethodType.PAYMENT_CARD,
                nullLimits
            )
        )
        whenever(interactor.getAvailablePaymentMethodsTypes()).thenReturn(Single.just(expectedAvailable))

        val testState = model.state.test()
        model.process(SettingsIntent.OnCardRemoved("id"))

        testState
            .assertValueAt(1) {
                it.paymentMethodInfo?.linkedCards == emptyList<PaymentMethod.Card>()
            }
            .assertValueAt(2) {
                it.paymentMethodInfo?.availablePaymentMethodTypes == expectedAvailable &&
                    it.paymentMethodInfo?.linkedBanks == emptyList<BankItem>() &&
                    it.paymentMethodInfo?.linkedCards == emptyList<PaymentMethod.Card>()
            }

        verify(interactor).getAvailablePaymentMethodsTypes()
    }

    @Test
    fun `loadPaymentMethods fails`() {
        whenever(interactor.getExistingPaymentMethods()).thenReturn(Single.error(Exception()))
        whenever(interactor.canPayWithBind()).thenReturn(Single.just(true))

        val testState = model.state.test()
        model.process(SettingsIntent.LoadPaymentMethods)

        testState
            .assertValueAt(0) {
                it == SettingsState()
            }.assertValueAt(1) {
                it == SettingsState(
                    error = SettingsError.PaymentMethodsLoadFail
                )
            }
    }

    @Test
    fun `bankLinkedSelected works`() {
        val bankLinkInfo: LinkBankTransfer = mock()
        whenever(interactor.getBankLinkingInfo()).thenReturn(Single.just(bankLinkInfo))

        val testState = model.state.test()
        model.process(SettingsIntent.AddLinkBankSelected)

        testState
            .assertValueAt(0) {
                it == SettingsState()
            }.assertValueAt(1) {
                it.viewToLaunch is ViewToLaunch.BankTransfer &&
                    (it.viewToLaunch as ViewToLaunch.BankTransfer).linkBankTransfer == bankLinkInfo
            }
    }

    @Test
    fun `bankLinkedSelected fails`() {
        whenever(interactor.getBankLinkingInfo()).thenReturn(Single.error(Exception()))

        val testState = model.state.test()
        model.process(SettingsIntent.AddLinkBankSelected)

        testState
            .assertValueAt(0) {
                it == SettingsState()
            }.assertValueAt(1) {
                it.error == SettingsError.BankLinkStartFail
            }
    }

    @Test
    fun `logout works`() {
        whenever(interactor.unpairWallet()).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(SettingsIntent.Logout)

        testState
            .assertValueAt(0) {
                it == SettingsState()
            }.assertValueAt(1) {
                it == SettingsState(
                    hasWalletUnpaired = true
                )
            }
    }

    @Test
    fun `logout fails`() {
        whenever(interactor.unpairWallet()).thenReturn(Completable.error(Exception()))

        val testState = model.state.test()
        model.process(SettingsIntent.Logout)

        testState
            .assertValueAt(0) {
                it == SettingsState()
            }.assertValueAt(1) {
                it == SettingsState(
                    error = SettingsError.UnpairFailed
                )
            }
    }

    companion object {
        private val USD = FiatCurrency.fromCurrencyCode("USD")
    }
}
