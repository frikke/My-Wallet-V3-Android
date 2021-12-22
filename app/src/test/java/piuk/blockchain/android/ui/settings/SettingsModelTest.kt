package piuk.blockchain.android.ui.settings

import com.blockchain.android.testutils.rxInit
import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.nabu.Tier
import com.blockchain.nabu.models.data.LinkBankTransfer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.settings.v2.PaymentMethods
import piuk.blockchain.android.ui.settings.v2.SettingsError
import piuk.blockchain.android.ui.settings.v2.SettingsIntent
import piuk.blockchain.android.ui.settings.v2.SettingsInteractor
import piuk.blockchain.android.ui.settings.v2.SettingsModel
import piuk.blockchain.android.ui.settings.v2.SettingsState
import piuk.blockchain.android.ui.settings.v2.UserDetails
import piuk.blockchain.android.ui.settings.v2.ViewToLaunch
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

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
            crashLogger = mock(),
            interactor = interactor
        )
    }

    @Test
    fun `checkContactSupportEligibility silver user`() {
        val userInformation = mock<BasicProfileInfo>()
        whenever(userInformation.email).thenReturn("paco@gmail.com")

        whenever(interactor.getSupportEligibilityAndBasicInfo()).thenReturn(
            Single.just(UserDetails(Tier.SILVER, userInformation))
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
            .thenReturn(Single.just(UserDetails(Tier.GOLD, userInformation)))

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
}
