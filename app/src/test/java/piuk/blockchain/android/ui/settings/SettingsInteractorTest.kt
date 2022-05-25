package piuk.blockchain.android.ui.settings

import com.blockchain.core.Database
import com.blockchain.core.featureflag.IntegratedFeatureFlag
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.CardService
import com.blockchain.domain.referral.ReferralService
import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.blockchain.preferences.CurrencyPrefs
import com.nhaarman.mockitokotlin2.doNothing
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
    private val database: Database = mock()
    private val credentialsWiper: CredentialsWiper = mock()
    private val bankService: BankService = mock()
    private val cardService: CardService = mock()
    private val getAvailablePaymentMethodsTypesUseCase: GetAvailablePaymentMethodsTypesUseCase = mock()
    private val currencyPrefs: CurrencyPrefs = mock()
    private val referralService: ReferralService = mock()
    private val referralFeatureFlag: IntegratedFeatureFlag = mock()

    @Before
    fun setup() {
        interactor = SettingsInteractor(
            userIdentity = userIdentity,
            database = database,
            credentialsWiper = credentialsWiper,
            bankService = bankService,
            cardService = cardService,
            getAvailablePaymentMethodsTypesUseCase = getAvailablePaymentMethodsTypesUseCase,
            currencyPrefs = currencyPrefs,
            referralService = referralService,
            referralFeatureFlag = referralFeatureFlag
        )
        whenever(referralFeatureFlag.enabled).thenReturn(Single.just(false))
    }

    @Test
    fun `Load eligibility and basic information`() = runBlocking {
        val userInformation = mock<BasicProfileInfo>()
        whenever(userIdentity.getHighestApprovedKycTier()).thenReturn(Single.just(Tier.GOLD))
        whenever(userIdentity.getBasicProfileInformation()).thenReturn(Single.just(userInformation))
        val observer = interactor.getSupportEligibilityAndBasicInfo().test()
        observer.assertValueAt(0) {
            it.userTier == Tier.GOLD && it.userInfo == userInformation
        }

        verify(userIdentity).getHighestApprovedKycTier()
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
}
