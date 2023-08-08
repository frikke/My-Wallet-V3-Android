package com.blockchain.core.settings

import com.blockchain.nabu.NabuUserSync
import com.blockchain.network.PollService
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import com.blockchain.utils.awaitOutcome
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

internal class SettingsEmailAndSyncUpdater(
    private val settingsDataManager: SettingsDataManager,
    private val nabuUserSync: NabuUserSync
) : EmailSyncUpdater {

    override fun email(): Single<Email> {
        return settingsDataManager.fetchSettings()
            .toJustEmail()
    }

    override suspend fun pollForEmailVerification(timerInSec: Long, retries: Int): Outcome<Exception, Email> =
        PollService.poll(
            fetch = { email().awaitOutcome() },
            until = { email -> email.isVerified },
            timerInSec = timerInSec,
            retries = retries
        ).map { it.value }

    override fun updateEmailAndSync(email: String): Single<Email> =
        email()
            .flatMap { existing ->
                if (!existing.isVerified || existing.address != email) {
                    settingsDataManager.updateEmail(email)
                        .flatMapSingle { settings ->
                            nabuUserSync
                                .syncUser()
                                .andThen(Single.just(settings))
                        }
                        .toJustEmail()
                } else {
                    Single.just(existing)
                }
            }

    override fun resendEmail(email: String): Single<Email> {
        return settingsDataManager
            .resendVerificationEmail(email)
            .toJustEmail()
    }
}

private fun Observable<Settings>.toJustEmail() =
    map { Email(it.email, it.isEmailVerified) }
        .single(Email("", false))
