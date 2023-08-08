package piuk.blockchain.android.ui.settings.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.commonarch.presentation.base.addTransactionAnimation
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.nabu.BasicProfileInfo
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityProfileBinding
import piuk.blockchain.android.ui.base.showFragment
import piuk.blockchain.android.ui.settings.SettingsActivity.Companion.BASIC_INFO
import piuk.blockchain.android.ui.settings.SettingsActivity.Companion.USER_TIER
import piuk.blockchain.android.ui.settings.profile.email.UpdateEmailFragment
import piuk.blockchain.android.ui.settings.profile.phone.UpdatePhoneFragment

class ProfileActivity :
    BlockchainActivity(),
    ProfileNavigator {

    private val binding: ActivityProfileBinding by lazy {
        ActivityProfileBinding.inflate(layoutInflater)
    }

    override val alwaysDisableScreenshots: Boolean = true

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    private val basicProfileInfo by lazy {
        intent.getSerializableExtra(BASIC_INFO) as BasicProfileInfo
    }

    private val userTier by lazy {
        intent.getSerializableExtra(USER_TIER) as KycTier
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        updateToolbarBackAction { onBackPressedDispatcher.onBackPressed() }

        supportFragmentManager.showFragment(
            fragment = ProfileFragment.newInstance(basicProfileInfo, userTier),
            reloadFragment = true
        )
    }

    override fun showLoading() = binding.progress.visible()

    override fun hideLoading() = binding.progress.gone()

    override fun goToUpdateEmailScreen(addToBackStack: Boolean) {
        supportFragmentManager.beginTransaction()
            .addTransactionAnimation()
            .replace(R.id.content_frame, UpdateEmailFragment.newInstance(), UpdateEmailFragment::class.simpleName)
            .apply {
                if (addToBackStack) {
                    addToBackStack(UpdateEmailFragment::class.simpleName)
                }
            }
            .commitAllowingStateLoss()
    }

    override fun goToUpdatePhoneScreen(addToBackStack: Boolean) {
        supportFragmentManager.beginTransaction()
            .addTransactionAnimation()
            .replace(R.id.content_frame, UpdatePhoneFragment.newInstance(), UpdatePhoneFragment::class.simpleName)
            .apply {
                if (addToBackStack) {
                    addToBackStack(UpdatePhoneFragment::class.simpleName)
                }
            }
            .commitAllowingStateLoss()
    }

    companion object {
        fun newIntent(context: Context, basicProfileInfo: BasicProfileInfo, tier: KycTier) =
            Intent(context, ProfileActivity::class.java).apply {
                putExtra(BASIC_INFO, basicProfileInfo)
                putExtra(USER_TIER, tier)
            }
    }
}
