package piuk.blockchain.android.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.blockchain.chrome.MultiAppActivity
import com.blockchain.deeplinking.navigation.Destination
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.home.presentation.navigation.HomeLaunch.INTENT_FROM_NOTIFICATION
import com.blockchain.home.presentation.navigation.HomeLaunch.LAUNCH_AUTH_FLOW
import com.blockchain.home.presentation.navigation.HomeLaunch.PENDING_DESTINATION
import piuk.blockchain.android.ui.auth.newlogin.presentation.AuthNewLoginSheet

class HomeActivityLauncher(private val featureFlag: FeatureFlag) {

    private var homeActivity: Class<*> = MainActivity::class.java

    suspend fun updateHomeActivity() {
        val isSuperAppEnabled = featureFlag.coEnabled()
        homeActivity = if (isSuperAppEnabled)
            MultiAppActivity::class.java
        else
            MainActivity::class.java
    }

    fun newIntent(
        context: Context,
        launchAuthFlow: Boolean,
        pubKeyHash: String,
        shouldBeNewTask: Boolean,
    ): Intent = Intent(context, homeActivity).apply {
        putExtra(LAUNCH_AUTH_FLOW, launchAuthFlow)
        putExtra(AuthNewLoginSheet.PUB_KEY_HASH, pubKeyHash)

        if (shouldBeNewTask) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun newIntent(
        context: Context,
        intentFromNotification: Boolean,
        notificationAnalyticsPayload: Map<String, String>? = null,
    ): Intent =
        Intent(context, homeActivity).apply {
            putExtra(INTENT_FROM_NOTIFICATION, intentFromNotification)
            notificationAnalyticsPayload?.keys?.forEach { key ->
                notificationAnalyticsPayload[key]?.let { value ->
                    putExtra(key, value)
                }
            }
        }

    fun newIntent(context: Context, pendingDestination: Destination): Intent =
        Intent(context, homeActivity).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(PENDING_DESTINATION, pendingDestination)
        }

    fun newIntentAsNewTask(context: Context): Intent =
        Intent(context, homeActivity).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun newIntent(
        context: Context,
        intentData: String?,
        shouldBeNewTask: Boolean,
    ): Intent = Intent(context, homeActivity).apply {
        if (intentData != null) {
            data = Uri.parse(intentData)
        }

        if (shouldBeNewTask) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
