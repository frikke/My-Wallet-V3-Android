package piuk.blockchain.android.ui.linkbank.presentation.openbanking.permission

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import com.blockchain.analytics.Analytics
import com.blockchain.commonarch.presentation.mvi_v2.MVIFragment
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.commonarch.presentation.mvi_v2.withArgs
import com.blockchain.domain.paymentmethods.model.BankAuthSource
import com.blockchain.domain.paymentmethods.model.YapilyInstitution
import com.blockchain.koin.payloadScope
import com.blockchain.presentation.openUrl
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope
import org.koin.java.KoinJavaComponent.get
import piuk.blockchain.android.ui.linkbank.BankAuthAnalytics
import piuk.blockchain.android.ui.linkbank.bankAuthEvent
import piuk.blockchain.android.ui.linkbank.presentation.openbanking.permission.OpenBankingPermissionArgs.Companion.ARGS_KEY
import piuk.blockchain.android.ui.linkbank.presentation.openbanking.permission.composables.OpenBankingPermissionScreen
import piuk.blockchain.android.ui.linkbank.toAnalyticsBankProvider

class OpenBankingPermissionFragment :
    MVIFragment<OpenBankingPermissionViewState>(),
    Analytics by get(Analytics::class.java),
    AndroidScopeComponent {

    private val args: OpenBankingPermissionArgs by lazy {
        arguments?.run { getParcelable(ARGS_KEY) as? OpenBankingPermissionArgs }
            ?: error("missing args")
    }

    private val navigationRouter: NavigationRouter<OpenBankingPermissionNavEvent> by lazy {
        activity as? NavigationRouter<OpenBankingPermissionNavEvent>
            ?: error("host does not implement NavigationRouter<OpenBankingPermissionNavEvent>")
    }

    override val scope: Scope = payloadScope

    private val viewModel: OpenBankingPermissionViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        bindViewModel(viewModel = viewModel, navigator = navigationRouter, args = args)
        viewModel.onIntent(OpenBankingPermissionIntents.GetTermsOfServiceLink)

        return ComposeView(requireContext()).apply {
            setContent {
                ScreenContent()
            }
        }
    }

    /**
     * Figma: https://www.figma.com/file/GqLrQTNiUnEUCCQvcjUUTo/And---Yapily(UK)?node-id=1604%3A15208
     */
    @Composable
    private fun ScreenContent() {
        val state = viewModel.viewState.collectAsState()

        OpenBankingPermissionScreen(
            institution = args.institution,
            termsOfServiceLink = state.value.termsOfServiceLink,
            urlOnclick = ::openUrl,
            approveOnClick = ::approveOnClick,
            denyOnClick = ::denyOnClick
        )
    }

    override fun onStateUpdated(state: OpenBankingPermissionViewState) {}

    private fun openUrl(url: String) {
        context.openUrl(url)
    }

    private fun approveOnClick() {
        logApproveAnalytics()
        logLinkBankConditionsApproved()

        viewModel.onIntent(OpenBankingPermissionIntents.ApproveClicked)
    }

    private fun denyOnClick() {
        logDenyAnalytics()

        viewModel.onIntent(OpenBankingPermissionIntents.DenyClicked)
    }

    private fun logApproveAnalytics() {
        logEvent(
            bankAuthEvent(
                BankAuthAnalytics.AIS_PERMISSIONS_APPROVED,
                args.authSource
            )
        )
    }

    private fun logLinkBankConditionsApproved() {
        logEvent(
            BankAuthAnalytics.LinkBankConditionsApproved(
                bankName = args.institution.name,
                provider = args.entity.toAnalyticsBankProvider(),
                partner = PARTNER
            )
        )
    }

    private fun logDenyAnalytics() {
        logEvent(
            bankAuthEvent(
                BankAuthAnalytics.AIS_PERMISSIONS_DENIED,
                args.authSource
            )
        )
    }

    companion object {
        private const val PARTNER = "YAPILY"

        fun newInstance(
            institution: YapilyInstitution,
            entity: String,
            authSource: BankAuthSource
        ): OpenBankingPermissionFragment =
            OpenBankingPermissionFragment().withArgs(
                key = ARGS_KEY,
                args = OpenBankingPermissionArgs(institution = institution, authSource = authSource, entity = entity)
            )
    }
}
