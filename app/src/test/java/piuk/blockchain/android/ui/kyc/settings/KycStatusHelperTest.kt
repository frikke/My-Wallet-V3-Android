package piuk.blockchain.android.ui.kyc.settings

import com.blockchain.android.testutils.rxInit
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.kyc.domain.model.KycTierState
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.eligibility.model.GetRegionScope
import com.blockchain.domain.eligibility.model.Region
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.outcome.Outcome
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.tiers
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class KycStatusHelperTest {

    private lateinit var subject: KycStatusHelper
    private val eligibilityService: EligibilityService = mock()
    private val userService: UserService = mock()
    private val nabuToken: NabuToken = mock()
    private val settingsDataManager: SettingsDataManager = mock()
    private val kycService: KycService = mock()

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = KycStatusHelper(
            kycService
        )
    }

    @Test
    fun `get settings kyc state should return failed`() = runTest {
        // Arrange
        val countryCode = "US"
        val countryList =
            listOf(Region.Country("UK", "United Kingdom", true, emptyList()))
        whenever(eligibilityService.getCountriesList(GetRegionScope.Kyc))
            .thenReturn(Outcome.Success(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        whenever(kycService.getTiersLegacy())
            .thenReturn(Single.just(tiers(KycTierState.Rejected, KycTierState.Rejected)))
        // Act
        val testObserver = subject.getKycTierStatus().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue {
            it.isRejectedFor(KycTier.GOLD) &&
                it.isRejectedFor(KycTier.SILVER)
        }
    }
}
