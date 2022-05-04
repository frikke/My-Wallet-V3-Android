package piuk.blockchain.android.maintenance.domain.appupdateapi

import com.google.android.play.core.appupdate.AppUpdateInfo

/**
 * Factory to create new instances of appUpdateInfo
 *
 * If the user cancels the download from playstore ui and tries to download again,
 * the pendingintent created by appUpdateManager needs a new instance of appUpdateInfo
 */
interface AppUpdateInfoFactory {
    suspend fun getAppUpdateInfo(): AppUpdateInfo
}
