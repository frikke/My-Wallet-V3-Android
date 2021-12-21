package piuk.blockchain.android.ui.settings

import android.content.Context
import android.content.Intent
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.ui.home.flags.RedesignPart2FeatureFlag
import piuk.blockchain.android.ui.settings.v2.RedesignSettingsPhase2Activity
import piuk.blockchain.android.ui.settings.v2.SettingsActivity

class SettingsScreenLauncher(
    private val redesignPart2FeatureFlag: RedesignPart2FeatureFlag
) {
    private val isNewIAEnabled by lazy {
        redesignPart2FeatureFlag.enabled.cache()
    }

    fun newIntent(context: Context): Single<Intent> =
        isNewIAEnabled.map {
            if (it) {
                RedesignSettingsPhase2Activity.newIntent(context)
            } else {
                SettingsActivity.newIntent(context)
            }
        }.onErrorResumeNext { Single.just(SettingsActivity.newIntent(context)) }
}
