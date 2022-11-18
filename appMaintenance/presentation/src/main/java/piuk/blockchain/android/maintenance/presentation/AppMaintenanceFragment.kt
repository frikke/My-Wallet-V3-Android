package piuk.blockchain.android.maintenance.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.blockchain.commonarch.presentation.mvi_v2.MVIBottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.commonarch.presentation.mvi_v2.disableDragging
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.utils.openUrl
import com.blockchain.enviroment.EnvironmentConfig
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

    private val environmentConfig: EnvironmentConfig by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        isCancelable = false

        disableDragging()

        setupViewModel()

        return ComposeView(requireContext()).apply {
            setContent {
                ScreenContent()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Calling in [onResume] to update the status automatically when returning to the app
        viewModel.onIntent(AppMaintenanceIntents.GetStatus)
    }

    private fun setupViewModel() {
        bindViewModel(viewModel, this, ModelConfigArgs.NoArgs)
    }

    /**
     * Figma: https://www.figma.com/file/Khjv2OKUvZ7xwTx2qmSadw/iOS---Upgrade-Prompts?node-id=0%3A1
     */
    @Composable
    private fun ScreenContent() {
        val state = viewModel.viewState.collectAsState()
        AppMaintenanceScreen(
            isDebugBuild = environmentConfig.isRunningInDebugMode(),
            debugSkip = ::resumeAppFlow,
            uiState = state.value,
            button1OnClick = viewModel::onIntent,
            button2OnClick = viewModel::onIntent
        )
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
        context?.run { openUrl(url) }
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
        view?.let {
            BlockchainSnackbar.make(
                it,
                getString(R.string.app_maintenance_error_inapp_update),
                type = SnackbarType.Error
            ).show()
        }
    }

    companion object {
        fun newInstance() = AppMaintenanceFragment()
    }
}
