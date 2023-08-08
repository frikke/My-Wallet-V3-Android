package com.blockchain.veriff

import android.app.Activity
import android.content.Intent
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import com.veriff.Branding
import com.veriff.Configuration
import com.veriff.Result
import com.veriff.Sdk
import timber.log.Timber

class VeriffLauncher {

    fun launchVeriff(activity: Activity, applicant: VeriffApplicantAndToken, requestCode: Int) {
        val sessionToken = applicant.token
        Timber.d("Veriff session token: $sessionToken")

        val branding = Branding.Builder()
            .logo(com.blockchain.componentlib.R.drawable.ic_blockchain_logo_with_text_veriff)
            .primary(ContextCompat.getColor(activity, com.blockchain.componentlib.R.color.primary))
            .build()

        val configuration = Configuration.Builder()
            .branding(branding)
            .build()

        val intent = Sdk.createLaunchIntent(activity, sessionToken, configuration)

        startActivityForResult(activity, intent, requestCode, null)
    }
}

class VeriffResultHandler(
    private val onSuccess: () -> Unit,
    private val onError: (String) -> Unit
) {
    fun handleResult(data: Intent) {
        val result = Result.fromResultIntent(data) ?: return
        when (result.status) {
            Result.Status.DONE -> onSuccess()
            Result.Status.ERROR -> onError(result.error?.name.orEmpty())
            Result.Status.CANCELED -> { }
        }
    }
}
