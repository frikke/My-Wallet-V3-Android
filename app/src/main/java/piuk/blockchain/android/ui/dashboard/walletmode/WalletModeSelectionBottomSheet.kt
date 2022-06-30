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
import com.blockchain.commonarch.presentation.mvi_v2.MVIBottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.extensions.exhaustive
import com.blockchain.koin.payloadScope
import com.blockchain.presentation.onboarding.DeFiOnboardingActivity
import com.blockchain.walletmode.WalletMode
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope
import piuk.blockchain.android.R

class WalletModeSelectionBottomSheet :
    MVIBottomSheet<WalletModeSelectionViewState>(),
    NavigationRouter<WalletModeSelectionNavigationEvent>,
    AndroidScopeComponent {
    interface Host {
        fun onActiveModeChanged(
            walletMode: WalletMode,
        )
    }

    val host: Host by lazy {
        activity as? Host
            ?: throw IllegalStateException("Host activity is not a WalletModeSelectionBottomSheet.Host")
    }

    override val scope: Scope
        get() = payloadScope

    private val viewModel: WalletModeSelectionViewModel by viewModel()

    private val onDeFiOnboardingResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            deFiOnboardingComplete()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setupViewModel()

        viewModel.onIntent(WalletModeSelectionIntent.LoadInitialData)

        return ComposeView(requireContext()).apply {
            setContent {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(dimensionResource(id = R.dimen.tiny_margin))),
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

    private fun setupViewModel() {
        bindViewModel(viewModel, this, ModelConfigArgs.NoArgs)
    }

    override fun onStateUpdated(state: WalletModeSelectionViewState) {
        with(state) {
            when {
                newSelectedWalletMode != null -> {
                    host.onActiveModeChanged(newSelectedWalletMode)
                    dismiss()
                }
            }
        }
    }

    override fun route(navigationEvent: WalletModeSelectionNavigationEvent) {
        when (navigationEvent) {
            WalletModeSelectionNavigationEvent.DeFiOnboarding -> {
                launchDeFiOnboarding()
            }
        }.exhaustive
    }

    private fun launchDeFiOnboarding() {
        onDeFiOnboardingResult.launch(DeFiOnboardingActivity.newIntent(context = requireContext()))
    }

    private fun deFiOnboardingComplete() {
        viewModel.onIntent(WalletModeSelectionIntent.DeFiOnboardingComplete)
    }

    companion object {
        fun newInstance(): WalletModeSelectionBottomSheet = WalletModeSelectionBottomSheet()
    }
}
