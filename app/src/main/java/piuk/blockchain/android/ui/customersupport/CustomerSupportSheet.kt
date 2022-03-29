package piuk.blockchain.android.ui.customersupport

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.commonarch.presentation.mvi_v2.MVIBottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.sheets.BottomSheet
import com.blockchain.componentlib.sheets.BottomSheetButton
import com.blockchain.componentlib.sheets.BottomSheetText
import com.blockchain.componentlib.sheets.ButtonType
import org.koin.androidx.viewmodel.ext.android.viewModel
import piuk.blockchain.android.R
import piuk.blockchain.android.urllinks.URL_CONTACT_SUBMIT_REQUEST
import piuk.blockchain.android.urllinks.URL_FAQ
import piuk.blockchain.android.util.openUrl

class CustomerSupportSheet :
    MVIBottomSheet<CustomerSupportViewState>(),
    NavigationRouter<CustomerSupportNavigationEvent> {

    private lateinit var composeView: ComposeView

    private val viewModel: CustomerSupportViewModel by viewModel()

    override fun onStateUpdated(state: CustomerSupportViewState) {
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).also { composeView = it }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        analytics.logEvent(CustomerSupportAnalytics.SheetShown)

        setupViews()
        setupViewModel()
    }

    private fun setupViews() {
        composeView.apply {
            setContent {
                SheetContent()
            }
        }
    }

    @Composable
    private fun SheetContent() {
        BottomSheet(
            title = BottomSheetText(
                text = stringResource(id = R.string.customer_support_title),
                textAlign = TextAlign.Start
            ),
            subtitle = BottomSheetText(
                text = stringResource(id = R.string.customer_support_description),
                textAlign = TextAlign.Start
            ),
            alignment = Alignment.Start,
            imageResource = ImageResource.None,
            topButton = BottomSheetButton(
                text = stringResource(id = R.string.customer_support_contact_us),
                onClick = {
                    analytics.logEvent(CustomerSupportAnalytics.EmailClicked)
                    viewUrl(URL_CONTACT_SUBMIT_REQUEST)
                },
                type = ButtonType.MINIMAL
            ),
            bottomButton = BottomSheetButton(
                text = stringResource(id = R.string.customer_support_faq),
                onClick = {
                    analytics.logEvent(CustomerSupportAnalytics.FaqClicked)
                    viewUrl(URL_FAQ)
                },
                type = ButtonType.MINIMAL
            )
        )
    }

    private fun setupViewModel() {
        bindViewModel(viewModel, this, ModelConfigArgs.NoArgs)
    }

    private fun viewUrl(url: String) {
        context.openUrl(url)
    }

    override fun route(navigationEvent: CustomerSupportNavigationEvent) {
    }

    companion object {
        fun newInstance() = CustomerSupportSheet()
    }

    @Preview
    @Composable
    private fun SheetContentPreview() {
        SheetContent()
    }
}

