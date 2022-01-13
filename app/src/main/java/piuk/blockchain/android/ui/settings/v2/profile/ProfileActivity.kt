package piuk.blockchain.android.ui.settings.v2.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.BasicProfileInfo
import com.blockchain.nabu.Tier
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityProfileBinding
import piuk.blockchain.android.databinding.ToolbarGeneralBinding
import piuk.blockchain.android.ui.base.mvi.MviActivity
import piuk.blockchain.android.ui.base.showFragment
import piuk.blockchain.android.ui.settings.v2.RedesignSettingsPhase2Activity.Companion.BASIC_INFO
import piuk.blockchain.android.ui.settings.v2.RedesignSettingsPhase2Activity.Companion.USER_TIER

class ProfileActivity :
    MviActivity<ProfileModel,
        ProfileIntent,
        ProfileState,
        ActivityProfileBinding>() {

    override val model: ProfileModel by scopedInject()

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
        supportFragmentManager.showFragment(
            fragment = ProfileFragment.newInstance(basicProfileInfo, userTier),
            reloadFragment = true
        )
        setupToolbar()
    }

    private fun setupToolbar() {
        updateToolbar(
            toolbarTitle = getString(R.string.profile_toolbar),
            backAction = { onBackPressed() }
        )
    }

    override fun render(newState: ProfileState) {}

    companion object {
        fun newIntent(context: Context, basicProfileInfo: BasicProfileInfo, tier: Tier) =
            Intent(context, ProfileActivity::class.java).apply {
                putExtra(BASIC_INFO, basicProfileInfo)
                putExtra(USER_TIER, tier)
            }
    }
}
