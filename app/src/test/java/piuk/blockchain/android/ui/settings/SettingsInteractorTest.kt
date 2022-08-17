package piuk.blockchain.android.ui.settings

import com.blockchain.core.Database
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.CardService
import com.blockchain.domain.referral.ReferralService
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.NabuUserIdentity
import com.blockchain.outcome.Outcome
import com.blockchain.preferences.CurrencyPrefs
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import exchangerate.HistoricRateQueries
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.domain.usecases.GetAvailablePaymentMethodsTypesUseCase
import piuk.blockchain.android.ui.home.CredentialsWiper
import piuk.blockchain.android.ui.settings.v2.SettingsInteractor

class SettingsInteractorTest {

    private lateinit var interactor: SettingsInteractor
    private val userIdentity: UserIdentity = mock()
    private val kycService: KycService = mock()
    private val database: Database = mock()
    private val credentialsWiper: CredentialsWiper = mock()
    private val bankService: BankService = mock()
    private val cardService: CardService = mock()
    private val getAvailablePaymentMethodsTypesUseCase: GetAvailablePaymentMethodsTypesUseCase = mock()
    private val currencyPrefs: CurrencyPrefs = mock()
    private val referralService: ReferralService = mock()
    private val nabuUserIdentity: NabuUserIdentity = mock()

    @Before
    fun setup() {
        interactor = SettingsInteractor(
            userIdentity = userIdentity,
            kycService = kycService,
            database = database,
            credentialsWiper = credentialsWiper,
            bankService = bankService,
            cardService = cardService,
            getAvailablePaymentMethodsTypesUseCase = getAvailablePaymentMethodsTypesUseCase,
            currencyPrefs = currencyPrefs,
            referralService = referralService,
            nabuUserIdentity = nabuUserIdentity
        )
    }

    @Test
    fun `Load eligibility and basic information`() = runBlocking {
        whenever(referralService.fetchReferralData()).doReturn(Outcome.Success(ReferralInfo.NotAvailable))
        val userInformation = mock<BasicProfileInfo>()
        whenever(kycService.getHighestApprovedTierLevelLegacy()).thenReturn(Single.just(KycTier.GOLD))
        whenever(userIdentity.getBasicProfileInformation()).thenReturn(Single.just(userInformation))
        val observer = interactor.getSupportEligibilityAndBasicInfo().test().await()

        observer.assertValueAt(0) {
            it.kycTier == KycTier.GOLD && it.userInfo == userInformation
        }

        verify(kycService).getHighestApprovedTierLevelLegacy()
        verify(userIdentity).getBasicProfileInformation()

        verifyNoMoreInteractions(userIdentity)
    }

    @Test
    fun `Sign out then unpair wallet`() {
        val mockQueries: HistoricRateQueries = mock()

        doNothing().whenever(credentialsWiper).wipe()
        whenever(database.historicRateQueries).thenReturn(mockQueries)
        doNothing().whenever(mockQueries).clear()

        val observer = interactor.unpairWallet().test()
        observer.assertComplete()

        verify(credentialsWiper).wipe()
        verify(database.historicRateQueries).clear()
    }

    @Test
    fun `canPayWithBind() should check if user is Argentinian`() {
        whenever(nabuUserIdentity.isArgentinian()).thenReturn(Single.just(true))

        interactor.canPayWithBind().test()
            .assertValue(true)
            .assertComplete()
    }
}
