package com.blockchain.core.settings

import com.blockchain.outcome.Outcome
import io.reactivex.rxjava3.core.Single

data class Email(
    val address: String,
    val isVerified: Boolean
)

interface EmailSyncUpdater {

    fun email(): Single<Email>

    suspend fun pollForEmailVerification(timerInSec: Long = 5, retries: Int = 20): Outcome<Exception, Email>

    /**
     * Does nothing when email is unchanged and verified.
     * Syncs changes with Nabu.
     */
    fun updateEmailAndSync(email: String): Single<Email>

    /**
     * Always sends a new email, even if verified
     */
    fun resendEmail(email: String): Single<Email>
}
