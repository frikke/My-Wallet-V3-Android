package piuk.blockchain.android.ui.dashboard.walletmode

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.blockchain.analytics.Analytics
import com.blockchain.commonarch.presentation.mvi_v2.MVIBottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.extensions.exhaustive
import com.blockchain.koin.payloadScope
import com.blockchain.presentation.backup.BackupPhraseActivity
import com.blockchain.presentation.onboarding.DeFiOnboardingActivity
import com.blockchain.walletmode.WalletMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope
import org.koin.java.KoinJavaComponent
import piuk.blockchain.android.R

class WalletModeSelectionBottomSheet :
    MVIBottomSheet<WalletModeSelectionViewState>(),
    Analytics by KoinJavaComponent.get(Analytics::class.java),
    NavigationRouter<WalletModeSelectionNavigationEvent>,
    AndroidScopeComponent {

    override val scope: Scope
        get() = payloadScope

    private val viewModel: WalletModeSelectionViewModel by viewModel()

    private val onDeFiOnboardingResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            /**
             * IMPORTANT
             *
             * mandatory Dispatchers.IO otherwise the nav event is not caught as we're coming back from another activity
             * has to do with running things serially in main thread where [Lifecycle.repeatOnLifecycle]
             * is supposed to start collecting
             */
            lifecycleScope.launch(Dispatchers.IO) {
                lifecycleScope.launchWhenStarted { deFiOnboardingComplete() }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setupViewModel()

        viewModel.onIntent(WalletModeSelectionIntent.LoadInitialData)

        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(dimensionResource(id = R.dimen.tiny_spacing))),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SheetHeader(
                            onClosePress = { dismiss() },
                            shouldShowDivider = false
                        )
                        WalletModes(viewModel)
                    }
                }
            }
        }
    }

    private fun setupViewModel() {
        bindViewModel(viewModel, this, ModelConfigArgs.NoArgs)
    }

    override fun onStateUpdated(state: WalletModeSelectionViewState) {}

    override fun route(navigationEvent: WalletModeSelectionNavigationEvent) {
        when (navigationEvent) {
            is WalletModeSelectionNavigationEvent.PhraseRecovery -> {
                launchPhraseRecovery(onboardingRequired = navigationEvent.onboardingRequired)
            }

            is WalletModeSelectionNavigationEvent.Close -> {
                when (navigationEvent.walletMode) {
                    WalletMode.CUSTODIAL -> {
                        logEvent(WalletModeAnalyticsEvents.SwitchedToTrading)
                    }

                    WalletMode.NON_CUSTODIAL -> {
                        logEvent(WalletModeAnalyticsEvents.SwitchedToDefi)
                    }

                    else -> {
                        /*n/a*/
                    }
                }

                dismiss()
            }
        }.exhaustive
    }

    private fun launchPhraseRecovery(onboardingRequired: Boolean) {
        onDeFiOnboardingResult.launch(
            if (onboardingRequired) {
                DeFiOnboardingActivity.newIntent(context = requireContext())
            } else {
                BackupPhraseActivity.newIntent(context = requireContext())
            }
        )
    }

    private fun deFiOnboardingComplete() {
        viewModel.onIntent(WalletModeSelectionIntent.DeFiOnboardingComplete)
    }

    companion object {
        fun newInstance(): WalletModeSelectionBottomSheet = WalletModeSelectionBottomSheet()
    }
}
