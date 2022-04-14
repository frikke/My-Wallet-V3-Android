package piuk.blockchain.android.maintenance.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.blockchain.commonarch.presentation.mvi_v2.MVIBottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.extensions.exhaustive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import piuk.blockchain.android.maintenance.presentation.appupdateapi.InAppUpdateSettings

class AppMaintenanceFragment :
    MVIBottomSheet<AppMaintenanceViewState>(),
    NavigationRouter<AppMaintenanceNavigationEvent> {

    private val viewModel: AppMaintenanceViewModel by viewModel()
    private val sharedViewModel: AppMaintenanceSharedViewModel by sharedViewModel()

    private val inAppUpdateSettings: InAppUpdateSettings by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        isCancelable = false

        setupViewModel()

        return ComposeView(requireContext()).apply {
            setContent {
                ScreenContent()
            }
        }
    }

    private fun setupViewModel() {
        lifecycle.addObserver(viewModel)
        bindViewModel(viewModel, this, ModelConfigArgs.NoArgs)
    }

    /**
     * Figma: https://www.figma.com/file/Khjv2OKUvZ7xwTx2qmSadw/iOS---Upgrade-Prompts?node-id=0%3A1
     */
    @Composable
    private fun ScreenContent() {
        val state = viewModel.viewState.collectAsState()
        AppMaintenanceScreen(state.value, viewModel)
    }

    override fun onStateUpdated(state: AppMaintenanceViewState) {
    }

    override fun route(navigationEvent: AppMaintenanceNavigationEvent) {
        when (navigationEvent) {

            is AppMaintenanceNavigationEvent.OpenUrl -> {
                openUrl(navigationEvent.url)
            }

            AppMaintenanceNavigationEvent.LaunchAppUpdate -> {
                launchAppUpdate()
            }

            AppMaintenanceNavigationEvent.ResumeAppFlow -> {
                resumeAppFlow()
            }
        }.exhaustive
    }

    private fun openUrl(url: String) {
        context.openUrl(url)
    }

    private fun launchAppUpdate() {
        activity?.let {
            lifecycleScope.launch {
                try {
                    inAppUpdateSettings.triggerOrResumeAppUpdate(it)
                } catch (e: Throwable) {
                    showError()
                }
            }
        }
    }

    private fun resumeAppFlow() {
        dismiss()
        sharedViewModel.resumeAppFlow()
    }

    private fun showError() {
        //        BlockchainSnackbar.make(
        //            composeView,
        //            getString(R.string.common_error),
        //            type = SnackbarType.Error
        //        ).show()
    }

    companion object {
        fun newInstance() = AppMaintenanceFragment()
    }

    @Preview
    @Composable
    private fun PreviewScreenContent() {
        ScreenContent()
    }
}
