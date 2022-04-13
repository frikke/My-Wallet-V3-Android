package piuk.blockchain.android.maintenance.data.appupdateapi

import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import piuk.blockchain.android.maintenance.domain.appupdateapi.AppUpdateInfoFactory

internal class AppUpdateInfoFactoryImpl(
    private val appUpdateManager: AppUpdateManager
) : AppUpdateInfoFactory {
    override suspend fun getAppUpdateInfo(): AppUpdateInfo = appUpdateManager.getInfo()
}