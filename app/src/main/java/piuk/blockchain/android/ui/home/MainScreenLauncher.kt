package piuk.blockchain.android.ui.home

import android.content.Context
import android.content.Intent
import com.blockchain.logging.CrashLogger
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import piuk.blockchain.android.ui.home.v2.RedesignMainActivity

class MainScreenLauncher(
    private val walletRedesignFeatureFlag: FeatureFlag,
    private val crashLogger: CrashLogger
) {
    private val isNewIAEnabled by lazy {
        walletRedesignFeatureFlag.enabled.cache()
    }

    fun startMainActivity(context: Context, compositeDisposable: CompositeDisposable) {
        compositeDisposable.checkEnabledFlag(
            onEnabled = {
                context.startActivity(
                    RedesignMainActivity.newIntentAsNewTask(context)
                )
            },
            onDisabled = {
                context.startActivity(
                    MainActivity.newInstanceAsNewTask(context)
                )
            },
            onError = {
                context.startActivity(
                    MainActivity.newInstanceAsNewTask(context)
                )
            }
        )
    }

    fun startMainActivity(
        context: Context,
        shouldShowSwap: Boolean,
        shouldBeNewTask: Boolean,
        compositeDisposable: CompositeDisposable
    ) {
        compositeDisposable.checkEnabledFlag(
            onEnabled = {
                context.startActivity(
                    RedesignMainActivity.newIntent(context, shouldShowSwap, shouldBeNewTask)
                )
            },
            onDisabled = {
                context.startActivity(
                    MainActivity.newInstance(context, shouldShowSwap, shouldBeNewTask)
                )
            },
            onError = {
                context.startActivity(
                    MainActivity.newInstance(context, shouldShowSwap, shouldBeNewTask)
                )
            }
        )
    }

    fun startMainActivity(
        context: Context,
        intentData: String?,
        shouldLaunchBuySellIntro: Boolean,
        shouldBeNewTask: Boolean,
        compositeDisposable: CompositeDisposable
    ) {
        compositeDisposable.checkEnabledFlag(
            onEnabled = {
                context.startActivity(
                    RedesignMainActivity.newIntent(
                        context = context,
                        intentData = intentData,
                        shouldLaunchBuySellIntro = shouldLaunchBuySellIntro,
                        shouldBeNewTask = shouldBeNewTask
                    )
                )
            },
            onDisabled = {
                context.startActivity(
                    MainActivity.newInstance(
                        context = context,
                        intentData = intentData,
                        shouldLaunchBuySellIntro = shouldLaunchBuySellIntro,
                        shouldBeNewTask = shouldBeNewTask
                    )
                )
            },
            onError = {
                context.startActivity(
                    MainActivity.newInstance(
                        context = context,
                        intentData = intentData,
                        shouldLaunchBuySellIntro = shouldLaunchBuySellIntro,
                        shouldBeNewTask = shouldBeNewTask
                    )
                )
            }
        )
    }

    fun startMainActivity(
        context: Context,
        launchAuthFlow: Boolean,
        pubKeyHash: String,
        message: String,
        originIp: String?,
        originLocation: String?,
        originBrowser: String?,
        forcePin: Boolean,
        shouldBeNewTask: Boolean
    ): Single<Intent> = isNewIAEnabled
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .map { enabled ->
            if (enabled) {
                RedesignMainActivity.newIntent(
                    context = context,
                    launchAuthFlow = launchAuthFlow,
                    pubKeyHash = pubKeyHash,
                    message = message,
                    originIp = originIp,
                    originLocation = originLocation,
                    originBrowser = originBrowser,
                    forcePin = forcePin,
                    shouldBeNewTask = shouldBeNewTask
                )
            } else {
                MainActivity.newInstance(
                    context = context,
                    launchAuthFlow = launchAuthFlow,
                    pubKeyHash = pubKeyHash,
                    message = message,
                    originIp = originIp,
                    originLocation = originLocation,
                    originBrowser = originBrowser,
                    forcePin = forcePin,
                    shouldBeNewTask = shouldBeNewTask
                )
            }
        }.doOnError {
            crashLogger.logEvent("Error getting new IA FF")
        }.onErrorReturnItem(
            MainActivity.newInstance(
                context = context,
                launchAuthFlow = launchAuthFlow,
                pubKeyHash = pubKeyHash,
                message = message,
                originIp = originIp,
                originLocation = originLocation,
                originBrowser = originBrowser,
                forcePin = forcePin,
                shouldBeNewTask = shouldBeNewTask
            )
        )

    fun startMainActivity(
        context: Context,
        intentFromNotification: Boolean
    ): Single<Intent> = isNewIAEnabled
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .map { enabled ->
            if (enabled) {
                RedesignMainActivity.newIntent(context, intentFromNotification)
            } else {
                MainActivity.newInstance(context, intentFromNotification)
            }
        }.doOnError {
            crashLogger.logEvent("Error getting new IA FF")
        }
        .onErrorReturnItem(
            MainActivity.newInstance(context, intentFromNotification)
        )

    private fun CompositeDisposable.checkEnabledFlag(
        onEnabled: () -> Unit,
        onDisabled: () -> Unit,
        onError: () -> Unit
    ) {
        this += isNewIAEnabled
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { enabled ->
                    if (enabled) {
                        onEnabled()
                    } else {
                        onDisabled()
                    }
                },
                onError = {
                    crashLogger.logEvent("Error getting new IA FF")
                    onError()
                }
            )
    }
}
