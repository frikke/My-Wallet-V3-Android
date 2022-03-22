package piuk.blockchain.android.ui.pinhelp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.commonarch.presentation.mvi_v2.MVIBottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.notifications.analytics.Analytics
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import piuk.blockchain.android.databinding.DialogSheetPinHelpBinding
import piuk.blockchain.android.urllinks.URL_CONTACT_SUBMIT_REQUEST
import piuk.blockchain.android.urllinks.URL_FAQ
import piuk.blockchain.android.util.openUrl

class PinHelpSheet : MVIBottomSheet<PinHelpViewState>(), NavigationRouter<PinHelpNavigationEvent> {

    private lateinit var binding: DialogSheetPinHelpBinding

    private val viewModel: PinHelpViewModel by viewModel()

    val analytics: Analytics by inject()

    override fun onStateUpdated(state: PinHelpViewState) {
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogSheetPinHelpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        analytics.logEvent(PinHelpAnalytics.SheetShown)

        setupViews()
        setupViewModel()
    }

    private fun setupViewModel() {
        bindViewModel(viewModel, this, ModelConfigArgs.NoArgs)
    }

    private fun setupViews() {
        with(binding) {
            btnEmail.setOnClickListener {
//                analytics.logEvent(PinHelpAnalytics.EmailClicked)
                viewUrl(URL_CONTACT_SUBMIT_REQUEST)
            }
            btnFaq.setOnClickListener {
//                analytics.logEvent(PinHelpAnalytics.FaqClicked)
                viewUrl(URL_FAQ)
            }
        }
    }

    // todo move to core
    private fun viewUrl(url: String) {
        context.openUrl(url)
    }

    override fun route(navigationEvent: PinHelpNavigationEvent) {
    }

    companion object {
        fun newInstance() = PinHelpSheet()
    }
}

