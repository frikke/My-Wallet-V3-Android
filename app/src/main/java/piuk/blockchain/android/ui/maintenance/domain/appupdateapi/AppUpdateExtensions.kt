package piuk.blockchain.android.ui.maintenance.domain.appupdateapi

import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.tasks.Task
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@ExperimentalCoroutinesApi
suspend fun AppUpdateManager.getInfo(): AppUpdateInfo {
    delay(1000)
    return appUpdateInfo.get()
}

@ExperimentalCoroutinesApi
private suspend fun Task<AppUpdateInfo>.get(): AppUpdateInfo = suspendCoroutine { continuation ->
    addOnSuccessListener { appUpdateInfo ->
        continuation.resume(appUpdateInfo)
    }

    addOnFailureListener {
        continuation.resumeWithException(it)
    }
}

/**
 * If the download flow was already triggered and is in one of the states
 * [InstallStatus.PENDING], [InstallStatus.DOWNLOADING], [InstallStatus.INSTALLING]
 * the flow should automatically be started to show play store ui
 */
fun AppUpdateInfo.isDownloadTriggered(): Boolean {
    return installStatus() == InstallStatus.PENDING ||
        installStatus() == InstallStatus.DOWNLOADING ||
        installStatus() == InstallStatus.INSTALLING
}
