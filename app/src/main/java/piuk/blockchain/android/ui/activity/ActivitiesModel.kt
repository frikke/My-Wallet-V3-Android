package piuk.blockchain.android.ui.activity

import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.rx3.asObservable
import timber.log.Timber

enum class ActivitiesSheet {
    ACCOUNT_SELECTOR,
    CRYPTO_ACTIVITY_DETAILS,
    FIAT_ACTIVITY_DETAILS
}

enum class ActivityType {
    NON_CUSTODIAL,
    CUSTODIAL_TRADING,
    CUSTODIAL_INTEREST,
    CUSTODIAL_TRANSFER,
    SWAP,
    SELL,
    RECURRING_BUY,
    UNKNOWN
}

data class ActivitiesState(
    val account: BlockchainAccount? = null,
    val activityList: ActivitySummaryList = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshRequested: Boolean = false,
    val bottomSheet: ActivitiesSheet? = null,
    val isError: Boolean = false,
    val selectedTxId: String = "",
    val selectedCurrency: Currency? = null,
    val activityType: ActivityType = ActivityType.UNKNOWN,
) : MviState

class ActivitiesModel(
    initialState: ActivitiesState,
    uiScheduler: Scheduler,
    private val interactor: ActivitiesInteractor,
    private val walletModeService: WalletModeService,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger,
) : MviModel<ActivitiesState, ActivitiesIntent>(
    initialState,
    uiScheduler,
    environmentConfig,
    remoteLogger
) {

    private var fetchSubscription: Disposable? = null

    override fun performAction(
        previousState: ActivitiesState,
        intent: ActivitiesIntent,
    ): Disposable? {
        Timber.d("***> performAction: ${intent.javaClass.simpleName}")

        return when (intent) {
            is AccountSelectedIntent -> {
                fetchSubscription?.dispose()

                fetchSubscription = interactor.getActivityForAccount(intent.account, intent.isRefreshRequested)
                    .subscribeBy(
                        onNext = { list ->
                            process(ActivityListUpdatedIntent(list))
                        },
                        onComplete = {
                            // do nothing
                        },
                        onError = {
                            Timber.e(it)
                            process(ActivityListUpdatedErrorIntent)
                        }
                    )

                fetchSubscription
            }
            is SelectDefaultAccountIntent ->
                walletModeService.walletMode.asObservable().flatMapSingle { interactor.getDefaultAccount(it) }
                    .subscribeBy(
                        onNext = { account ->
                            process(AccountSelectedIntent(account, false))
                        },
                        onError = { process(ActivityListUpdatedErrorIntent) }
                    )
            is CancelSimpleBuyOrderIntent -> interactor.cancelSimpleBuyOrder(intent.orderId)
            else -> null
        }
    }
}
