package piuk.blockchain.android.maintenance.data.appupdateapi

import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.tasks.Task
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun AppUpdateManager.getInfo() = appUpdateInfo.get()

private suspend fun Task<AppUpdateInfo>.get(): AppUpdateInfo = suspendCancellableCoroutine { continuation ->
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
 * the flow should automatically resume on app launch to show play store ui
 */
fun AppUpdateInfo.isDownloadTriggered(): Boolean {
    return installStatus() == InstallStatus.PENDING ||
        installStatus() == InstallStatus.DOWNLOADING ||
        installStatus() == InstallStatus.INSTALLING
}
