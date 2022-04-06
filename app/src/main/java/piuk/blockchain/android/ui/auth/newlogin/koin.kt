package piuk.blockchain.android.ui.auth.newlogin

import com.blockchain.koin.payloadScopeQualifier
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.dsl.module

val secureChannelModule = module {

    scope(payloadScopeQualifier) {

        factory {
            AuthNewLoginModel(
                initialState = AuthNewLoginState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                remoteLogger = get(),
                secureChannelManager = get(),
                secureChannelPrefs = get(),
                walletApi = get()
            )
        }
    }
}
