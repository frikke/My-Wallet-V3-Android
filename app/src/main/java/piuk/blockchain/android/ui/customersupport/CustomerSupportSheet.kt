package piuk.blockchain.android.ui.customersupport

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.commonarch.presentation.mvi_v2.MVIBottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import piuk.blockchain.android.databinding.DialogSheetCustomerSupportBinding
import piuk.blockchain.android.urllinks.URL_CONTACT_SUBMIT_REQUEST
import piuk.blockchain.android.urllinks.URL_FAQ
import piuk.blockchain.android.util.openUrl

class CustomerSupportSheet : MVIBottomSheet<CustomerSupportViewState>(),
    NavigationRouter<CustomerSupportNavigationEvent> {

    private lateinit var binding: DialogSheetCustomerSupportBinding

    private val viewModel: CustomerSupportViewModel by viewModel()

    override fun onStateUpdated(state: CustomerSupportViewState) {
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogSheetCustomerSupportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        analytics.logEvent(CustomerSupportAnalytics.SheetShown)

        setupViews()
        setupViewModel()
    }

    private fun setupViews() {
        with(binding) {
            contactUsButton.setOnClickListener {
                analytics.logEvent(CustomerSupportAnalytics.EmailClicked)
                viewUrl(URL_CONTACT_SUBMIT_REQUEST)
            }
            faqButton.setOnClickListener {
                analytics.logEvent(CustomerSupportAnalytics.FaqClicked)
                viewUrl(URL_FAQ)
            }
        }
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
}

