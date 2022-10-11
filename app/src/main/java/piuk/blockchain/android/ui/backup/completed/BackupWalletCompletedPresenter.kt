package piuk.blockchain.android.ui.backup.completed

import com.blockchain.core.auth.AuthDataManager
import com.blockchain.preferences.WalletStatusPrefs
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.base.View
import timber.log.Timber

interface BackupWalletCompletedView : View {
    fun showLastBackupDate(lastBackup: Long)
    fun hideLastBackupDate()
    fun onBackupDone()
    fun showErrorToast()
}

class BackupWalletCompletedPresenter(
    private val walletStatusPrefs: WalletStatusPrefs,
    private val authDataManager: AuthDataManager
) : BasePresenter<BackupWalletCompletedView>() {

    override fun onViewReady() {
        val lastBackup = walletStatusPrefs.lastBackupTime
        if (lastBackup != 0L) {
            view.showLastBackupDate(lastBackup)
        } else {
            view.hideLastBackupDate()
        }
    }

    fun updateMnemonicBackup() {
        compositeDisposable += authDataManager.updateMnemonicBackup()
            .subscribeBy(
                onComplete = { view.onBackupDone() },
                onError = { throwable ->
                    Timber.e(throwable)
                    view.showErrorToast()
                }
            )
    }
}
