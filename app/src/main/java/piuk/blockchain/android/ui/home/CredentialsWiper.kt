package piuk.blockchain.android.ui.home

import com.blockchain.api.services.ActivityWebSocketService
import com.blockchain.core.chains.bitcoincash.BchDataManager
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.internalnotifications.NotificationEvent
import com.blockchain.internalnotifications.NotificationTransmitter
import com.blockchain.metadata.MetadataService
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.notifications.NotificationTokenManager
import com.blockchain.storedatasource.StoreWiper
import com.blockchain.unifiedcryptowallet.domain.activity.service.UnifiedActivityService
import com.blockchain.utils.then
import com.blockchain.utils.thenSingle
import com.blockchain.walletconnect.domain.WalletConnectV2Service
import com.blockchain.walletmode.WalletModeService
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.rx3.rxCompletable
import piuk.blockchain.android.util.AppUtil
import timber.log.Timber

class CredentialsWiper(
    private val ethDataManager: EthDataManager,
    private val appUtil: AppUtil,
    private val unifiedActivityService: UnifiedActivityService,
    private val activityWebSocketService: ActivityWebSocketService,
    private val walletModeService: WalletModeService,
    private val notificationTokenManager: NotificationTokenManager,
    private val bchDataManager: BchDataManager,
    private val metadataService: MetadataService,
    private val nabuDataManager: NabuDataManager,
    private val storeWiper: StoreWiper,
    private val notificationTransmitter: NotificationTransmitter,
    private val intercomEnabledFF: FeatureFlag,
    private val walletConnectV2Service: WalletConnectV2Service,
) {
    fun wipe() {
        notificationTokenManager.revokeAccessToken().then {
            Completable.fromAction {
                unifiedActivityService.clear()
                appUtil.unpairWallet()
                activityWebSocketService.close()
                ethDataManager.clearAccountDetails()
                bchDataManager.clearAccountDetails()
                nabuDataManager.clearAccessToken()
                walletModeService.reset()
                metadataService.reset()
                notificationTransmitter.postEvent(NotificationEvent.Logout)
            }
        }.onErrorComplete()
            .then {
                rxCompletable {
                    walletConnectV2Service.disconnectAllSessions()
                }
            }
            .then {
                rxCompletable { storeWiper.wipe() }
            }.thenSingle {
                intercomEnabledFF.enabled
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = {
                    Timber.e(it)
                    // StoreWiper failed, we can't safely recover at this point
                    throw it
                },
                onSuccess = { intercomEnabled ->
                    appUtil.logout(intercomEnabled)
                    appUtil.restartApp()
                }
            )
    }
}
