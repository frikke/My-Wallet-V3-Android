package piuk.blockchain.android.ui.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.blockchain.commonarch.presentation.base.addAnimationTransaction
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.NotificationAnalyticsEvents
import com.blockchain.notifications.analytics.NotificationAppOpened
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityInterestDashboardBinding
import piuk.blockchain.android.ui.base.MvpActivity
import piuk.blockchain.android.ui.customersupport.CustomerSupportSheet
import piuk.blockchain.android.ui.maintenance.presentation.AppMaintenanceFragment
import piuk.blockchain.android.ui.maintenance.presentation.AppMaintenanceSharedViewModel
import piuk.blockchain.android.ui.settings.v2.security.pin.PinActivity
import piuk.blockchain.android.ui.start.LandingActivity
import piuk.blockchain.android.ui.start.PasswordRequiredActivity
import piuk.blockchain.android.util.wiper.DataWiper
import timber.log.Timber

class LauncherActivity : MvpActivity<LauncherView, LauncherPresenter>(), LauncherView {

    private val appMaintenanceViewModel: AppMaintenanceSharedViewModel by viewModel()
    private var appMaintenanceJob: Job? = null

    private val binding: ActivityInterestDashboardBinding by lazy {
        ActivityInterestDashboardBinding.inflate(layoutInflater)
    }

    private val dataWiper: DataWiper by scopedInject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        dataWiper.clearData()

        if (intent.hasExtra(INTENT_FROM_NOTIFICATION) &&
            intent.getBooleanExtra(INTENT_FROM_NOTIFICATION, false)
        ) {
            analytics.logEvent(NotificationAppOpened)
            analytics.logEvent(NotificationAnalyticsEvents.PushNotificationTapped)
        }
    }

    override fun getViewIntentData(): ViewIntentData {
        val deeplinkURL =
            when {
                intent.data != null -> intent.data.toString()
                intent.hasExtra("data") -> {
                    try {
                        val jsonObject = JSONObject(intent.getStringExtra("data"))
                        if (jsonObject.has("url")) {
                            jsonObject.getString("url")
                        } else {
                            null
                        }
                    } catch (e: JSONException) {
                        Timber.e(e)
                        null
                    }
                }
                else -> null
            }

        return ViewIntentData(
            action = intent.action,
            scheme = intent.scheme,
            dataString = intent.dataString,
            data = deeplinkURL,
            isAutomationTesting = intent.extras?.getBoolean(INTENT_AUTOMATION_TEST, false) ?: false
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

        fun newInstance(context: Context, intentFromNotification: Boolean): Intent =
            Intent(context, LauncherActivity::class.java).apply {
                putExtra(INTENT_FROM_NOTIFICATION, intentFromNotification)
            }
    }
}
