package piuk.blockchain.android.ui.dataremediation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.blockchain.commonarch.presentation.mvi_v2.MVIBottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.commonarch.presentation.mvi_v2.disableDragging
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.koin.payloadScope
import com.blockchain.fiatActions.QuestionnaireSheetHost
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope
import org.koin.java.KoinJavaComponent.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.fraud.domain.service.FraudFlow
import piuk.blockchain.android.fraud.domain.service.FraudService

class QuestionnaireSheet() :
    MVIBottomSheet<QuestionnaireState>(),
    AndroidScopeComponent,
    QuestionnaireDropdownPickerSheet.Host {

    override val host: QuestionnaireSheetHost by lazy {
        (activity as? QuestionnaireSheetHost)
            ?: (parentFragment as? QuestionnaireSheetHost) ?: throw IllegalStateException(
            "Host is not a QuestionnaireFragment.Host"
        )
    }

    override val scope: Scope = payloadScope

    private val model: QuestionnaireModel by viewModel()
    private val fraudService: FraudService by inject()

    private val navigator: NavigationRouter<Navigation> = object : NavigationRouter<Navigation> {
        override fun route(navigationEvent: Navigation) {
            when (navigationEvent) {
                is Navigation.OpenDropdownPickerForSingleSelection -> {
                    if (!childFragmentManager.isDestroyed) {
                        QuestionnaireDropdownPickerSheet.newInstance(navigationEvent.original, navigationEvent.node)
                            .show(childFragmentManager, "BOTTOM_DIALOG")
                    }
                }
                is Navigation.OpenDropdownPickerForMultipleSelection -> {
                    if (!childFragmentManager.isDestroyed) {
                        QuestionnaireDropdownPickerSheet.newInstance(navigationEvent.original, navigationEvent.node)
                            .show(childFragmentManager, "BOTTOM_DIALOG")
                    }
                }
                Navigation.FinishSuccessfully -> {
                    host.questionnaireSubmittedSuccessfully()
                    if (showsDialog) dismiss()
                }
            }
        }
    }

    private val questionnaire: Questionnaire by lazy {
        arguments?.getSerializable(ARG_QUESTIONNAIRE) as Questionnaire
    }

    private val showNavigationBar: Boolean by lazy {
        arguments?.getBoolean(ARG_SHOW_NAVIGATION_BAR) ?: false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        bindViewModel(model, navigator, Args(questionnaire))
        if (showsDialog) disableDragging()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by model.viewState.collectAsState()

                QuestionnaireScreen(
                    showNavigationBar = showNavigationBar,
                    isSkipVisible = !questionnaire.isMandatory,
                    header = questionnaire.header,
                    state = state,
                    onDropdownOpenPickerClicked = { node ->
                        model.onIntent(QuestionnaireIntent.DropdownOpenPickerClicked(node))
                    },
                    onSelectionClicked = { node ->
                        model.onIntent(QuestionnaireIntent.SelectionClicked(node))
                    },
                    onOpenEndedInputChanged = { node, newInput ->
                        model.onIntent(QuestionnaireIntent.OpenEndedInputChanged(node, newInput))
                    },
                    onContinueClicked = {
                        fraudService.endFlow(FraudFlow.KYC)
                        model.onIntent(QuestionnaireIntent.ContinueClicked)
                    },
                    onSkipClicked = {
                        host.questionnaireSkipped()
                        if (showsDialog) dismiss()
                    },
                    onBackClicked = {
                        if (showsDialog) dismiss()
                        else requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                )
            }
        }
    }

    override fun onStateUpdated(state: QuestionnaireState) {
        if (state.error != null) {
            val errorMessage = when (state.error) {
                is QuestionnaireError.InvalidNode -> getString(R.string.kyc_additional_info_invalid_node_error)
                is QuestionnaireError.InvalidOpenEndedRegexMatch ->
                    getString(R.string.questionnaire_error_invalid_format)
                is QuestionnaireError.Unknown -> state.error.message ?: getString(R.string.server_unreachable_exit)
            }
            BlockchainSnackbar.make(
                requireView(), errorMessage, type = SnackbarType.Error
            ).show()
            model.onIntent(QuestionnaireIntent.ErrorHandled)
        }
    }

    override fun selectionChanged(node: FlatNode.Dropdown, newSelectedChoices: List<FlatNode.Selection>) {
        model.onIntent(QuestionnaireIntent.DropdownChoicesChanged(node, newSelectedChoices))
    }

    override fun onSheetClosed() {
    }

    companion object {
        private const val ARG_QUESTIONNAIRE = "ARG_QUESTIONNAIRE"
        private const val ARG_SHOW_NAVIGATION_BAR = "ARG_SHOW_NAVIGATION_BAR"

        fun newInstance(
            questionnaire: Questionnaire,
            showNavigationBar: Boolean = false
        ): QuestionnaireSheet = QuestionnaireSheet().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_QUESTIONNAIRE, questionnaire)
                putBoolean(ARG_SHOW_NAVIGATION_BAR, showNavigationBar)
            }
        }
    }
}
