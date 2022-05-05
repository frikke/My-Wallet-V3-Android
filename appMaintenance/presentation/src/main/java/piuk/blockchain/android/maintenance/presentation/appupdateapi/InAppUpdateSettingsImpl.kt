package piuk.blockchain.android.maintenance.presentation.appupdateapi

import android.app.Activity
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import piuk.blockchain.android.maintenance.domain.appupdateapi.AppUpdateInfoFactory

internal class InAppUpdateSettingsImpl(
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
                REQUEST_CODE
            )
        }
    }

    companion object {
        const val REQUEST_CODE = 29138
    }
}
