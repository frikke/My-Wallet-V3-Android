package piuk.blockchain.android.rating.presentaion

import com.blockchain.koin.payloadScopeQualifier
import com.google.android.play.core.review.ReviewManagerFactory
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import piuk.blockchain.android.rating.presentaion.inappreview.InAppReviewSettings
import piuk.blockchain.android.rating.presentaion.inappreview.InAppReviewSettingsImpl

val appRatingPresentationModule = module {
    scope(payloadScopeQualifier) {

        scoped<InAppReviewSettings> {
            InAppReviewSettingsImpl(
                reviewManager = ReviewManagerFactory.create(get())
            )
        }

        viewModel {
            AppRatingViewModel(
                appRatingService = get()
            )
        }
    }
}
