package piuk.blockchain.android.ui.kyc.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintSet
import com.blockchain.analytics.data.logEvent
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.coincore.AssetAction
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.legacy.MaterialProgressDialog
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.px
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.core.kyc.domain.model.KycTierState
import com.blockchain.presentation.getResolvedColor
import com.blockchain.presentation.getResolvedDrawable
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.utils.consume
import com.blockchain.utils.unsafeLazy
import io.reactivex.rxjava3.disposables.CompositeDisposable
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.ActivityKycStatusBinding
import piuk.blockchain.android.ui.base.BaseMvpActivity
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity

class KycStatusActivity :
    BaseMvpActivity<KycStatusView, KycStatusPresenter>(),
    KycStatusView {

    private val binding: ActivityKycStatusBinding by lazy {
        ActivityKycStatusBinding.inflate(layoutInflater)
    }

    private val statusPresenter: KycStatusPresenter by scopedInject()
    private val campaignType by unsafeLazy { intent.getSerializableExtra(EXTRA_CAMPAIGN_TYPE) as CampaignType }
    private var progressDialog: MaterialProgressDialog? = null
    private val compositeDisposable = CompositeDisposable()

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        logEvent(AnalyticsEvents.KycComplete)

        val title = when (campaignType) {
            CampaignType.Swap -> com.blockchain.stringResources.R.string.kyc_splash_title
            CampaignType.SimpleBuy,
            CampaignType.Resubmission,
            CampaignType.FiatFunds,
            CampaignType.None,
            CampaignType.Interest -> com.blockchain.stringResources.R.string.identity_verification
        }
        updateToolbar(
            toolbarTitle = getString(title),
            backAction = { onBackPressedDispatcher.onBackPressed() }
        )
        onViewReady()
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    override fun startExchange() {
        startSwapFlow()
    }

    private fun startSwapFlow() =
        startActivity(
            TransactionFlowActivity.newIntent(
                context = this,
                action = AssetAction.Swap
            )
        )

    override fun renderUi(kycState: KycTierState) {
        when (kycState) {
            KycTierState.Pending -> onPending()
            KycTierState.UnderReview -> onInReview()
            KycTierState.Expired, KycTierState.Rejected -> onFailed()
            KycTierState.Verified -> onVerified()
            KycTierState.None -> throw IllegalStateException(
                "Users who haven't started KYC should not be able to access this page"
            )
        }
    }

    private fun onPending() {
        with(binding) {
            imageViewKycStatus.setImageDrawable(getResolvedDrawable(R.drawable.vector_in_progress))
            textViewVerificationSubtitle.visible()
            textViewVerificationState.setTextColor(getResolvedColor(com.blockchain.common.R.color.kyc_in_progress))
            textViewVerificationState.setText(com.blockchain.stringResources.R.string.kyc_status_title_in_progress)
            displayNotificationButton()
            val message = when (campaignType) {
                CampaignType.Swap,
                CampaignType.None,
                CampaignType.Resubmission -> com.blockchain.stringResources.R.string.kyc_status_message_in_progress
                CampaignType.SimpleBuy,
                CampaignType.FiatFunds,
                CampaignType.Interest -> com.blockchain.stringResources.R.string.sunriver_status_message
            }
            textViewVerificationMessage.setText(message)
        }
    }

    private fun onInReview() {
        with(binding) {
            imageViewKycStatus.setImageDrawable(getResolvedDrawable(R.drawable.vector_in_progress))
            textViewVerificationState.setTextColor(getResolvedColor(com.blockchain.common.R.color.kyc_in_progress))
            textViewVerificationState.setText(com.blockchain.stringResources.R.string.kyc_status_title_in_review)
            textViewVerificationMessage.setText(com.blockchain.stringResources.R.string.kyc_status_message_under_review)
            displayNotificationButton()
        }
    }

    private fun displayNotificationButton() {
        binding.buttonKycStatusNext.apply {
            setText(com.blockchain.stringResources.R.string.kyc_status_button_notify_me)
            setOnClickListener { presenter?.onClickNotifyUser() }
            visible()
        }
        binding.textViewKycStatusNoThanks.apply {
            visible()
            setOnClickListener { finish() }
        }
    }

    private fun onFailed() {
        with(binding) {
            imageViewKycStatus.setImageDrawable(getResolvedDrawable(R.drawable.vector_failed))
            textViewVerificationState.setTextColor(getResolvedColor(com.blockchain.common.R.color.product_red_medium))
            textViewVerificationState.setText(com.blockchain.stringResources.R.string.kyc_status_title_failed)
            textViewVerificationMessage.setText(com.blockchain.stringResources.R.string.kyc_status_message_failed)
            buttonKycStatusNext.gone()
        }
    }

    private fun onVerified() {
        with(binding) {
            imageViewKycStatus.setImageDrawable(getResolvedDrawable(R.drawable.vector_verified))
            textViewVerificationState.setTextColor(getResolvedColor(com.blockchain.common.R.color.kyc_progress_green))
            textViewVerificationState.setText(com.blockchain.stringResources.R.string.kyc_settings_status_verified)
            textViewVerificationMessage.setText(com.blockchain.stringResources.R.string.kyc_status_message_verified)
            buttonKycStatusNext.apply {
                setText(com.blockchain.stringResources.R.string.kyc_status_button_get_started)
                setOnClickListener { presenter?.onClickContinue() }
                ConstraintSet().apply {
                    clone(constraintLayoutKycStatus)
                    setMargin(
                        R.id.button_kyc_status_next,
                        ConstraintSet.BOTTOM,
                        32.px
                    )
                    applyTo(constraintLayoutKycStatus)
                }

                visible()
            }
        }
    }

    override fun showSnackbar(@StringRes message: Int) {
        BlockchainSnackbar.make(
            binding.root,
            getString(message),
            type = SnackbarType.Success
        ).show()
    }

    override fun showNotificationsEnabledDialog() {
        AlertDialog.Builder(this, com.blockchain.componentlib.R.style.AlertDialogStyle)
            .setTitle(com.blockchain.stringResources.R.string.kyc_status_button_notifications_enabled_title)
            .setMessage(com.blockchain.stringResources.R.string.kyc_status_button_notifications_enabled_message)
            .setPositiveButton(android.R.string.ok, null)
            .setOnDismissListener { finish() }
            .show()
    }

    override fun showProgressDialog() {
        progressDialog = MaterialProgressDialog(this).apply {
            setOnCancelListener { presenter?.onProgressCancelled() }
            setMessage(com.blockchain.stringResources.R.string.kyc_country_selection_please_wait)
            show()
        }
    }

    override fun finishPage() {
        BlockchainSnackbar.make(
            binding.root,
            getString(com.blockchain.stringResources.R.string.kyc_status_error),
            type = SnackbarType.Error
        ).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean = consume { finish() }

    override fun createPresenter(): KycStatusPresenter = statusPresenter

    override fun getView(): KycStatusView = this

    companion object {

        private const val EXTRA_CAMPAIGN_TYPE =
            "info.blockchain.wallet.ui.BalanceFragment.EXTRA_CAMPAIGN_TYPE"

        @JvmStatic
        fun start(context: Context, campaignType: CampaignType) {
            Intent(context, KycStatusActivity::class.java)
                .apply { putExtra(EXTRA_CAMPAIGN_TYPE, campaignType) }
                .run { context.startActivity(this) }
        }
    }
}
