package piuk.blockchain.android.rating.presentaion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.blockchain.koin.payloadScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ViewModelOwner
import org.koin.androidx.viewmodel.scope.getViewModel
import piuk.blockchain.android.rating.presentaion.composable.AppRatingNavHost
import piuk.blockchain.android.rating.presentaion.inappreview.InAppReviewSettings

class AppRatingFragment : DialogFragment() {

    private val viewModel: AppRatingViewModel by lazy {
        payloadScope.getViewModel(owner = { ViewModelOwner.from(this) })
    }

    private val inAppReviewSettings: InAppReviewSettings by payloadScope.inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        collectViewState()

        inAppReviewSettings.init(requireContext())

        return ComposeView(requireContext()).apply {
            setContent {
                AppRatingNavHost(
                    viewModel = viewModel,
                    triggerInAppReview = ::triggerInAppReview
                )
            }
        }
    }

    private fun collectViewState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.viewState.collect { viewState ->
                    with(viewState) {
                        if (dismiss) {
                            dismiss()
                            return@collect
                        }
                    }
                }
            }
        }
    }

    private fun triggerInAppReview() {
        inAppReviewSettings.triggerAppReview(requireActivity()) {
            viewModel.onIntent(AppRatingIntents.InAppReviewCompleted)
        }
    }

    companion object {
        fun newInstance() = AppRatingFragment()
    }
}
