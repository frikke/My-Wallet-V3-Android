package piuk.blockchain.android.maintenance.domain.appupdateapi

import com.google.android.play.core.appupdate.AppUpdateInfo

interface AppUpdateInfoFactory {
    suspend fun getAppUpdateInfo(): AppUpdateInfo
}