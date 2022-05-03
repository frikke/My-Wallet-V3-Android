package piuk.blockchain.android.ui.linkbank.presentation.yapily.permission

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
import com.blockchain.core.payments.model.YapilyInstitution
import com.blockchain.koin.payloadScope
import org.koin.androidx.viewmodel.ViewModelOwner
import org.koin.androidx.viewmodel.scope.getViewModel
import org.koin.java.KoinJavaComponent.get
import piuk.blockchain.android.ui.linkbank.BankAuthAnalytics
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.linkbank.bankAuthEvent
import piuk.blockchain.android.ui.linkbank.presentation.yapily.permission.YapilyPermissionArgs.Companion.ARGS_KEY
import piuk.blockchain.android.ui.linkbank.presentation.yapily.permission.composables.YapilyPermissionScreen
import piuk.blockchain.android.ui.linkbank.toAnalyticsBankProvider
import piuk.blockchain.android.util.openUrl

class YapilyPermissionFragment :
    MVIFragment<YapilyPermissionViewState>(),
    Analytics by get(Analytics::class.java) {

    private val args: YapilyPermissionArgs by lazy {
        arguments?.run { getParcelable(ARGS_KEY) as? YapilyPermissionArgs }
            ?: error("missing args")
    }

    private val navigationRouter: NavigationRouter<YapilyPermissionNavigationEvent> by lazy {
        activity as? NavigationRouter<YapilyPermissionNavigationEvent>
            ?: error("host does not implement NavigationRouter<YapilyPermissionNavigationEvent>")
    }

    private val viewModel: YapilyPermissionViewModel by lazy {
        payloadScope.getViewModel(owner = { ViewModelOwner.from(this) })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        bindViewModel(viewModel = viewModel, navigator = navigationRouter, args = args)
        viewModel.onIntent(YapilyPermissionIntents.GetTermsOfServiceLink)

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

        YapilyPermissionScreen(
            institution = args.institution,
            termsOfServiceLink = state.value.termsOfServiceLink,
            urlOnclick = ::openUrl,
            approveOnClick = ::approveOnClick,
            denyOnClick = ::denyOnClick
        )
    }

    override fun onStateUpdated(state: YapilyPermissionViewState) {}

    private fun openUrl(url: String) {
        context.openUrl(url)
    }

    private fun approveOnClick() {
        logApproveAnalytics()
        logLinkBankConditionsApproved()

        viewModel.onIntent(YapilyPermissionIntents.ApproveClicked)
    }

    private fun denyOnClick() {
        logDenyAnalytics()

        viewModel.onIntent(YapilyPermissionIntents.DenyClicked)
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
        ): YapilyPermissionFragment =
            YapilyPermissionFragment().withArgs(
                key = ARGS_KEY,
                args = YapilyPermissionArgs(institution = institution, authSource = authSource, entity = entity)
            )
    }
}
