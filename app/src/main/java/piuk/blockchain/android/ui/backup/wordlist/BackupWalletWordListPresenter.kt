package piuk.blockchain.android.ui.backup.wordlist

import android.os.Bundle
import com.blockchain.wallet.BackupWallet
import piuk.blockchain.android.ui.backup.wordlist.BackupWalletWordListFragment.Companion.ARGUMENT_SECOND_PASSWORD
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.base.View

interface BackupWalletWordListView : View {
    fun getPageBundle(): Bundle?
    fun finish()
}

class BackupWalletWordListPresenter(
    private val backupWallet: BackupWallet
) : BasePresenter<BackupWalletWordListView>() {

    internal var secondPassword: String? = null
    private var mnemonic: List<String>? = null

    override fun onViewReady() {
        val bundle = view.getPageBundle()
        secondPassword = bundle?.getString(ARGUMENT_SECOND_PASSWORD)

        mnemonic = backupWallet.getMnemonic(secondPassword)
        if (mnemonic == null) {
            view.finish()
        }
    }

    internal fun getWordForIndex(index: Int) = mnemonic?.let {
        it[index]
    }

    internal fun getMnemonicSize() = mnemonic?.size ?: -1
}
