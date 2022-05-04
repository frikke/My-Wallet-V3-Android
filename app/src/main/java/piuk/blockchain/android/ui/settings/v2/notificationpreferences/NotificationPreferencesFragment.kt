package piuk.blockchain.android.ui.settings.v2.notificationpreferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.blockchain.analytics.Analytics
import com.blockchain.api.services.ContactPreference
import com.blockchain.commonarch.presentation.base.updateToolbar
import com.blockchain.commonarch.presentation.mvi_v2.MVIFragment
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.koin.payloadScope
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ViewModelOwner
import org.koin.androidx.viewmodel.scope.getViewModel
import piuk.blockchain.android.ui.settings.v2.SettingsNavigator

class NotificationPreferencesFragment : MVIFragment<NotificationPreferencesViewState>() {

    private val analytics: Analytics by inject()

    private val model: NotificationPreferencesViewModel by lazy {
        payloadScope.getViewModel(owner = { ViewModelOwner.from(this) })
    }

    private val navigator: NavigationRouter<NotificationPreferencesNavigation> =
        object : NavigationRouter<NotificationPreferencesNavigation> {
            override fun route(navigationEvent: NotificationPreferencesNavigation) {
                when (navigationEvent) {
                    NotificationPreferencesNavigation.Back -> requireActivity().onBackPressed()
                    is NotificationPreferencesNavigation.Details ->
                        startDetailsFragment(navigationEvent.contactPreference)
                }
            }
        }

    private fun startDetailsFragment(preference: ContactPreference) {
        (requireActivity() as SettingsNavigator).goToNotificationPreferencesDetails(preference)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        bindViewModel(model, navigator, ModelConfigArgs.NoArgs)
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by model.viewState.collectAsState()
                NotificationPreferenceScreen(
                    state = state,
                    onItemClicked = { model.onIntent(NotificationPreferencesIntent.NavigateToDetails(it)) },
                    onRetryClicked = { model.onIntent(NotificationPreferencesIntent.Retry) },
                    onBackClicked = { model.onIntent(NotificationPreferencesIntent.NavigateBack) }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        analytics.logEvent(NotificationPreferencesAnalyticsEvents.NotificationViewed)

        updateToolbar(
            menuItems = emptyList()
        )
    }

    override fun onStateUpdated(state: NotificationPreferencesViewState) { }

    companion object {
        fun newInstance() = NotificationPreferencesFragment()
    }
}
