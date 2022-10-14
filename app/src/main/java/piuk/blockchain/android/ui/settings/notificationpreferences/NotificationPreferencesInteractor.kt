package piuk.blockchain.android.ui.settings.notificationpreferences

import com.blockchain.core.user.NabuUserDataManager
import com.blockchain.utils.awaitOutcome

class NotificationPreferencesInteractor(private val userDataManager: NabuUserDataManager) {

    suspend fun getNotificationPreferences() = userDataManager.getContactPreferences()
        .awaitOutcome()
}
