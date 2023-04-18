package piuk.blockchain.android.ui.kyc.limits

import com.blockchain.android.testutils.rxInit
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.limits.Feature
import com.blockchain.core.limits.FeatureLimit
import com.blockchain.core.limits.FeatureWithLimit
import com.blockchain.core.limits.TxLimitPeriod
import com.blockchain.core.limits.TxPeriodicLimit
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.testutils.USD
import com.blockchain.testutils.numberToBigInteger
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class KycLimitsModelTest {

    private val interactor: KycLimitsInteractor = mock {
        on { fetchLimits() }.thenReturn(Single.just(TEST_FEATURE_LIMITS))
        on { fetchHighestApprovedTier() }.thenReturn(Single.just(KycTier.SILVER))
        on { fetchIsKycRejected() }.thenReturn(Single.just(false))
        on { fetchIsEligibleToKyc() }.thenReturn(Single.just(true))
    }

    private lateinit var model: KycLimitsModel

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = KycLimitsModel(
            interactor = interactor,
            uiScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            remoteLogger = mock()
        )
    }

    @Test
    fun `fetching limits should show loading and should display results on success`() {
        whenever(interactor.fetchLimits()).thenReturn(Single.just(TEST_FEATURE_LIMITS))

        val state = model.state.test()
        model.process(KycLimitsIntent.FetchLimitsAndTiers)
        state.assertValueAt(0, KycLimitsState())
        state.assertValueAt(1) {
            it.isLoading
        }
        state.assertValueAt(2) {
            !it.isLoading &&
                it.featuresWithLimits == TEST_FEATURE_LIMITS
        }
    }

    @Test
    fun `fetching limits should show error on failure`() {
        val error = IllegalStateException("test")
        whenever(interactor.fetchLimits()).thenReturn(Single.error(error))

        val state = model.state.test()
        model.process(KycLimitsIntent.FetchLimitsAndTiers)
        state.assertValueAt(2) {
            !it.isLoading && it.errorState == KycLimitsError.FullscreenError(error)
        }
    }

    @Test
    fun `given bronze user, on fetching tiers success should show new kyc header and hide kyc tier row`() {
        whenever(interactor.fetchHighestApprovedTier()).thenReturn(Single.just(KycTier.BRONZE))

        val state = model.state.test()
        model.process(KycLimitsIntent.FetchLimitsAndTiers)

        state.assertValueAt(2) {
            it.header == Header.NEW_KYC &&
                it.currentKycTierRow == CurrentKycTierRow.HIDDEN
        }
    }

    @Test
    fun `given silver user, on fetching tiers success should show upgrade to gold header and show silver kyc tier row`() {
        whenever(interactor.fetchHighestApprovedTier()).thenReturn(Single.just(KycTier.SILVER))

        val state = model.state.test()
        model.process(KycLimitsIntent.FetchLimitsAndTiers)

        state.assertValueAt(0, KycLimitsState())
        state.assertValueAt(2) {
            it.header == Header.UPGRADE_TO_GOLD &&
                it.currentKycTierRow == CurrentKycTierRow.SILVER
        }
    }

    @Test
    fun `given gold user, on fetching tiers success should show max tier reached header and show gold kyc tier row`() {
        whenever(interactor.fetchHighestApprovedTier()).thenReturn(Single.just(KycTier.GOLD))

        val state = model.state.test()
        model.process(KycLimitsIntent.FetchLimitsAndTiers)

        state.assertValueAt(2) {
            it.header == Header.MAX_TIER_REACHED &&
                it.currentKycTierRow == CurrentKycTierRow.GOLD
        }
    }

    @Test
    fun `given kyc rejected, on fetching tiers success should show max tier reached header and kyc tier row`() {
        whenever(interactor.fetchIsKycRejected()).thenReturn(Single.just(true))

        val state = model.state.test()
        model.process(KycLimitsIntent.FetchLimitsAndTiers)

        state.assertValueAt(2) {
            it.header == Header.MAX_TIER_REACHED &&
                it.currentKycTierRow == CurrentKycTierRow.HIDDEN
        }
    }

    @Test
    fun `on fetching highest approved tier failure should show max tier reached header and hide kyc tier`() {
        val error = IllegalStateException("test")
        whenever(interactor.fetchHighestApprovedTier()).thenReturn(Single.error(error))

        val state = model.state.test()
        model.process(KycLimitsIntent.FetchLimitsAndTiers)

        state.assertValueAt(2) {
            it.header == Header.HIDDEN &&
                it.currentKycTierRow == CurrentKycTierRow.HIDDEN
        }
    }

    @Test
    fun `on fetching kyc denied failure should show max tier reached header and hide kyc tier`() {
        val error = IllegalStateException("test")
        whenever(interactor.fetchIsKycRejected()).thenReturn(Single.error(error))

        val state = model.state.test()
        model.process(KycLimitsIntent.FetchLimitsAndTiers)

        state.assertValueAt(2) {
            it.header == Header.HIDDEN &&
                it.currentKycTierRow == CurrentKycTierRow.HIDDEN
        }
    }

    @Test
    fun `given gold kyc pending on fetching tier to upgrade to gold success should open upgrade now sheet`() {
        val isGoldPending = true
        whenever(interactor.fetchIsGoldKycPending()).thenReturn(Single.just(isGoldPending))

        val state = model.state.test()
        model.process(KycLimitsIntent.UpgradeToGoldHeaderCtaClicked)

        state.assertValueAt(1) {
            it.activeSheet == KycLimitsSheet.UpgradeNow(isGoldPending) &&
                it.navigationAction == KycLimitsNavigationAction.None
        }
    }

    @Test
    fun `given gold kyc not pending on fetching tier to upgrade to gold success should start kyc`() {
        val isGoldPending = false
        whenever(interactor.fetchIsGoldKycPending()).thenReturn(Single.just(isGoldPending))

        val state = model.state.test()
        model.process(KycLimitsIntent.UpgradeToGoldHeaderCtaClicked)

        state.assertValueAt(1) {
            it.activeSheet == KycLimitsSheet.None &&
                it.navigationAction == KycLimitsNavigationAction.StartKyc
        }
    }

    @Test
    fun `on fetching tier to upgrade to gold failure should show sheet error`() {
        val error = IllegalStateException("test")
        whenever(interactor.fetchIsGoldKycPending()).thenReturn(Single.error(error))

        val state = model.state.test()
        model.process(KycLimitsIntent.UpgradeToGoldHeaderCtaClicked)

        state.assertValueAt(1) {
            it.activeSheet == KycLimitsSheet.None &&
                it.errorState == KycLimitsError.SheetError(error)
        }
    }

    companion object {
        private val TEST_FEATURE_LIMITS = listOf(
            FeatureWithLimit(
                Feature.REWARDS,
                FeatureLimit.Limited(
                    TxPeriodicLimit(
                        amount = FiatValue.fromMinor(USD, 4000L.numberToBigInteger()),
                        period = TxLimitPeriod.MONTHLY,
                        effective = true
                    )
                )
            ),
            FeatureWithLimit(Feature.FIAT_DEPOSIT, FeatureLimit.Infinite),
            FeatureWithLimit(Feature.FIAT_WITHDRAWAL, FeatureLimit.Unspecified),
            FeatureWithLimit(Feature.RECEIVE_TO_TRADING, FeatureLimit.Disabled)
        )
    }
}
