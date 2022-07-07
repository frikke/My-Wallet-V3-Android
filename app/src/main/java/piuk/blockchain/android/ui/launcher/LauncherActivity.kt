package piuk.blockchain.android.ui.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.blockchain.analytics.NotificationAppOpened
import com.blockchain.koin.scopedInject
import com.blockchain.logging.MomentEvent
import com.blockchain.logging.MomentLogger
import com.blockchain.notifications.analytics.NotificationAnalyticsEvents
import com.blockchain.notifications.analytics.NotificationAnalyticsEvents.Companion.createCampaignPayload
import com.blockchain.notifications.models.NotificationDataConstants.DATA
import com.blockchain.notifications.models.NotificationDataConstants.DATA_REFERRAL_SUCCESS_BODY
import com.blockchain.notifications.models.NotificationDataConstants.DATA_REFERRAL_SUCCESS_TITLE
import com.blockchain.notifications.models.NotificationDataConstants.DATA_URL
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import piuk.blockchain.android.R
import piuk.blockchain.android.maintenance.presentation.AppMaintenanceFragment
import piuk.blockchain.android.maintenance.presentation.AppMaintenanceSharedViewModel
import piuk.blockchain.android.ui.base.MvpActivity
import piuk.blockchain.android.ui.settings.v2.security.pin.PinActivity
import piuk.blockchain.android.ui.start.LandingActivity
import piuk.blockchain.android.ui.start.PasswordRequiredActivity
import piuk.blockchain.android.util.wiper.DataWiper
import timber.log.Timber

class LauncherActivity : MvpActivity<LauncherView, LauncherPresenter>(), LauncherView {

    private val appMaintenanceViewModel: AppMaintenanceSharedViewModel by viewModel()
    private var appMaintenanceJob: Job? = null

    private val dataWiper: DataWiper by scopedInject()

    private val momentLogger: MomentLogger by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        momentLogger.endEvent(MomentEvent.LAUNCHER_TO_SPLASH)
        momentLogger.startEvent(MomentEvent.SPLASH_TO_FIRST_SCREEN)

        dataWiper.clearData()

        if (intent.hasExtra(INTENT_FROM_NOTIFICATION) &&
            intent.getBooleanExtra(INTENT_FROM_NOTIFICATION, false)
        ) {
            analytics.logEvent(NotificationAppOpened)
            val payload = createCampaignPayload(intent.extras)
            analytics.logEvent(NotificationAnalyticsEvents.PushNotificationTapped(payload))
        }

        if (intent.hasExtra(INTENT_DIRECT_FROM_FCM)) {
            intent.extras?.let {
                val analyticsPayload = createCampaignPayload(it)
                analytics.logEvent(NotificationAnalyticsEvents.PushNotificationTapped(analyticsPayload))
            }
        }
    }

    override fun getViewIntentData(): ViewIntentData {
        var deeplinkURL: String? = null
        var referralSuccessTitle: String? = null
        var referralSuccessBody: String? = null
        when {
            intent.data != null -> deeplinkURL = intent.data.toString()
            intent.hasExtra(DATA) -> {
                try {
                    val jsonObject = JSONObject(intent.getStringExtra(DATA))
                    if (jsonObject.has(DATA_URL)) {
                        deeplinkURL = jsonObject.getString(DATA_URL)
                    }
                    if (jsonObject.has(DATA_REFERRAL_SUCCESS_TITLE)) {
                        referralSuccessTitle = jsonObject.getString(DATA_REFERRAL_SUCCESS_TITLE)
                    }
                    if (jsonObject.has(DATA_REFERRAL_SUCCESS_BODY)) {
                        referralSuccessBody = jsonObject.getString(DATA_REFERRAL_SUCCESS_BODY)
                    }
                } catch (e: JSONException) {
                    Timber.e(e)
                }
            }
        }

        return ViewIntentData(
            action = intent.action,
            scheme = intent.scheme,
            dataString = intent.dataString,
            data = deeplinkURL,
            isAutomationTesting = intent.extras?.getBoolean(INTENT_AUTOMATION_TEST, false) ?: false,
            referralSuccessTitle,
            referralSuccessBody
        )
    }

    /**
     * Show maintenance screen and observe resume flow
     */
    override fun onAppMaintenance() {
        showBottomSheet(AppMaintenanceFragment.newInstance())
        observeResumeAppFlow()
    }

    /**
     * Because the maintenance screen is shown and the app/servers might be broken,
     * the flow will stop until notified by [AppMaintenanceSharedViewModel.resumeAppFlow]
     */
    private fun observeResumeAppFlow() {
        appMaintenanceJob?.cancel()
        appMaintenanceJob = lifecycleScope.launch {
            appMaintenanceViewModel.resumeAppFlow.collect {

                presenter.resumeAppFlow()

                appMaintenanceJob?.cancel()
                appMaintenanceJob = null
            }
        }
    }

    override fun onNoGuid() {
        Handler(Looper.getMainLooper()).postDelayed({
            LandingActivity.start(this)
        }, 500)
    }

    override fun onRequestPin() {
        startActivity(
            PinActivity.newIntent(
                context = this,
                startForResult = false,
                originScreen = PinActivity.Companion.OriginScreenToPin.LAUNCHER_SCREEN,
                addFlagsToClear = true
            )
        )
    }

    override fun onReenterPassword() {
        startSingleActivity(PasswordRequiredActivity::class.java, null)
    }

    override fun onCorruptPayload() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(getString(R.string.not_sane_error))
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                presenter.clearCredentialsAndRestart()
            }
            .show()
    }

    private fun startSingleActivity(clazz: Class<*>, extras: Bundle?, uri: Uri? = null) {
        val intent = Intent(this, clazz).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            data = uri
        }
        Timber.d("DeepLink: Starting Activity $clazz with: $uri")
        extras?.let { intent.putExtras(extras) }
        startActivity(intent)
    }

    override val presenter: LauncherPresenter by inject()
    override val view: LauncherView
        get() = this

    companion object {
        const val INTENT_AUTOMATION_TEST = "IS_AUTOMATION_TESTING"
        private const val INTENT_FROM_NOTIFICATION = "INTENT_FROM_NOTIFICATION"

        private const val INTENT_DIRECT_FROM_FCM = "google.message_id"

        fun newInstance(
            context: Context,
            intentFromNotification: Boolean = false,
            notificationAnalyticsPayload: Map<String, String>? = null
        ): Intent =
            Intent(context, LauncherActivity::class.java).apply {
                putExtra(INTENT_FROM_NOTIFICATION, intentFromNotification)
                notificationAnalyticsPayload?.keys?.forEach { key ->
                    notificationAnalyticsPayload[key]?.let { value ->
                        putExtra(key, value)
                    }
                }
            }
    }
}
