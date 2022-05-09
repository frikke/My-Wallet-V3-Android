package piuk.blockchain.android.ui.kyc.limits

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.ActivityKycLimitsBinding
import piuk.blockchain.android.ui.dashboard.sheets.KycUpgradeNowSheet
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity

class KycLimitsActivity : BlockchainActivity(), KycUpgradeNowSheet.Host {
    override val alwaysDisableScreenshots: Boolean = false

    private lateinit var binding: ActivityKycLimitsBinding

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    private val userIdentity: UserIdentity by scopedInject()

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKycLimitsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateToolbar(
            toolbarTitle = getString(R.string.feature_limits_toolbar),
            backAction = { finish() }
        )
    }

    override fun onResume() {
        super.onResume()
        disposables += userIdentity.getHighestApprovedKycTier()
            .doOnSubscribe { showLoading() }
            .doOnTerminate { hideLoading() }
            .subscribeBy(
                onSuccess = {
                    if (it == Tier.GOLD) showLimits()
                    else showUpgradeNow()
                },
                onError = {
                    showLimits()
                }
            )
    }

    private fun showUpgradeNow() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, KycUpgradeNowSheet.newInstance())
            .commit()

        updateToolbar(
            toolbarTitle = getString(R.string.upgrade_now),
            backAction = { finish() }
        )
    }

    private fun showLimits() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, KycLimitsFragment.newInstance())
            .commit()

        updateToolbar(
            toolbarTitle = getString(R.string.feature_limits_toolbar),
            backAction = { finish() }
        )
    }

    override fun onPause() {
        disposables.clear()
        super.onPause()
    }

    override fun startKycClicked() {
        KycNavHostActivity.start(this, CampaignType.None)
    }

    override fun onSheetClosed() {
    }

    override fun showLoading() {
        binding.progress.visible()
        binding.progress.playAnimation()
    }

    override fun hideLoading() {
        binding.progress.gone()
        binding.progress.pauseAnimation()
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, KycLimitsActivity::class.java)
    }
}
