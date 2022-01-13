package piuk.blockchain.android.ui.settings.v2.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.nabu.Tier
import org.koin.core.scope.Scope
import org.koin.java.KoinJavaComponent
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityProfileBinding
import piuk.blockchain.android.databinding.ToolbarGeneralBinding
import piuk.blockchain.android.ui.base.addAnimationTransaction
import piuk.blockchain.android.ui.base.mvi.MviActivity
import piuk.blockchain.android.ui.base.showFragment
import piuk.blockchain.android.ui.settings.v2.RedesignSettingsPhase2Activity.Companion.BASIC_INFO
import piuk.blockchain.android.ui.settings.v2.RedesignSettingsPhase2Activity.Companion.USER_TIER
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import timber.log.Timber

class ProfileActivity :
    MviActivity<ProfileModel,
        ProfileIntent,
        ProfileState,
        ActivityProfileBinding>(),
    ProfileNavigator {

    private val scopeId: String by lazy {
        "${TX_SCOPE_ID}_${this@ProfileActivity.hashCode()}"
    }

    val scope: Scope by lazy {
        openScope()
        KoinJavaComponent.getKoin().getScope(scopeId)
    }

    override val model: ProfileModel by scope.inject()

    override fun initBinding(): ActivityProfileBinding =
        ActivityProfileBinding.inflate(layoutInflater)

    override val alwaysDisableScreenshots: Boolean = true

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    private val basicProfileInfo by lazy {
        intent.getSerializableExtra(BASIC_INFO) as BasicProfileInfo
    }

    private val userTier by lazy {
        intent.getSerializableExtra(USER_TIER) as Tier
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        updateToolbarBackAction { onBackPressed() }

        supportFragmentManager.showFragment(
            fragment = ProfileFragment.newInstance(basicProfileInfo, userTier),
            reloadFragment = true
        )
    }

    override fun showLoading() = binding.progress.visible()

    override fun hideLoading() = binding.progress.gone()

    override fun render(newState: ProfileState) {}

    override fun goToUpdateEmailScreen(addToBackStack: Boolean) {
        supportFragmentManager.beginTransaction()
            .addAnimationTransaction()
            .replace(R.id.content_frame, UpdateEmailFragment.newInstance(), UpdateEmailFragment::class.simpleName)
            .apply {
                if (addToBackStack) {
                    addToBackStack(UpdateEmailFragment::class.simpleName)
                }
            }
            .commitAllowingStateLoss()
    }

    override fun goToUpdatePhoneScreen(addToBackStack: Boolean) {
        // TODO next ticket
//        supportFragmentManager.showFragment(
//            fragment = UpdateEmailFragment.newInstance()
//        )
//        supportFragmentManager.beginTransaction()
//            .addAnimationTransaction()
//            .replace(R.id.content_frame, UpdatePhoneFragment.newInstance(), UpdatePhoneFragment::class.simpleName)
//            .apply {
//                if (addToBackStack) {
//                    addToBackStack(UpdatePhoneFragment::class.simpleName)
//                }
//            }
//            .commitAllowingStateLoss()
    }

    private fun openScope() =
        try {
            KoinJavaComponent.getKoin().getOrCreateScope(
                scopeId,
                payloadScopeQualifier
            )
        } catch (e: Throwable) {
            Timber.wtf("Error opening scope for id $scopeId - $e")
        }

    companion object {

        private const val TX_SCOPE_ID = "PROFILE_SCOPE_ID"

        fun newIntent(context: Context, basicProfileInfo: BasicProfileInfo, tier: Tier) =
            Intent(context, ProfileActivity::class.java).apply {
                putExtra(BASIC_INFO, basicProfileInfo)
                putExtra(USER_TIER, tier)
            }
    }
}
