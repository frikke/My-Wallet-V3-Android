package piuk.blockchain.android.rating.presentaion

import com.blockchain.koin.payloadScopeQualifier
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appRatingPresentationModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            AppRatingViewModel(
                appRatingService = get()
            )
        }
    }
}
