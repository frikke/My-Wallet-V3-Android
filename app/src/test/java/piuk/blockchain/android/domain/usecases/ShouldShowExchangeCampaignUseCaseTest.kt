package piuk.blockchain.android.domain.usecases

import com.blockchain.domain.mercuryexperiments.MercuryExperimentsService
import com.blockchain.domain.mercuryexperiments.model.MercuryExperiments
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.preferences.ExchangeCampaignPrefs
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import org.junit.Test

class ShouldShowExchangeCampaignUseCaseTest {

    private val exchangeWAPromptFF: FeatureFlag = mock()
    private val mercuryExperimentsService: MercuryExperimentsService = mock()
    private val subject = ShouldShowExchangeCampaignUseCase(
        exchangeWAPromptFF = exchangeWAPromptFF,
        exchangeCampaignPrefs = FakeExchangeCampaignPrefs(),
        mercuryExperimentsService = mercuryExperimentsService
    )

    @Test
    fun `given exchange WA prompt feature flag is disabled then campaign should not be shown`() {
        whenever(exchangeWAPromptFF.enabled).thenReturn(Single.just(false))

        subject.invoke()
            .test()
            .assertValue(false)
    }

    @Test
    fun `given exchange WA prompt feature flag is enabled and campaign was dismissed less than twice then campaign should be shown`() {
        whenever(exchangeWAPromptFF.enabled).thenReturn(Single.just(true))
        whenever(mercuryExperimentsService.getMercuryExperiments()).thenReturn(
            Single.just(MercuryExperiments(walletAwarenessPrompt = 0))
        )

        subject.onDismiss()

        subject.invoke()
            .test()
            .assertValue(true)
    }

    @Test
    fun `given exchange WA prompt feature flag is enabled and campaign was dismissed more than once then campaign should not be shown`() {
        whenever(exchangeWAPromptFF.enabled).thenReturn(Single.just(true))
        whenever(mercuryExperimentsService.getMercuryExperiments()).thenReturn(
            Single.just(MercuryExperiments(walletAwarenessPrompt = 0))
        )

        subject.onDismiss()
        subject.onDismiss()

        subject.invoke()
            .test()
            .assertValue(false)
    }

    @Test
    fun `given exchange WA prompt feature flag is enabled and action was taken then campaign should not be shown`() {
        whenever(exchangeWAPromptFF.enabled).thenReturn(Single.just(true))
        whenever(mercuryExperimentsService.getMercuryExperiments()).thenReturn(
            Single.just(MercuryExperiments(walletAwarenessPrompt = 0))
        )

        subject.onActionTaken()

        subject.invoke()
            .test()
            .assertValue(false)
    }

    @Test
    fun `given exchange WA prompt feature flag is enabled and and user is in experiment and campaign was dismissed less than twice then campaign should be shown`() {
        whenever(exchangeWAPromptFF.enabled).thenReturn(Single.just(true))
        whenever(mercuryExperimentsService.getMercuryExperiments()).thenReturn(
            Single.just(MercuryExperiments(walletAwarenessPrompt = 0))
        )

        subject.invoke()
            .test()
            .assertValue(true)
    }

    @Test
    fun `given exchange WA prompt feature flag is enabled and and user is not in experiment then campaign should not be shown`() {
        whenever(exchangeWAPromptFF.enabled).thenReturn(Single.just(true))
        whenever(mercuryExperimentsService.getMercuryExperiments()).thenReturn(
            Single.just(MercuryExperiments(walletAwarenessPrompt = null))
        )

        subject.invoke()
            .test()
            .assertValue(false)
    }
}

private class FakeExchangeCampaignPrefs : ExchangeCampaignPrefs {
    override var dismissCount: Int = ExchangeCampaignPrefs.DEFAULT_DISMISS_COUNT
    override var actionTaken: Boolean = ExchangeCampaignPrefs.DEFAULT_ACTION_TAKEN
}
