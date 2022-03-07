package piuk.blockchain.android.ui.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import com.blockchain.koin.redesignPart2FeatureFlag
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.NotificationAppOpened
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.auth.PinEntryActivity
import piuk.blockchain.android.ui.base.MvpActivity
import piuk.blockchain.android.ui.settings.v2.security.pin.PinActivity
import piuk.blockchain.android.ui.start.LandingActivity
import piuk.blockchain.android.ui.start.PasswordRequiredActivity
import piuk.blockchain.android.util.wiper.DataWiper
import timber.log.Timber

class LauncherActivity : MvpActivity<LauncherView, LauncherPresenter>(), LauncherView {

    private val redesign: FeatureFlag by inject(redesignPart2FeatureFlag)
    private val dataWiper: DataWiper by scopedInject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dataWiper.clearData()

        if (intent.hasExtra(INTENT_FROM_NOTIFICATION) &&
            intent.getBooleanExtra(INTENT_FROM_NOTIFICATION, false)
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
        // TODO remove ff
        redesign.enabled.onErrorReturnItem(false).subscribeBy(
            onSuccess = { isEnabled ->
                if (isEnabled) {
                    startActivity(PinActivity.newIntent(this))
                } else {
                    startSingleActivity(PinEntryActivity::class.java, null)
                }
            }
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
