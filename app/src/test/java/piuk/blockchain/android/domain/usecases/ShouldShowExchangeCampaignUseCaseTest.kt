package piuk.blockchain.android.domain.usecases

import com.blockchain.featureflag.FeatureFlag
import com.blockchain.preferences.ExchangeCampaignPrefs
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import org.junit.Test

class ShouldShowExchangeCampaignUseCaseTest {

    private val exchangeWAPromptFF: FeatureFlag = mock()
    private val subject = ShouldShowExchangeCampaignUseCase(
        exchangeWAPromptFF = exchangeWAPromptFF,
        exchangeCampaignPrefs = FakeExchangeCampaignPrefs()
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

        subject.onDismiss()

        subject.invoke()
            .test()
            .assertValue(true)
    }

    @Test
    fun `given exchange WA prompt feature flag is enabled and campaign was dismissed more than once then campaign should not be shown`() {
        whenever(exchangeWAPromptFF.enabled).thenReturn(Single.just(true))

        subject.onDismiss()
        subject.onDismiss()

        subject.invoke()
            .test()
            .assertValue(false)
    }

    @Test
    fun `given exchange WA prompt feature flag is enabled and action was taken then campaign should not be shown`() {
        whenever(exchangeWAPromptFF.enabled).thenReturn(Single.just(true))

        subject.onActionTaken()

        subject.invoke()
            .test()
            .assertValue(false)
    }
}

private class FakeExchangeCampaignPrefs : ExchangeCampaignPrefs {
    override var dismissCount: Int = ExchangeCampaignPrefs.DEFAULT_DISMISS_COUNT
    override var actionTaken: Boolean = ExchangeCampaignPrefs.DEFAULT_ACTION_TAKEN
}
