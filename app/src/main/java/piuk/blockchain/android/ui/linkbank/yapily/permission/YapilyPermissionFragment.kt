package piuk.blockchain.android.ui.linkbank.yapily.permission

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.blockchain.analytics.Analytics
import com.blockchain.commonarch.presentation.mvi_v2.MVIFragment
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.core.payments.model.YapilyInstitution
import java.io.File
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.java.KoinJavaComponent.get
import piuk.blockchain.android.ui.linkbank.BankAuthAnalytics
import piuk.blockchain.android.ui.linkbank.BankAuthFlowNavigator
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.linkbank.bankAuthEvent
import piuk.blockchain.android.ui.linkbank.toAnalyticsBankProvider
import piuk.blockchain.android.ui.linkbank.yapily.permission.YapilyPermissionArgs.Companion.ARGS_KEY
import piuk.blockchain.android.ui.linkbank.yapily.permission.composables.YapilyPermissionScreen
import piuk.blockchain.android.urllinks.URL_YAPILY_PRIVACY_POLICY
import piuk.blockchain.android.util.getFilePath
import piuk.blockchain.android.util.openPdfFile
import piuk.blockchain.android.util.openUrl

class YapilyPermissionFragment :
    MVIFragment<YapilyPermissionViewState>(),
    Analytics by get(Analytics::class.java),
    NavigationRouter<YapilyPermissionNavigationEvent> {

    private val args: YapilyPermissionArgs by lazy {
        arguments?.run { getParcelable(ARGS_KEY) as? YapilyPermissionArgs }
            ?: error("missing args")
    }

    private val navigator: BankAuthFlowNavigator by lazy {
        (activity as? BankAuthFlowNavigator)
            ?: error("Parent must implement BankAuthFlowNavigator")
    }

    private val viewModel: YapilyPermissionViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ScreenContent()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViewModel(viewModel = viewModel, navigator = this, args = args)
    }

    /**
     * Figma: https://www.figma.com/file/GqLrQTNiUnEUCCQvcjUUTo/And---Yapily(UK)?node-id=1604%3A15208
     */
    @Composable
    private fun ScreenContent() {
        YapilyPermissionScreen(
            institution = args.institution,
            termsOfServiceOnclick = ::termsOfServiceOnClick,
            privacyPolicyOnClick = ::privacyPolicyOnClick,
            approveOnClick = ::approveOnClick,
            denyOnClick = ::denyOnClick
        )
    }

    override fun onStateUpdated(state: YapilyPermissionViewState) {
    }

    override fun route(navigationEvent: YapilyPermissionNavigationEvent) {
        when (navigationEvent) {
            is YapilyPermissionNavigationEvent.OpenFile -> {
                openFile(navigationEvent.file)
            }
        }
    }

    private fun openFile(file: File) {
        context?.openPdfFile(file)
    }

    private fun termsOfServiceOnClick() {
        context?.let { context ->
            viewModel.onIntent(
                YapilyPermissionIntents.DownloadTermsOfService(
                    context.getFilePath(
                        fileName = SAFECONNECT_TERMS_FILE_NAME,
                        extension = SAFECONNECT_TERMS_FILE_EXTENSION
                    )
                )
            )
        }
    }

    private fun privacyPolicyOnClick() {
        context.openUrl(URL_YAPILY_PRIVACY_POLICY)
    }

    private fun approveOnClick() {
        logApproveAnalytics()
        logLinkBankConditionsApproved()
        navigator.yapilyAgreementAccepted(args.institution)
    }

    private fun denyOnClick() {
        logDenyAnalytics()
        navigator.yapilyAgreementCancelled()
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
            YapilyPermissionFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(
                        ARGS_KEY,
                        YapilyPermissionArgs(institution = institution, authSource = authSource, entity = entity)
                    )
                }
            }
    }
}
