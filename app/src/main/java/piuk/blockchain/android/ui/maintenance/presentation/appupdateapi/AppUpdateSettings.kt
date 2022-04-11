package piuk.blockchain.android.ui.maintenance.presentation.appupdateapi

import android.app.Activity

interface InAppUpdateSettings {
    companion object {
        /**
         * Random request code for catching intent results
         */
        const val REQUEST_CODE = 29138
    }

    suspend fun triggerOrResumeAppUpdate(activity: Activity)
}