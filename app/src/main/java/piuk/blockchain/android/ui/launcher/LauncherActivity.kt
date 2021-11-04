package piuk.blockchain.android.ui.launcher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import com.blockchain.featureflags.GatedFeature
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.notifications.NotificationsUtil
import com.blockchain.notifications.analytics.NotificationAppOpened
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.auth.PinEntryActivity
import piuk.blockchain.android.ui.base.MvpActivity
import piuk.blockchain.android.ui.start.LandingActivity
import piuk.blockchain.android.ui.start.PasswordRequiredActivity
import timber.log.Timber

class LauncherActivity : MvpActivity<LauncherView, LauncherPresenter>(), LauncherView {

    private val internalFlags: InternalFeatureFlagApi by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        if (internalFlags.isFeatureEnabled(GatedFeature.NEW_ONBOARDING)) {
            setTheme(R.style.AppTheme_Splash)
        }
        super.onCreate(savedInstanceState)
        if (intent.hasExtra(NotificationsUtil.INTENT_FROM_NOTIFICATION) &&
            intent.getBooleanExtra(NotificationsUtil.INTENT_FROM_NOTIFICATION, false)
        ) {
            analytics.logEvent(NotificationAppOpened)
        }
    }

    override fun getViewIntentData(): ViewIntentData =
        ViewIntentData(
            action = intent.action,
            scheme = intent.scheme,
            dataString = intent.dataString,
            data = intent.data?.toString(),
            isAutomationTesting = intent.extras?.getBoolean(INTENT_AUTOMATION_TEST, false) ?: false
        )

    override fun onNoGuid() {
        Handler(Looper.getMainLooper()).postDelayed({
            LandingActivity.start(this)
        }, 500)
    }

    override fun onRequestPin() {
        startSingleActivity(PinEntryActivity::class.java, null)
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
    }
}
