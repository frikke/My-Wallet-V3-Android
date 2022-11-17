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
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.koin.payloadScope
import com.blockchain.presentation.koin.scopedInject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope
import piuk.blockchain.android.rating.presentaion.composable.AppRatingNavHost
import piuk.blockchain.android.rating.presentaion.inappreview.InAppReviewSettings

class AppRatingFragment : DialogFragment(), AndroidScopeComponent {

    override var scope: Scope? = payloadScope

    private val viewModel: AppRatingViewModel by viewModel()

    private val appRatingTriggerSource: AppRatingTriggerSource by lazy {
        arguments?.getParcelable<AppRatingTriggerSource>(AppRatingTriggerSource.ARGS_KEY) ?: error(
            "missing AppRatingTriggerSource"
        )
    }

    private val inAppReviewSettings: InAppReviewSettings by scopedInject()

    private val environmentConfig: EnvironmentConfig by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        isCancelable = environmentConfig.isRunningInDebugMode()

        collectViewState()

        lifecycleScope.launch { inAppReviewSettings.init(requireContext()) }

        return ComposeView(requireContext()).apply {
            setContent {
                AppRatingNavHost(viewModel, appRatingTriggerSource)
            }
        }
    }

    private fun collectViewState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.viewState.collect { viewState ->
                    with(viewState) {
                        when {
                            dismiss -> {
                                dismiss()
                            }

                            promptInAppReview -> {
                                triggerInAppReview()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Try to trigger in-app review, and notify the vm of the status so it can:
     * * fire the api if successful
     * * save date to retrigger in 1 month otherwise
     *
     * @see AppRatingViewModel.inAppReviewRequested
     */
    private fun triggerInAppReview() {
        lifecycleScope.launch {
            inAppReviewSettings.triggerAppReview(requireActivity())
                .also { successfulTrigger ->
                    viewModel.onIntent(AppRatingIntents.InAppReviewRequested(successful = successfulTrigger))
                }
        }
    }

    companion object {
        fun newInstance(appRatingTriggerSource: AppRatingTriggerSource) = AppRatingFragment().apply {
            arguments = Bundle().apply {
                putParcelable(AppRatingTriggerSource.ARGS_KEY, appRatingTriggerSource)
            }
        }

        val TAG = AppRatingFragment::class.simpleName
    }
}
