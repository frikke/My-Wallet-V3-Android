package piuk.blockchain.android.util

import com.blockchain.wallet.BackupWallet
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import timber.log.Timber
import java.security.SecureRandom

class BackupWalletUtil(
    private val payloadDataManager: PayloadDataManager
) : BackupWallet {

    override fun getConfirmSequence(secondPassword: String?): List<Pair<Int, String>> {
        val mnemonic = getMnemonic(secondPassword)
        val randomGenerator = SecureRandom()
        val seen = mutableListOf<Int>()

        var i = 0
        while (i < 3) {
            val number = randomGenerator.nextInt(mnemonic!!.size)
            if (!seen.contains(number)) {
                seen.add(number)
                i++
            }
        }

        seen.sort()

        return (0..2).map { seen[it] to mnemonic!![seen[it]] }
    }

    override fun getMnemonic(secondPassword: String?): List<String>? = try {
        payloadDataManager.wallet?.let {
            it.decryptHDWallet(secondPassword)
            it.walletBody?.getMnemonic()?.toList()
        }
    } catch (e: Exception) {
        Timber.e(e)
        null
    }
}
