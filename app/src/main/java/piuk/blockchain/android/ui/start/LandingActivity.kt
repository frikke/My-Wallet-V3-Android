package piuk.blockchain.android.ui.start

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.coreui.carousel.CarouselViewType
import com.blockchain.coreui.price.PriceView
import com.blockchain.featureflags.GatedFeature
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.koin.scopedInject
import com.blockchain.koin.ssoAccountRecoveryFeatureFlag
import com.blockchain.remoteconfig.FeatureFlag
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.loader.ALGO
import piuk.blockchain.android.coincore.loader.CLOUT
import piuk.blockchain.android.coincore.loader.DOGE
import piuk.blockchain.android.coincore.loader.DOT
import piuk.blockchain.android.coincore.loader.ETC
import piuk.blockchain.android.coincore.loader.MOB
import piuk.blockchain.android.coincore.loader.STX
import piuk.blockchain.android.coincore.loader.THETA
import piuk.blockchain.android.data.connectivity.ConnectivityStatus
import piuk.blockchain.android.databinding.ActivityLandingBinding
import piuk.blockchain.android.ui.base.MvpActivity
import piuk.blockchain.android.ui.createwallet.CreateWalletActivity
import piuk.blockchain.android.ui.createwallet.NewCreateWalletActivity
import piuk.blockchain.android.ui.customviews.toast
import piuk.blockchain.android.ui.recover.AccountRecoveryActivity
import piuk.blockchain.android.ui.recover.RecoverFundsActivity
import piuk.blockchain.android.urllinks.WALLET_STATUS_URL
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.visible

class LandingActivity : MvpActivity<LandingView, LandingPresenter>(), LandingView {

    override val presenter: LandingPresenter by scopedInject()

    private val ssoARFF: FeatureFlag by inject(ssoAccountRecoveryFeatureFlag)
    private val internalFlags: InternalFeatureFlagApi by inject()
    private val compositeDisposable = CompositeDisposable()
    override val view: LandingView = this

    private val binding: ActivityLandingBinding by lazy {
        ActivityLandingBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        with(binding) {
            btnCreate.setOnClickListener { launchCreateWalletActivity() }

            if (!ConnectivityStatus.hasConnectivity(this@LandingActivity)) {
                showConnectivityWarning()
            } else {
                presenter.checkForRooted()
            }

//            textVersion.text =
//                "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) ${BuildConfig.COMMIT_HASH}"
//
//            textVersion.copyHashOnLongClick(this@LandingActivity)
            carousel.listAdapter.submitList(
                listOf(
                    CarouselViewType.ValuePropView(
                        com.blockchain.coreui.R.drawable.carousel_placeholder_1, "Buy, Sell and Swap Crypto in Seconds."
                    ),
                    CarouselViewType.ValuePropView(
                        com.blockchain.coreui.R.drawable.carousel_placeholder_2, "Earn Interest on Your Crypto."
                    ),
                    CarouselViewType.ValuePropView(
                        com.blockchain.coreui.R.drawable.carousel_placeholder_3, "Control Your Crypto with Private Keys."
                    ),
                    CarouselViewType.PriceListView(
                        "Never Miss a Crypto Moment.",
                        listOf(
                            PriceView.Price(CLOUT.logo, CLOUT.name, CLOUT.ticker, "$4.00", -0.045),
                            PriceView.Price(ALGO.logo, ALGO.name, ALGO.ticker, "$451.00", 0.052),
                            PriceView.Price(DOT.logo, DOT.name, DOT.ticker, "$4.23", -0.02),
                            PriceView.Price(DOGE.logo, DOGE.name, DOGE.ticker, "$0.52", 0.42),
                            PriceView.Price(STX.logo, STX.name, STX.ticker, "$4523.11", 0.2134),
                            PriceView.Price(MOB.logo, MOB.name, MOB.ticker, "$3.40", -0.0523),
                            PriceView.Price(THETA.logo, THETA.name, THETA.ticker, "$4.00", -0.4),
                            PriceView.Price(CryptoCurrency.BTC.logo, CryptoCurrency.BTC.name, CryptoCurrency.BTC.ticker, "$420000.00", 0.01),
                            PriceView.Price(ETC.logo, ETC.name, ETC.ticker, "$4540.21", 0.05)
                    )
                )
            ))

            carousel.setOnScrollChangeListener { _, _, _, _, _ ->
                carouselIndicators.selectedIndicator =
                    (carousel.layoutManager as? LinearLayoutManager)?.findFirstCompletelyVisibleItemPosition() ?: 0
            }


        }
    }

    override fun onStart() {
        super.onStart()
        setupSSOControls()
    }

    override fun onStop() {
        compositeDisposable.clear()
        super.onStop()
    }

    private fun launchSSOAccountRecoveryFlow() =
        startActivity(Intent(this, AccountRecoveryActivity::class.java))

    private fun setupSSOControls() {
        with(binding) {
            btnLogin.setRightButtonOnClickListener {
                launchSSOLoginActivity()
            }
            compositeDisposable += ssoARFF.enabled
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { isAccountRecoveryEnabled ->
                        btnLogin.apply {
                            if (isAccountRecoveryEnabled &&
                                internalFlags.isFeatureEnabled(GatedFeature.ACCOUNT_RECOVERY)
                            ) {
                                leftButtonText = getString(R.string.restore_wallet_cta)
                                setLeftButtonOnClickListener { launchSSOAccountRecoveryFlow() }
                            } else {
                                leftButtonText = getString(R.string.recover_funds)
                                setLeftButtonOnClickListener { showFundRecoveryWarning() }
                            }
                        }
                    },
                    onError = {
                        btnLogin.setOnClickListener { launchLoginActivity() }
                        btnLogin.apply {
                            leftButtonText = getString(R.string.recover_funds)
                            setLeftButtonOnClickListener { showFundRecoveryWarning() }
                        }
                    }
                )
        }
    }

    private fun launchCreateWalletActivity() {
        if (internalFlags.isFeatureEnabled(GatedFeature.LOCALISATION_SIGN_UP)) {
            NewCreateWalletActivity.start(this)
        } else {
            CreateWalletActivity.start(this)
        }
    }

    private fun launchLoginActivity() =
        startActivity(Intent(this, LoginActivity::class.java))

    private fun launchSSOLoginActivity() =
        startActivity(Intent(this, piuk.blockchain.android.ui.login.LoginActivity::class.java))

    private fun startRecoverFundsActivity() = RecoverFundsActivity.start(this)

    private fun showConnectivityWarning() =
        showAlert(AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setMessage(getString(R.string.check_connectivity_exit))
            .setCancelable(false)
            .setNegativeButton(R.string.exit) { _, _ -> finishAffinity() }
            .setPositiveButton(R.string.retry) { _, _ ->
                val intent = Intent(this, LandingActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            .create()
        )

    private fun showFundRecoveryWarning() =
        showAlert(AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(R.string.recover_funds_warning_message_1)
            .setPositiveButton(R.string.dialog_continue) { _, _ -> startRecoverFundsActivity() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> clearAlert() }
            .create()
        )

    override fun showIsRootedWarning() =
        showAlert(AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setMessage(R.string.device_rooted)
            .setCancelable(false)
            .setPositiveButton(R.string.dialog_continue) { _, _ -> clearAlert() }
            .create()
        )

    override fun showApiOutageMessage() {
        binding.layoutWarning.root.visible()
        val learnMoreMap = mapOf<String, Uri>("learn_more" to Uri.parse(WALLET_STATUS_URL))
        binding.layoutWarning.warningMessage.apply {
            movementMethod = LinkMovementMethod.getInstance()
            text = StringUtils.getStringWithMappedAnnotations(
                this@LandingActivity, R.string.wallet_issue_message, learnMoreMap
            )
        }
    }

    override fun showToast(message: String, toastType: String) = toast(message, toastType)

    companion object {
        @JvmStatic
        fun start(context: Context) {
            Intent(context, LandingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(this)
            }
        }
    }
}
