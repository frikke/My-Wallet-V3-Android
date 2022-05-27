package piuk.blockchain.android.ui.kyc.additional_info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.blockchain.commonarch.presentation.mvi_v2.MVIFragment
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.extensions.exhaustive
import com.blockchain.koin.payloadScope
import com.blockchain.nabu.datamanagers.kyc.UpdateKycAdditionalInfoError
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope
import piuk.blockchain.android.KycNavXmlDirections
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener
import piuk.blockchain.android.ui.kyc.navigate

class KycAdditionalInfoFragment() : MVIFragment<KycAdditionalInfoState>(), AndroidScopeComponent {

    private val progressListener: KycProgressListener by ParentActivityDelegate(
        this
    )

    override val scope: Scope = payloadScope

    private val model: KycAdditionalInfoModel by viewModel()

    private val navigator: NavigationRouter<Navigation> = object : NavigationRouter<Navigation> {
        override fun route(navigationEvent: Navigation) {
            when (navigationEvent) {
                Navigation.LaunchVeriff -> navigate(KycNavXmlDirections.actionStartVeriff(countryCode))
            }.exhaustive
        }
    }

    private val root: TreeNode.Root by lazy {
        KycAdditionalInfoFragmentArgs.fromBundle(arguments ?: Bundle()).root
    }

    private val countryCode: String by lazy {
        KycAdditionalInfoFragmentArgs.fromBundle(arguments ?: Bundle()).countryCode
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        bindViewModel(model, navigator, Args(root, progressListener.campaignType))

        progressListener.setHostTitle(R.string.kyc_additional_info_toolbar)

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by model.viewState.collectAsState()

                KycAdditionalInfoScreen(
                    state = state,
                    onDropdownChoiceChanged = { node, newChoice ->
                        model.onIntent(KycAdditionalInfoIntent.DropdownChoiceChanged(node, newChoice))
                    },
                    onSelectionClicked = { node ->
                        model.onIntent(KycAdditionalInfoIntent.SelectionClicked(node))
                    },
                    onOpenEndedInputChanged = { node, newInput ->
                        model.onIntent(KycAdditionalInfoIntent.OpenEndedInputChanged(node, newInput))
                    },
                    onContinueClicked = {
                        model.onIntent(KycAdditionalInfoIntent.ContinueClicked)
                    },
                )
            }
        }
    }

    override fun onStateUpdated(state: KycAdditionalInfoState) {
        if (state.error != null) {
            val stringRes = when (state.error) {
                is UpdateKycAdditionalInfoError.InvalidNode -> R.string.kyc_additional_info_invalid_node_error
                UpdateKycAdditionalInfoError.RequestFailed -> R.string.server_unreachable_exit
            }
            BlockchainSnackbar.make(
                requireView(), getString(stringRes), type = SnackbarType.Error
            ).show()
            model.onIntent(KycAdditionalInfoIntent.ErrorHandled)
        }
    }
}
