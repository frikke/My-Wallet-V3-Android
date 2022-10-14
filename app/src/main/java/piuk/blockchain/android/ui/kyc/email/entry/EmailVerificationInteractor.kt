package piuk.blockchain.android.ui.kyc.email.entry

import com.blockchain.core.settings.Email
import com.blockchain.core.settings.EmailSyncUpdater
import com.blockchain.nabu.api.getuser.data.GetUserStore
import com.blockchain.network.PollService
import com.blockchain.utils.thenSingle
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

class EmailVerificationInteractor(
    private val emailUpdater: EmailSyncUpdater,
    private val getUserStore: GetUserStore
) {

    private val pollEmail = PollService(
        emailUpdater.email()
    ) {
        it.isVerified
    }

    fun fetchEmail(): Single<Email> =
        emailUpdater.email()

    fun pollForEmailStatus(): Single<Email> {
        return cancelPolling().thenSingle {
            pollEmail.start(timerInSec = 1, retries = Integer.MAX_VALUE).map {
                it.value
            }.doOnSuccess {
                getUserStore.invalidate()
            }
        }
    }

    fun resendEmail(email: String): Single<Email> {
        return emailUpdater.updateEmailAndSync(email)
    }

    fun updateEmail(email: String): Single<Email> =
        emailUpdater.updateEmailAndSync(email)

    fun cancelPolling(): Completable =
        Completable.fromCallable {
            pollEmail.cancel.onNext(true)
        }
}
