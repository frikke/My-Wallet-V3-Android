package piuk.blockchain.android.auth

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.blockchain.auth.LogoutTimer
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import piuk.blockchain.android.ui.auth.LogoutActivity

class AppLockTimer(private val application: Application) : LogoutTimer {
    private lateinit var logoutPendingIntent: PendingIntent

    override fun start() {
        val intent = Intent(application, LogoutActivity::class.java)
            .apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                action = BlockchainActivity.LOGOUT_ACTION
            }
        logoutPendingIntent = PendingIntent.getActivity(
            application,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        (application.getSystemService(Context.ALARM_SERVICE) as AlarmManager).set(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + LOGOUT_TIMEOUT_MILLIS,
            logoutPendingIntent
        )
    }

    override fun stop() {
        if (::logoutPendingIntent.isInitialized) {
            (application.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(logoutPendingIntent)
        }
    }

    companion object {
        private const val LOGOUT_TIMEOUT_MILLIS = 1000L * 60L * 5L // 5 minutes
    }
}
