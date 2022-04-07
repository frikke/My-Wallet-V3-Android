package piuk.blockchain.android.ui.maintenance.data.appupdateapi

import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class AppUpdateInfoFactoryImpl(
    private val appUpdateManager: AppUpdateManager
) : AppUpdateInfoFactory {
    override suspend fun getAppUpdateInfo(): AppUpdateInfo = appUpdateManager.getInfo()
}