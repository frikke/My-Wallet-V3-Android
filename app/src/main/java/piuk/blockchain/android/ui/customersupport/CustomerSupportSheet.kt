package piuk.blockchain.android.ui.customersupport

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.blockchain.analytics.Analytics
import com.blockchain.commonarch.presentation.mvi_v2.MVIBottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.java.KoinJavaComponent.get
import piuk.blockchain.android.ui.customersupport.composable.CustomerSupportScreen
import piuk.blockchain.android.urllinks.URL_CONTACT_SUBMIT_REQUEST
import piuk.blockchain.android.urllinks.URL_FAQ
import piuk.blockchain.android.util.copyToClipboard
import piuk.blockchain.android.util.openUrl

class CustomerSupportSheet :
    MVIBottomSheet<CustomerSupportViewState>(),
    NavigationRouter<CustomerSupportNavigationEvent>,
    Analytics by get(Analytics::class.java) {

    private val viewModel: CustomerSupportViewModel by viewModel()

    override fun onStateUpdated(state: CustomerSupportViewState) {
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setupViewModel()

        return ComposeView(requireContext()).apply {
            setContent {
                CustomerSupportScreen(
                    onDismiss = ::dismiss,
                    contactUsClicked = ::contactUsClicked,
                    faqClicked = ::faqClicked,
                    copyCommitHash = ::copyCommitHash
                )
            }
        }
    }

    private fun setupViewModel() {
        bindViewModel(viewModel, this, ModelConfigArgs.NoArgs)
    }

    private fun contactUsClicked() {
        logEvent(CustomerSupportAnalytics.ContactUsClicked)
        viewUrl(URL_CONTACT_SUBMIT_REQUEST)
    }

    private fun faqClicked() {
        logEvent(CustomerSupportAnalytics.FaqClicked)
        viewUrl(URL_FAQ)
    }

    private fun copyCommitHash(hash: String) {
        context?.copyToClipboard(label = "commit hash", text = hash)
    }

    private fun viewUrl(url: String) {
        context.openUrl(url)
    }

    override fun route(navigationEvent: CustomerSupportNavigationEvent) {
    }

    companion object {
        fun newInstance() = CustomerSupportSheet()
    }
}
