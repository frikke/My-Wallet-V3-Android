package piuk.blockchain.android.ui.dataremediation

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
import com.blockchain.domain.dataremediation.model.QuestionnaireContext
import com.blockchain.domain.dataremediation.model.SubmitQuestionnaireError
import com.blockchain.extensions.exhaustive
import com.blockchain.koin.payloadScope
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope
import piuk.blockchain.android.KycNavXmlDirections
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener
import piuk.blockchain.android.ui.kyc.navigate

class QuestionnaireFragment() : MVIFragment<QuestionnaireState>(), AndroidScopeComponent {

    private val kycHost: KycProgressListener? by lazy {
        requireActivity() as? KycProgressListener
    }

    override val scope: Scope = payloadScope

    private val model: QuestionnaireModel by viewModel()

    private val navigator: NavigationRouter<Navigation> = object : NavigationRouter<Navigation> {
        override fun route(navigationEvent: Navigation) {
            when (navigationEvent) {
                Navigation.LaunchVeriff -> navigate(KycNavXmlDirections.actionStartVeriff(countryCode))
            }.exhaustive
        }
    }

    private val root: TreeNode.Root by lazy {
        QuestionnaireFragmentArgs.fromBundle(arguments ?: Bundle()).root
    }

    private val countryCode: String by lazy {
        QuestionnaireFragmentArgs.fromBundle(arguments ?: Bundle()).countryCode
    }

    private val questionnaireContext: QuestionnaireContext by lazy {
        QuestionnaireContext.TIER_TWO_VERIFICATION
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        bindViewModel(model, navigator, Args(root))

        kycHost?.setHostTitle(R.string.kyc_additional_info_toolbar)

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by model.viewState.collectAsState()

                QuestionnaireScreen(
                    state = state,
                    onDropdownChoiceChanged = { node, newChoice ->
                        model.onIntent(QuestionnaireIntent.DropdownChoiceChanged(node, newChoice))
                    },
                    onSelectionClicked = { node ->
                        model.onIntent(QuestionnaireIntent.SelectionClicked(node))
                    },
                    onOpenEndedInputChanged = { node, newInput ->
                        model.onIntent(QuestionnaireIntent.OpenEndedInputChanged(node, newInput))
                    },
                    onContinueClicked = {
                        model.onIntent(QuestionnaireIntent.ContinueClicked)
                    },
                )
            }
        }
    }

    override fun onStateUpdated(state: QuestionnaireState) {
        if (state.error != null) {
            val stringRes = when (state.error) {
                is SubmitQuestionnaireError.InvalidNode -> R.string.kyc_additional_info_invalid_node_error
                SubmitQuestionnaireError.RequestFailed -> R.string.server_unreachable_exit
            }
            BlockchainSnackbar.make(
                requireView(), getString(stringRes), type = SnackbarType.Error
            ).show()
            model.onIntent(QuestionnaireIntent.ErrorHandled)
        }
    }
}
