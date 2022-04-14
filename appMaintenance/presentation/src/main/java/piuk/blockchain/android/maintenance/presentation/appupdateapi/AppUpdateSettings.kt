package piuk.blockchain.android.maintenance.presentation.appupdateapi

import android.app.Activity

interface InAppUpdateSettings {
    companion object {
        const val REQUEST_CODE = 29138
    }

    suspend fun triggerOrResumeAppUpdate(activity: Activity)
}