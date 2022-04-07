package piuk.blockchain.android.ui.maintenance.data.appupdateapi

import com.google.android.play.core.appupdate.AppUpdateInfo

interface AppUpdateInfoFactory {
    suspend fun getAppUpdateInfo(): AppUpdateInfo
}