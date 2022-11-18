package piuk.blockchain.android.ui.settings.notificationpreferences.details

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.blockchain.analytics.Analytics
import com.blockchain.api.services.ContactMethod
import com.blockchain.api.services.ContactPreference
import com.blockchain.commonarch.presentation.base.updateToolbar
import com.blockchain.commonarch.presentation.mvi_v2.MVIFragment
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.koin.payloadScope
import kotlinx.parcelize.Parcelize
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope
import piuk.blockchain.android.ui.settings.notificationpreferences.NotificationPreferencesAnalyticsEvents

class NotificationPreferenceDetailsFragment :
    MVIFragment<NotificationPreferenceDetailsViewState>(),
    AndroidScopeComponent {

    private val analytics: Analytics by inject()

    private val args: NotificationPreferenceDetailsArguments by lazy {
        (arguments?.getParcelable(NOTIFICATION_PREFERENCE_DETAILS)!!)
    }

    override val scope: Scope = payloadScope

    private val model: NotificationPreferencesDetailsViewModel by viewModel()

    private val navigator: NavigationRouter<NotificationPreferenceDetailsNavigation> =
        object : NavigationRouter<NotificationPreferenceDetailsNavigation> {
            override fun route(navigationEvent: NotificationPreferenceDetailsNavigation) {}
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        bindViewModel(model, navigator, args)
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by model.viewState.collectAsState()
                NotificationPreferenceDetailsScreen(state = state) { methods, changed ->
                    model.onIntent(NotificationPreferenceDetailsIntent.ContactMethodChanged(methods, changed))
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        analytics.logEvent(NotificationPreferencesAnalyticsEvents.NotificationOptionViewed(args.channel))
        updateToolbar(
            toolbarTitle = args.title
        )
    }

    override fun onStop() {
        analytics.logEvent(NotificationPreferencesAnalyticsEvents.NotificationsClosed)
        super.onStop()
    }

    override fun onStateUpdated(state: NotificationPreferenceDetailsViewState) {}

    companion object {
        private const val NOTIFICATION_PREFERENCE_DETAILS = "NOTIFICATION_DETAILS"

        fun newInstance(preference: ContactPreference) =
            NotificationPreferenceDetailsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(
                        NOTIFICATION_PREFERENCE_DETAILS,
                        preference.toNotificationPreferenceDetailsArguments()
                    )
                }
            }
    }
}

@Parcelize
data class NotificationPreferenceDetailsArguments(
    val title: String,
    val subtitle: String,
    val description: String,
    val channel: String,
    val methods: List<NotificationContactMethod>
) : ModelConfigArgs.ParcelableArgs

fun ContactPreference.toNotificationPreferenceDetailsArguments() =
    NotificationPreferenceDetailsArguments(
        title = title,
        subtitle = subtitle,
        description = description,
        channel = channel,
        methods = methods.map { it.toNotificationContact() }
    )

@Parcelize
data class NotificationContactMethod(
    val method: String,
    val title: String,
    var enabled: Boolean,
    val required: Boolean,
    val configured: Boolean,
    val verified: Boolean
) : Parcelable

fun ContactMethod.toNotificationContact() = NotificationContactMethod(
    method = method,
    title = title,
    enabled = enabled,
    required = required,
    configured = configured,
    verified = verified
)
