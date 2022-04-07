package piuk.blockchain.android.ui.maintenance.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.commonarch.presentation.mvi_v2.MVIFragment
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import org.koin.androidx.viewmodel.ext.android.viewModel

class AppMaintenanceFragment : MVIFragment<AppMaintenanceViewState>(), NavigationRouter<AppMaintenanceNavigationEvent> {

    private val viewModel: AppMaintenanceViewModel by viewModel()

    override fun onStateUpdated(state: AppMaintenanceViewState) {
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun route(navigationEvent: AppMaintenanceNavigationEvent) {
    }

    companion object {
        fun newInstance() = AppMaintenanceFragment()
    }
}

