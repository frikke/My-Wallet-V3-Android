package piuk.blockchain.android.ui.settings

import android.content.Context
import android.content.Intent
import com.blockchain.logging.CrashLogger
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import piuk.blockchain.android.ui.home.flags.RedesignPart2FeatureFlag
import piuk.blockchain.android.ui.settings.v2.RedesignSettingsPhase2Activity
import piuk.blockchain.android.ui.settings.v2.SettingsActivity

class SettingsScreenLauncher(
    private val redesignPart2FeatureFlag: RedesignPart2FeatureFlag,
    private val crashLogger: CrashLogger
) {
    private val isNewIAEnabled by lazy {
        redesignPart2FeatureFlag.enabled.cache()
    }

    fun startSettingsActivity(context: Context, compositeDisposable: CompositeDisposable) {
        compositeDisposable.checkEnabledFlag(
            onEnabled = {
                context.startActivity(
                    RedesignSettingsPhase2Activity.newIntent(context)
                )
            },
            onDisabled = {
                context.startActivity(
                    SettingsActivity.newIntent(context)
                )
            },
            onError = {
                context.startActivity(
                    SettingsActivity.newIntent(context)
                )
            }
        )
    }

    fun newIntent(context: Context): Single<Intent> =
        isNewIAEnabled.map {
            if (it) {
                RedesignSettingsPhase2Activity.newIntent(context)
            } else {
                SettingsActivity.newIntent(context)
            }
        }.onErrorResumeNext { Single.just(SettingsActivity.newIntent(context)) }

    fun startSettingsActivityFor2FA(context: Context, compositeDisposable: CompositeDisposable) {
        compositeDisposable.checkEnabledFlag(
            onEnabled = {
                context.startActivity(
                    RedesignSettingsPhase2Activity.newIntentFor2FA(context)
                )
            },
            onDisabled = {
                context.startActivity(
                    SettingsActivity.newIntentFor2FA(context)
                )
            },
            onError = {
                context.startActivity(
                    SettingsActivity.newIntentFor2FA(context)
                )
            }
        )
    }

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
