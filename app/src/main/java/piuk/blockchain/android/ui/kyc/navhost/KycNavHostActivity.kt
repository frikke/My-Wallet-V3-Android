package piuk.blockchain.android.ui.kyc.navhost

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
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
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.viewextensions.invisibleIf
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.nabu.UserIdentity
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.utils.consume
import com.blockchain.utils.rxSingleOutcome
import com.blockchain.utils.unsafeLazy
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.android.KycNavXmlDirections
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityKycNavHostBinding
import piuk.blockchain.android.fraud.domain.service.FraudFlow
import piuk.blockchain.android.fraud.domain.service.FraudService
import piuk.blockchain.android.ui.base.BaseMvpActivity
import piuk.blockchain.android.ui.kyc.email.entry.EmailEntryHost
import piuk.blockchain.android.ui.kyc.email.entry.KycEmailVerificationFragmentDirections
import piuk.blockchain.android.ui.kyc.navhost.models.KycEntryPoint

class KycNavHostActivity :
    BaseMvpActivity<KycNavHostView, KycNavHostPresenter>(),
    KycProgressListener,
    EmailEntryHost,
    KycNavHostView {

    private lateinit var backPressCallback: OnBackPressedCallback

    private val binding: ActivityKycNavHostBinding by lazy {
        ActivityKycNavHostBinding.inflate(layoutInflater)
    }

    private val kycNavHastPresenter: KycNavHostPresenter by scopedInject()
    private var navInitialDestination: NavDestination? = null
    private val navController: NavController by lazy {
        (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment).navController
    }

    private val compositeDisposable = CompositeDisposable()
    private val userIdentity: UserIdentity by scopedInject()
    private val fraudService: FraudService by inject()
    private val kycService: KycService by scopedInject()

    override val entryPoint by unsafeLazy {
        intent.getSerializableExtra(EXTRA_CAMPAIGN_TYPE) as KycEntryPoint
    }
    override val isCowboysUser: Boolean
        get() = intent.getBooleanExtra(FROM_COWBOYS, false)

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupBackPress()

        updateToolbar(
            backAction = { onBackPressedDispatcher.onBackPressed() }
        )

        updateToolbarBackground()

        analytics.logEvent(
            KYCAnalyticsEvents.UpgradeKycVeriffClicked(
                entryPoint.toLaunchOrigin(),
                KycTier.GOLD.ordinal
            )
        )

        navController.removeOnDestinationChangedListener { controller, destination, arguments ->
            verifyBackPressCallback(destination)
        }
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            verifyBackPressCallback(destination)
        }

        navController.setGraph(R.navigation.kyc_nav, intent.extras)

        onViewReady()
    }

    override fun setupHostToolbar(@StringRes title: Int?, navigationBarButtons: List<NavigationBarButton>) {
        updateToolbarTitle(title?.let { getString(title) }.orEmpty())
        updateToolbarMenuItems(navigationBarButtons)
    }

    override fun displayLoading(loading: Boolean) {
        binding.frameLayoutFragmentWrapper.invisibleIf(loading)
        binding.progressBarLoadingUser.invisibleIf(!loading)
    }

    override fun showErrorSnackbarAndFinish(@StringRes message: Int) {
        setResult(RESULT_CANCELED)

        BlockchainSnackbar.make(
            binding.root,
            getString(message),
            type = SnackbarType.Error
        ).show()

        finish()
    }

    override fun navigate(directions: NavDirections) {
        if (directions.actionId != R.id.action_startTierCurrentState) {
            updateToolbarTitle(getString(com.blockchain.stringResources.R.string.identity_verification))
        }
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
        updateToolbarBackAction(null)
    }

    override fun onEmailEntryFragmentUpdated(showSkipButton: Boolean, buttonAction: () -> Unit) {
        updateToolbarTitle(
            title = getString(com.blockchain.stringResources.R.string.kyc_email_title)
        )
    }

    override fun onEmailVerified() {
        compositeDisposable +=
            Singles.zip(
                userIdentity.getUserCountry().defaultIfEmpty(""),
                userIdentity.getUserState().defaultIfEmpty(""),
                rxSingleOutcome { kycService.shouldLaunchProve() }
            )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { (country, state, shouldLaunchProve) ->
                        if (shouldLaunchProve) {
                            navigate(
                                KycEmailVerificationFragmentDirections.actionProve(country, state)
                            )
                        } else {
                            navigate(
                                KycEmailVerificationFragmentDirections.actionAfterValidation(country, state, state)
                            )
                        }
                    },
                    onError = {
                        BlockchainSnackbar.make(
                            binding.root,
                            getString(com.blockchain.stringResources.R.string.common_error),
                            type = SnackbarType.Error
                        ).show()
                    }
                )
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        fraudService.endFlows(FraudFlow.ONBOARDING, FraudFlow.KYC)
        super.onDestroy()
    }

    override fun onEmailVerificationSkipped() {
        finish()
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
        onBackPressedDispatcher.onBackPressed()
    }

    private fun setupBackPress() {
        backPressCallback = onBackPressedDispatcher.addCallback(owner = this) {
            // see ApplicationCompleteFragment for success result
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun verifyBackPressCallback(destination: NavDestination) {
        // If not coming from settings, we want the 1st launched screen to be the 1st screen in the stack
        backPressCallback.isEnabled = navInitialDestination?.id == destination.id
    }

    override fun createPresenter(): KycNavHostPresenter = kycNavHastPresenter

    override fun getView(): KycNavHostView = this

    companion object {
        const val RESULT_KYC_FOR_TIER_COMPLETE = 8954234
        private const val EXTRA_CAMPAIGN_TYPE = "piuk.blockchain.android.EXTRA_CAMPAIGN_TYPE"
        private const val FROM_COWBOYS = "FROM_COWBOYS"

        @JvmStatic
        fun start(context: Context, entryPoint: KycEntryPoint) {
            newIntent(context, entryPoint)
                .run { context.startActivity(this) }
        }

        @JvmStatic
        fun startForResult(activity: Activity, entryPoint: KycEntryPoint, requestCode: Int) {
            newIntent(activity, entryPoint)
                .run { activity.startActivityForResult(this, requestCode) }
        }

        @JvmStatic
        fun startForResult(
            fragment: Fragment,
            entryPoint: KycEntryPoint,
            requestCode: Int
        ) {
            newIntent(fragment.requireContext(), entryPoint)
                .run { fragment.startActivityForResult(this, requestCode) }
        }

        @JvmStatic
        fun newIntent(
            context: Context,
            entryPoint: KycEntryPoint
        ): Intent =
            Intent(context, KycNavHostActivity::class.java)
                .apply {
                    putExtra(EXTRA_CAMPAIGN_TYPE, entryPoint)
                }
    }
}

private fun KycEntryPoint.toLaunchOrigin(): LaunchOrigin =
    when (this) {
        KycEntryPoint.Airdrop -> LaunchOrigin.AIRDROP
        KycEntryPoint.CoinView -> LaunchOrigin.COIN_VIEW
        KycEntryPoint.FiatFunds -> LaunchOrigin.FIAT_FUNDS
        KycEntryPoint.Interest -> LaunchOrigin.INTEREST
        KycEntryPoint.Onboarding -> LaunchOrigin.ONBOARDING
        KycEntryPoint.Resubmission -> LaunchOrigin.RESUBMISSION
        KycEntryPoint.Buy -> LaunchOrigin.SIMPLEBUY
        KycEntryPoint.Sell -> LaunchOrigin.SIMPLETRADE
        KycEntryPoint.Swap -> LaunchOrigin.SWAP
        KycEntryPoint.Cowboys -> LaunchOrigin.DASHBOARD
        KycEntryPoint.Other -> LaunchOrigin.SETTINGS
    }

interface KycProgressListener {

    val entryPoint: KycEntryPoint

    fun setupHostToolbar(@StringRes title: Int?, navigationBarButtons: List<NavigationBarButton> = emptyList())

    fun hideBackButton()
}
