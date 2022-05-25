package piuk.blockchain.android.rating.presentaion

import app.cash.turbine.test
import com.blockchain.preferences.AuthPrefs
import com.blockchain.testutils.CoroutineTestRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.rating.domain.model.AppRating
import piuk.blockchain.android.rating.domain.service.AppRatingService

@OptIn(ExperimentalCoroutinesApi::class)
class AppRatingViewModelTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    var coroutineTestRule = CoroutineTestRule()

    private val appRatingService = mockk<AppRatingService>()
    private val authPrefs = mockk<AuthPrefs>()

    private lateinit var viewModel: AppRatingViewModel

    private val appRatingTriggerSource = AppRatingTriggerSource.DASHBOARD

    private val feedback = "feedback"
    private val walletGuid = "walletGuid"
    private val SEPARATOR = ", ------ "
    private val SCREEN = "Screen: "
    private val WALLET_ID = "Wallet id: "

    @Before
    fun setUp() {
        every { authPrefs.walletGuid } returns walletGuid
        every { appRatingService.saveRatingDateForLater() } just Runs
        every { appRatingService.markRatingCompleted() } just Runs

        viewModel = AppRatingViewModel(
            appRatingService = appRatingService,
            authPrefs = authPrefs
        )
    }

    @Test
    fun `WHEN RatingCanceled is called, THEN saveRatingDateForLater should be called, dismiss should be true`() =
        runTest {
            viewModel.viewState.test {
                viewModel.onIntent(AppRatingIntents.RatingCanceled)

                verify(exactly = 1) { appRatingService.saveRatingDateForLater() }

                assertEquals(true, expectMostRecentItem().dismiss)
            }
        }

    @Test
    fun `GIVEN stars greater than threshold, WHEN StarsSubmitted is called, THEN promptInAppReview should be true`() =
        runTest {
            coEvery { appRatingService.getThreshold() } returns 3

            viewModel.viewState.test {
                viewModel.onIntent(AppRatingIntents.StarsSubmitted(stars = 4))

                assertEquals(true, expectMostRecentItem().promptInAppReview)
            }
        }

    @Test
    fun `GIVEN stars less than or eq threshold, WHEN StarsSubmitted is called, THEN Feedback should be called`() =
        runTest {
            coEvery { appRatingService.getThreshold() } returns 3

            viewModel.navigationEventFlow.test {
                viewModel.onIntent(AppRatingIntents.StarsSubmitted(stars = 1))

                assertEquals(AppRatingNavigationEvent.Feedback, expectMostRecentItem())
            }
        }

    @Test
    fun `WHEN FeedbackSubmitted is called, THEN Completed(withFeedback true) should be called`() =
        runTest {
            viewModel.navigationEventFlow.test {
                viewModel.onIntent(AppRatingIntents.FeedbackSubmitted(feedback = feedback))

                assertEquals(AppRatingNavigationEvent.Completed(withFeedback = true), expectMostRecentItem())
            }
        }

    @Test
    fun `WHEN InAppReviewCompleted is called, THEN promptInAppReview should be false, Completed(withFeedback false) should be called`() =
        runTest {
            combine(viewModel.navigationEventFlow, viewModel.viewState) { event, view ->
                Pair(event, view)
            }.test {
                viewModel.onIntent(AppRatingIntents.InAppReviewCompleted(successful = true))

                val item = expectMostRecentItem()
                assertEquals(AppRatingNavigationEvent.Completed(withFeedback = false), item.first)
                assertEquals(false, item.second.promptInAppReview)
            }
        }

    @Test
    fun `GIVEN 3 stars, feedback message, WHEN RatingCompleted is called, THEN postRatingData should be called with correct data`() =
        runTest {
            val data = AppRating(
                rating = 3,
                feedback = "$feedback$SEPARATOR$WALLET_ID$walletGuid$SEPARATOR$SCREEN${appRatingTriggerSource.value}"
            )
            coEvery { appRatingService.getThreshold() } returns 3
            coEvery { appRatingService.postRatingData(any()) } returns true

            viewModel.viewCreated(appRatingTriggerSource)
            viewModel.onIntent(AppRatingIntents.StarsSubmitted(stars = 3))
            viewModel.onIntent(AppRatingIntents.FeedbackSubmitted(feedback = feedback))
            viewModel.onIntent(AppRatingIntents.RatingCompleted)

            coVerify { appRatingService.postRatingData(data) }
        }

    @Test
    fun `GIVEN postRatingData successful, retrigger false, WHEN RatingCompleted is called, THEN markRatingCompleted should be called`() =
        runTest {
            coEvery { appRatingService.postRatingData(any()) } returns true

            viewModel.onIntent(AppRatingIntents.InAppReviewCompleted(true)) // = retrigger false
            viewModel.onIntent(AppRatingIntents.RatingCompleted)

            verify(exactly = 1) { appRatingService.markRatingCompleted() }
        }

    @Test
    fun `GIVEN postRatingData successful, retrigger true, WHEN RatingCompleted is called, THEN saveRatingDateForLater should be called`() =
        runTest {
            coEvery { appRatingService.postRatingData(any()) } returns true

            viewModel.onIntent(AppRatingIntents.InAppReviewCompleted(false)) // = retrigger true
            viewModel.onIntent(AppRatingIntents.RatingCompleted)

            verify(exactly = 1) { appRatingService.saveRatingDateForLater() }
        }

    @Test
    fun `GIVEN postRatingData unsuccessful, retrigger false, WHEN RatingCompleted is called, THEN markRatingCompleted should be called`() =
        runTest {
            coEvery { appRatingService.postRatingData(any()) } returns false

            viewModel.onIntent(AppRatingIntents.InAppReviewCompleted(true)) // = retrigger false
            viewModel.onIntent(AppRatingIntents.RatingCompleted)

            verify(exactly = 1) { appRatingService.saveRatingDateForLater() }
        }

    @Test
    fun `GIVEN postRatingData unsuccessful, retrigger true, WHEN RatingCompleted is called, THEN saveRatingDateForLater should be called`() =
        runTest {
            coEvery { appRatingService.postRatingData(any()) } returns true

            viewModel.onIntent(AppRatingIntents.InAppReviewCompleted(false)) // = retrigger true
            viewModel.onIntent(AppRatingIntents.RatingCompleted)

            verify(exactly = 1) { appRatingService.saveRatingDateForLater() }
        }
}
