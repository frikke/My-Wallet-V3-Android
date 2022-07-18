package piuk.blockchain.android.ui.kyc.settings

import com.blockchain.android.testutils.rxInit
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.eligibility.model.GetRegionScope
import com.blockchain.domain.eligibility.model.Region
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.nabu.models.responses.nabu.KycTierState
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.nabu.models.responses.nabu.UserState
import com.blockchain.nabu.service.TierService
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
import piuk.blockchain.android.ui.getBlankNabuUser
import piuk.blockchain.android.ui.tiers
import piuk.blockchain.android.ui.validOfflineToken
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class KycStatusHelperTest {

    private lateinit var subject: KycStatusHelper
    private val eligibilityService: EligibilityService = mock()
    private val userService: UserService = mock()
    private val nabuToken: NabuToken = mock()
    private val settingsDataManager: SettingsDataManager = mock()
    private val tierService: TierService = mock()

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = KycStatusHelper(
            eligibilityService,
            userService,
            nabuToken,
            settingsDataManager,
            tierService
        )
    }

    @Test
    fun `has account returns false due to error fetching token`() {
        // Arrange
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.error { Throwable() })
        // Act
        val testObserver = subject.hasAccount().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
    }

    @Test
    fun `has account returns true as token was found in metadata`() {
        // Arrange
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        // Act
        val testObserver = subject.hasAccount().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }

    @Test
    fun `is in kyc region returns false as country code not found`() = runTest {
        // Arrange
        val countryCode = "US"
        val countryList =
            listOf(Region.Country("UK", "United Kingdom", true, emptyList()))
        whenever(eligibilityService.getCountriesList(GetRegionScope.Kyc))
            .thenReturn(Outcome.Success(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        // Act
        val testObserver = subject.isInKycRegion().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
    }

    @Test
    fun `is in kyc region returns true as country code is in list`() = runTest {
        // Arrange
        val countryCode = "UK"
        val countryList =
            listOf(Region.Country("UK", "United Kingdom", true, emptyList()))
        whenever(eligibilityService.getCountriesList(GetRegionScope.Kyc))
            .thenReturn(Outcome.Success(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        // Act
        val testObserver = subject.isInKycRegion().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }

    @Test
    fun `get kyc status returns none as error fetching user object`() {
        // Arrange
        whenever(userService.getUser())
            .thenReturn(Single.error { Throwable() })
        // Act
        val testObserver = subject.getKycStatus().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(KycState.None)
    }

    @Test
    fun `get kyc status returns user object status`() {
        // Arrange
        val kycState = KycState.Verified
        whenever(userService.getUser())
            .thenReturn(Single.just(getBlankNabuUser(kycState)))
        // Act
        val testObserver = subject.getKycStatus().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(kycState)
    }

    @Test
    fun `should display kyc returns false as in wrong region and no account`() = runTest {
        // Arrange
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.error { Throwable() })
        val countryCode = "US"
        val countryList =
            listOf(Region.Country("UK", "United Kingdom", true, emptyList()))
        whenever(eligibilityService.getCountriesList(GetRegionScope.Kyc))
            .thenReturn(Outcome.Success(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        // Act
        val testObserver = subject.shouldDisplayKyc().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
    }

    @Test
    fun `should display kyc returns true as in correct region but no account`() = runTest {
        // Arrange
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.error { Throwable() })
        val countryCode = "UK"
        val countryList =
            listOf(Region.Country("UK", "United Kingdom", true, emptyList()))
        whenever(eligibilityService.getCountriesList(GetRegionScope.Kyc))
            .thenReturn(Outcome.Success(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        // Act
        val testObserver = subject.shouldDisplayKyc().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }

    @Test
    fun `should display kyc returns true as in wrong region but has account`() = runTest {
        // Arrange
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        val countryCode = "US"
        val countryList =
            listOf(Region.Country("UK", "United Kingdom", true, emptyList()))
        whenever(eligibilityService.getCountriesList(GetRegionScope.Kyc))
            .thenReturn(Outcome.Success(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        // Act
        val testObserver = subject.shouldDisplayKyc().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }

    @Test
    fun `get settings kyc state should return hidden as no account and wrong country`() = runTest {
        // Arrange
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.error { Throwable() })
        val countryCode = "US"
        val countryList =
            listOf(Region.Country("UK", "United Kingdom", true, emptyList()))
        whenever(eligibilityService.getCountriesList(GetRegionScope.Kyc))
            .thenReturn(Outcome.Success(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        // Act
        val testObserver = subject.getSettingsKycStateTier().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(KycTiers.default())
    }

    @Test
    fun `get settings kyc state should return unverified`() = runTest {
        // Arrange
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        val countryCode = "US"
        val countryList =
            listOf(Region.Country("UK", "United Kingdom", true, emptyList()))
        whenever(eligibilityService.getCountriesList(GetRegionScope.Kyc))
            .thenReturn(Outcome.Success(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        whenever(tierService.tiers())
            .thenReturn(Single.just(tiers(KycTierState.Pending, KycTierState.Pending)))
        // Act
        val testObserver = subject.getSettingsKycStateTier().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue {
            it.isPendingFor(KycTierLevel.SILVER)
        }
    }

    @Test
    fun `get settings kyc state should return verified`() = runTest {
        // Arrange
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        val countryCode = "US"
        val countryList =
            listOf(Region.Country("UK", "United Kingdom", true, emptyList()))
        whenever(eligibilityService.getCountriesList(GetRegionScope.Kyc))
            .thenReturn(Outcome.Success(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        whenever(tierService.tiers())
            .thenReturn(Single.just(tiers(KycTierState.Verified, KycTierState.Verified)))
        // Act
        val testObserver = subject.getSettingsKycStateTier().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue {
            it.isApprovedFor(KycTierLevel.GOLD)
        }
    }

    @Test
    fun `get settings kyc state should return failed`() = runTest {
        // Arrange
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        val countryCode = "US"
        val countryList =
            listOf(Region.Country("UK", "United Kingdom", true, emptyList()))
        whenever(eligibilityService.getCountriesList(GetRegionScope.Kyc))
            .thenReturn(Outcome.Success(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        whenever(tierService.tiers())
            .thenReturn(Single.just(tiers(KycTierState.Rejected, KycTierState.Rejected)))
        // Act
        val testObserver = subject.getKycTierStatus().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue {
            it.isRejectedFor(KycTierLevel.GOLD) &&
                it.isRejectedFor(KycTierLevel.SILVER)
        }
    }

    @Test
    fun `get settings kyc state should return in progress`() = runTest {
        // Arrange
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        val countryCode = "US"
        val countryList =
            listOf(Region.Country("UK", "United Kingdom", true, emptyList()))
        whenever(eligibilityService.getCountriesList(GetRegionScope.Kyc))
            .thenReturn(Outcome.Success(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        whenever(tierService.tiers())
            .thenReturn(Single.just(tiers(KycTierState.Pending, KycTierState.None)))
        // Act
        val testObserver = subject.getSettingsKycStateTier().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue {
            it.isPendingFor(KycTierLevel.SILVER)
        }
    }

    @Test
    fun `get settings kyc state should return in review`() = runTest {
        // Arrange
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        val countryCode = "US"
        val countryList =
            listOf(Region.Country("UK", "United Kingdom", true, emptyList()))
        whenever(eligibilityService.getCountriesList(GetRegionScope.Kyc))
            .thenReturn(Outcome.Success(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        whenever(tierService.tiers())
            .thenReturn(Single.just(tiers(KycTierState.UnderReview, KycTierState.None)))
        // Act
        val testObserver = subject.getSettingsKycStateTier().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue {
            it.isUnderReviewFor(KycTierLevel.SILVER)
        }
    }

    @Test
    fun `get settings kyc state should return state from tiers service`() = runTest {
        // Arrange
        whenever(tierService.tiers()).thenReturn(Single.just(tiers(KycTierState.Verified, KycTierState.Verified)))
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        val countryCode = "US"
        val countryList =
            listOf(Region.Country("UK", "United Kingdom", true, emptyList()))
        whenever(eligibilityService.getCountriesList(GetRegionScope.Kyc))
            .thenReturn(Outcome.Success(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        // Act
        val testObserver = subject.getSettingsKycStateTier().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue {
            it.isApprovedFor(KycTierLevel.GOLD)
        }
    }

    @Test
    fun `get user state successful, returns created`() {
        // Arrange
        whenever(userService.getUser())
            .thenReturn(Single.just(getBlankNabuUser().copy(state = UserState.Created)))
        // Act
        val testObserver = subject.getUserState().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(UserState.Created)
    }
}
