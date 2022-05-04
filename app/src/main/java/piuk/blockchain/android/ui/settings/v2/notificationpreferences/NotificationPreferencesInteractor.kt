package piuk.blockchain.android.ui.settings.v2.notificationpreferences

import com.blockchain.core.user.NabuUserDataManager
import piuk.blockchain.androidcore.utils.extensions.awaitOutcome

class NotificationPreferencesInteractor(private val userDataManager: NabuUserDataManager) {

    suspend fun getNotificationPreferences() = userDataManager.getContactPreferences()
        .awaitOutcome()
}
