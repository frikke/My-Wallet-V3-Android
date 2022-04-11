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

fun AppUpdateInfo.isDownloading() = installStatus() == InstallStatus.DOWNLOADING