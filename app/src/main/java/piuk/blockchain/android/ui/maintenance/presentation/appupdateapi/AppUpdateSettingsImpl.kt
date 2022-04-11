package piuk.blockchain.android.ui.maintenance.presentation.appupdateapi

import android.app.Activity
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import piuk.blockchain.android.ui.maintenance.data.appupdateapi.AppUpdateInfoFactory

class AppUpdateSettingsImpl(
    private val appUpdateManager: AppUpdateManager,
    private val appUpdateInfoFactory: AppUpdateInfoFactory
) : InAppUpdateSettings {

    override suspend fun triggerOrResumeAppUpdate(activity: Activity) {
        // if the user cancels the download from playstore ui and tries to download again
        // the pendingintent created by appUpdateManager needs a new instance of appUpdateInfo
        with(appUpdateInfoFactory.getAppUpdateInfo()) {
            appUpdateManager.startUpdateFlowForResult(
                this,
                AppUpdateType.IMMEDIATE,
                activity,
                InAppUpdateSettings.REQUEST_CODE
            )
        }
    }
}