package piuk.blockchain.android.ui.kyc.limits

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.utils.schedulers.applySchedulers
import com.blockchain.data.asSingle
import com.blockchain.nabu.Feature
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.presentation.customviews.kyc.KycUpgradeNowSheet
import com.blockchain.presentation.koin.scopedInject
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityKycLimitsBinding
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.kyc.navhost.models.KycEntryPoint

class KycLimitsActivity : BlockchainActivity(), KycUpgradeNowSheet.Host {
    override val alwaysDisableScreenshots: Boolean = false

    private lateinit var binding: ActivityKycLimitsBinding

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    private val kycService: KycService by scopedInject()
    private val userFeaturePermissionService: UserFeaturePermissionService by scopedInject()

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKycLimitsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateToolbar(
            toolbarTitle = getString(com.blockchain.stringResources.R.string.feature_limits_toolbar),
            backAction = { finish() }
        )
    }

    override fun onResume() {
        super.onResume()
        disposables += Singles.zip(
            kycService.getHighestApprovedTierLevelLegacy(),
            userFeaturePermissionService.isEligibleFor(Feature.Kyc).asSingle()
        )
            .applySchedulers()
            .doOnSubscribe { showLoading() }
            .doOnTerminate { hideLoading() }
            .subscribeBy(
                onSuccess = { (tier, isEligibleToKyc) ->
                    if (tier == KycTier.GOLD || !isEligibleToKyc) {
                        showLimits()
                    } else {
                        showUpgradeNow()
                    }
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
            toolbarTitle = getString(com.blockchain.stringResources.R.string.upgrade_now),
            backAction = { finish() }
        )
    }

    private fun showLimits() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, KycLimitsFragment.newInstance())
            .commit()

        updateToolbar(
            toolbarTitle = getString(com.blockchain.stringResources.R.string.feature_limits_toolbar),
            backAction = { finish() }
        )
    }

    override fun onPause() {
        disposables.clear()
        super.onPause()
    }

    override fun startKycClicked() {
        KycNavHostActivity.start(this, KycEntryPoint.Other)
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
