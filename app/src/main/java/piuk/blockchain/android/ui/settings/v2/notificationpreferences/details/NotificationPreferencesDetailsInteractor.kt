package piuk.blockchain.android.ui.settings.v2.notificationpreferences.details

import com.blockchain.api.services.ContactPreferenceAction
import com.blockchain.api.services.ContactPreferenceUpdate
import com.blockchain.core.user.NabuUserDataManager
import piuk.blockchain.androidcore.utils.extensions.awaitOutcome

class NotificationPreferencesDetailsInteractor(private val userDataManager: NabuUserDataManager) {

    suspend fun updateContactPreferences(channel: String, methods: List<ContactMethod>) =
        userDataManager.updateContactPreferences(
            methods.map {
                ContactPreferenceUpdate(
                    channel = channel,
                    method = it.method,
                    action = if (it.isMethodEnabled) ContactPreferenceAction.ENABLE else ContactPreferenceAction.DISABLE
                )
            }
        ).awaitOutcome()
}
