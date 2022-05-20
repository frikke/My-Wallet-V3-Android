package piuk.blockchain.android.ui.settings

import com.blockchain.android.testutils.rxInit
import com.blockchain.api.services.MobilePaymentType
import com.blockchain.core.payments.model.LinkBankTransfer
import com.blockchain.core.payments.model.Partner
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.nabu.Tier
import com.blockchain.nabu.datamanagers.PaymentLimits
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.nabu.datamanagers.custodialwalletimpl.CardStatus
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.braintreepayments.cardform.utils.CardType
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
import piuk.blockchain.android.ui.settings.v2.BankItem
import piuk.blockchain.android.ui.settings.v2.PaymentMethods
import piuk.blockchain.android.ui.settings.v2.SettingsError
import piuk.blockchain.android.ui.settings.v2.SettingsIntent
import piuk.blockchain.android.ui.settings.v2.SettingsInteractor
import piuk.blockchain.android.ui.settings.v2.SettingsModel
import piuk.blockchain.android.ui.settings.v2.SettingsState
import piuk.blockchain.android.ui.settings.v2.UserDetails
import piuk.blockchain.android.ui.settings.v2.ViewToLaunch

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
            interactor = interactor
        )
    }

    @Test
    fun `checkContactSupportEligibility silver user`() {
        val userInformation = mock<BasicProfileInfo>()
        whenever(userInformation.email).thenReturn("paco@gmail.com")

        whenever(interactor.getSupportEligibilityAndBasicInfo()).thenReturn(
            Single.just(UserDetails(Tier.SILVER, userInformation, ReferralInfo.NotAvailable))
        )

        val testState = model.state.test()
        model.process(SettingsIntent.LoadHeaderInformation)

        testState
            .assertValueAt(0) {
                it == SettingsState()
            }.assertValueAt(1) {
                it == SettingsState(
                    basicProfileInfo = userInformation,
                    tier = Tier.SILVER
                )
            }
    }

    @Test
    fun `checkContactSupportEligibility is gold user`() {
        val userInformation = mock<BasicProfileInfo>()
        whenever(userInformation.email).thenReturn("paco@gmail.com")

        whenever(interactor.getSupportEligibilityAndBasicInfo())
            .thenReturn(Single.just(UserDetails(Tier.GOLD, userInformation, ReferralInfo.NotAvailable)))

        val testState = model.state.test()
        model.process(SettingsIntent.LoadHeaderInformation)

        testState
            .assertValueAt(0) {
                it == SettingsState()
            }.assertValueAt(1) {
                it == SettingsState(
                    basicProfileInfo = userInformation,
                    tier = Tier.GOLD
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

        val testState = model.state.test()
        model.process(SettingsIntent.LoadPaymentMethods)

        testState
            .assertValueAt(0) {
                it == SettingsState()
            }.assertValueAt(1) {
                it == SettingsState(
                    paymentMethodInfo = paymentDetails
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
            interactor = interactor
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

        val testState = model.state.test()
        model.process(SettingsIntent.LoadPaymentMethods)

        testState
            .assertValueAt(0) {
                it == SettingsState()
            }.assertValueAt(1) {
                it == SettingsState(
                    error = SettingsError.PAYMENT_METHODS_LOAD_FAIL
                )
            }
    }

    @Test
    fun `bankTransferSelected works`() {
        val bankLinkInfo: LinkBankTransfer = mock()
        whenever(interactor.getBankLinkingInfo()).thenReturn(Single.just(bankLinkInfo))

        val testState = model.state.test()
        model.process(SettingsIntent.AddBankTransferSelected)

        testState
            .assertValueAt(0) {
                it == SettingsState()
            }.assertValueAt(1) {
                it.viewToLaunch is ViewToLaunch.BankTransfer &&
                    (it.viewToLaunch as ViewToLaunch.BankTransfer).linkBankTransfer == bankLinkInfo
            }
    }

    @Test
    fun `bankTransferSelected fails`() {
        whenever(interactor.getBankLinkingInfo()).thenReturn(Single.error(Exception()))

        val testState = model.state.test()
        model.process(SettingsIntent.AddBankTransferSelected)

        testState
            .assertValueAt(0) {
                it == SettingsState()
            }.assertValueAt(1) {
                it.error == SettingsError.BANK_LINK_START_FAIL
            }
    }

    @Test
    fun `bankAccountSelected works`() {
        val userFiat: FiatCurrency = mock()
        whenever(interactor.getUserFiat()).thenReturn(userFiat)

        val testState = model.state.test()
        model.process(SettingsIntent.AddBankAccountSelected)

        testState
            .assertValueAt(0) {
                it == SettingsState()
            }.assertValueAt(1) {
                it.viewToLaunch is ViewToLaunch.BankAccount &&
                    (it.viewToLaunch as ViewToLaunch.BankAccount).currency == userFiat
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
                    error = SettingsError.UNPAIR_FAILED
                )
            }
    }

    companion object {
        private val USD = FiatCurrency.fromCurrencyCode("USD")
    }
}
