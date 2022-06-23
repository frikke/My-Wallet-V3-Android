package piuk.blockchain.com

import android.content.SharedPreferences
import androidx.core.content.edit
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

class WalletModeRepository(private val sharedPreferences: SharedPreferences) : WalletModeService {
    override fun enabledWalletMode(): WalletMode {
        val walletModeString = sharedPreferences.getString(
            WALLET_MODE,
            ""
        )
        return WalletMode.values().firstOrNull { walletModeString == it.name } ?: WalletMode.UNIVERSAL
    }

    private val _walletMode = PublishSubject.create<WalletMode>()

    override val walletMode: Observable<WalletMode>
        get() = _walletMode

    override fun updateEnabledWalletMode(type: WalletMode) {
        sharedPreferences.edit {
            putString(WALLET_MODE, type.name)
        }.also {
            _walletMode.onNext(type)
        }
    }
}

private const val WALLET_MODE = "WALLET_MODE"
