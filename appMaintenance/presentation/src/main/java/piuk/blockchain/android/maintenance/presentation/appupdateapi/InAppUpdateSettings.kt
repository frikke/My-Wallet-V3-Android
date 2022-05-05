package piuk.blockchain.android.maintenance.presentation.appupdateapi

import android.app.Activity

interface InAppUpdateSettings {
    suspend fun triggerOrResumeAppUpdate(activity: Activity)
}
