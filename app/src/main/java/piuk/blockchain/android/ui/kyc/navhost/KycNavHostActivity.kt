package piuk.blockchain.android.ui.kyc.navhost

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import com.blockchain.analytics.events.KYCAnalyticsEvents
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.viewextensions.invisibleIf
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.KycNavXmlDirections
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.ActivityKycNavHostBinding
import piuk.blockchain.android.ui.base.BaseMvpActivity
import piuk.blockchain.android.ui.kyc.complete.ApplicationCompleteFragment
import piuk.blockchain.android.ui.kyc.email.entry.EmailEntryHost
import piuk.blockchain.android.ui.kyc.email.entry.KycEmailEntryFragmentDirections
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class KycNavHostActivity :
    BaseMvpActivity<KycNavHostView, KycNavHostPresenter>(),
    KycProgressListener,
    KycNavHostView {

    private val binding: ActivityKycNavHostBinding by lazy {
        ActivityKycNavHostBinding.inflate(layoutInflater)
    }

    private val kycNavHastPresenter: KycNavHostPresenter by scopedInject()
    private var navInitialDestination: NavDestination? = null
    private val navController: NavController by lazy {
        (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment).navController
    }
    private val currentFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(R.id.nav_host)

    private val compositeDisposable = CompositeDisposable()
    private val userIdentity: UserIdentity by scopedInject()

    override val campaignType by unsafeLazy {
        intent.getSerializableExtra(EXTRA_CAMPAIGN_TYPE) as CampaignType
    }

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateToolbar(
            toolbarTitle = getString(R.string.identity_verification),
            backAction = { onBackPressed() }
        )
        analytics.logEvent(
            KYCAnalyticsEvents.UpgradeKycVeriffClicked(
                campaignType.toLaunchOrigin(),
                Tier.GOLD.ordinal
            )
        )
        navController.setGraph(R.navigation.kyc_nav, intent.extras)

        onViewReady()
    }

    override fun setHostTitle(title: Int) {
        updateToolbarTitle(getString(title))
    }

    override fun displayLoading(loading: Boolean) {
        binding.frameLayoutFragmentWrapper.invisibleIf(loading)
        binding.progressBarLoadingUser.invisibleIf(!loading)
    }

    override fun showErrorSnackbarAndFinish(@StringRes message: Int) {
        BlockchainSnackbar.make(
            binding.root,
            getString(message),
            type = SnackbarType.Error
        ).show()
        finish()
    }

    override fun navigate(directions: NavDirections) {
        navController.navigate(directions)
        navInitialDestination = navController.currentDestination
    }

    override fun navigateToKycSplash() {
        navController.navigate(KycNavXmlDirections.actionDisplayKycSplash())
        navInitialDestination = navController.currentDestination
    }

    override fun navigateToResubmissionSplash() {
        navController.navigate(KycNavXmlDirections.actionDisplayResubmissionSplash())
        navInitialDestination = navController.currentDestination
    }

    override fun hideBackButton() {
        updateToolbarTitle(
            title = getString(R.string.identity_verification)
        )
    }

    override fun onEmailEntryFragmentUpdated(shouldShowButton: Boolean, buttonAction: () -> Unit) {
        updateToolbarTitle(
            title = getString(R.string.kyc_email_title)
        )
    }

    override fun onEmailVerified() {
        compositeDisposable +=
            Singles.zip(
                userIdentity.getUserCountry().defaultIfEmpty(""),
                userIdentity.getUserState().defaultIfEmpty("")
            )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { (country, state) ->
                        navigate(
                            KycEmailEntryFragmentDirections.actionAfterValidation(country, state, state)
                        )
                    },
                    onError = {
                        BlockchainSnackbar.make(
                            binding.root,
                            getString(R.string.common_error),
                            type = SnackbarType.Error
                        ).show()
                    }
                )
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    override fun onEmailVerificationSkipped() {
        throw IllegalStateException("Email must be verified")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        supportFragmentManager.fragments.forEach { fragment ->
            fragment.childFragmentManager.fragments.forEach {
                it.onActivityResult(
                    requestCode,
                    resultCode,
                    data
                )
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean = consume {
        if (flowShouldBeClosedAfterBackAction() || !navController.navigateUp()) {
            finish()
        }
    }

    override fun onBackPressed() {
        if (flowShouldBeClosedAfterBackAction()) {
            finish()
        } else {
            super.onBackPressed()
        }
    }

    private fun flowShouldBeClosedAfterBackAction() =
        // If on final page, close host Activity on navigate up
        currentFragment is ApplicationCompleteFragment ||
            // If not coming from settings, we want the 1st launched screen to be the 1st screen in the stack
            (navInitialDestination != null && navInitialDestination?.id == navController.currentDestination?.id)

    override fun createPresenter(): KycNavHostPresenter = kycNavHastPresenter

    override fun getView(): KycNavHostView = this

    companion object {
        const val RESULT_KYC_FOR_SDD_COMPLETE = 35432
        const val RESULT_KYC_FOR_TIER_COMPLETE = 8954234
        private const val EXTRA_CAMPAIGN_TYPE = "piuk.blockchain.android.EXTRA_CAMPAIGN_TYPE"

        @JvmStatic
        fun start(context: Context, campaignType: CampaignType) {
            newIntent(context, campaignType)
                .run { context.startActivity(this) }
        }

        @JvmStatic
        fun startForResult(activity: Activity, campaignType: CampaignType, requestCode: Int) {
            newIntent(activity, campaignType)
                .run { activity.startActivityForResult(this, requestCode) }
        }

        @JvmStatic
        fun startForResult(
            fragment: Fragment,
            campaignType: CampaignType,
            requestCode: Int
        ) {
            newIntent(fragment.requireContext(), campaignType)
                .run { fragment.startActivityForResult(this, requestCode) }
        }

        @JvmStatic
        fun newIntent(
            context: Context,
            campaignType: CampaignType
        ): Intent =
            Intent(context, KycNavHostActivity::class.java)
                .apply {
                    putExtra(EXTRA_CAMPAIGN_TYPE, campaignType)
                }
    }
}

private fun CampaignType.toLaunchOrigin(): LaunchOrigin =
    when (this) {
        CampaignType.Swap -> LaunchOrigin.SWAP
        CampaignType.Blockstack -> LaunchOrigin.AIRDROP
        CampaignType.Resubmission -> LaunchOrigin.RESUBMISSION
        CampaignType.SimpleBuy -> LaunchOrigin.SIMPLETRADE
        CampaignType.FiatFunds -> LaunchOrigin.FIAT_FUNDS
        CampaignType.Interest -> LaunchOrigin.SAVINGS
        CampaignType.None -> LaunchOrigin.SETTINGS
    }

interface KycProgressListener : EmailEntryHost {

    val campaignType: CampaignType

    fun setHostTitle(@StringRes title: Int)

    fun hideBackButton()
}
