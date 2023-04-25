package piuk.blockchain.android.ui.transfer.receive

import com.blockchain.koin.payloadScopeQualifier
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.dsl.module
import piuk.blockchain.android.ui.transfer.receive.detail.ReceiveDetailModel
import piuk.blockchain.android.ui.transfer.receive.detail.ReceiveDetailState
import piuk.blockchain.android.util.AppUtil

val receiveCryptoModule = module {

    scope(payloadScopeQualifier) {
        factory {
            ReceiveDetailModel(
                _activityIndicator = lazy { get<AppUtil>().activityIndicator },
                initialState = ReceiveDetailState(),
                uiScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                remoteLogger = get()
            )
        }

        factory {
            ReceiveModel(
                initialState = ReceiveState(),
                uiScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                remoteLogger = get(),
                walletModeService = get(),
                coincore = get(),
            )
        }
    }
}
