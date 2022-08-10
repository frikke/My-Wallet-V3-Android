package piuk.blockchain.android.ui.kyc.veriffsplash

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.content.ContextCompat
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment.findNavController
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.data.logEvent
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.analytics.events.KYCAnalyticsEvents
import com.blockchain.coincore.AssetAction
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.legacy.MaterialProgressDialog
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.goneIf
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.Tier
import com.blockchain.nabu.models.responses.nabu.SupportedDocuments
import com.blockchain.veriff.VeriffApplicantAndToken
import com.blockchain.veriff.VeriffLauncher
import com.blockchain.veriff.VeriffResultHandler
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.FragmentKycVeriffSplashBinding
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener
import piuk.blockchain.android.ui.kyc.navhost.models.UiState
import piuk.blockchain.android.ui.kyc.navhost.models.UiState.CONTENT
import piuk.blockchain.android.ui.kyc.navhost.models.UiState.EMPTY
import piuk.blockchain.android.ui.kyc.navhost.models.UiState.FAILURE
import piuk.blockchain.android.ui.kyc.navhost.models.UiState.LOADING
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import piuk.blockchain.android.urllinks.URL_BLOCKCHAIN_GOLD_UNAVAILABLE_SUPPORT
import piuk.blockchain.android.urllinks.URL_BLOCKCHAIN_KYC_SUPPORTED_COUNTRIES_LIST
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.throttledClicks
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class VeriffSplashFragment :
    BaseFragment<VeriffSplashView, VeriffSplashPresenter>(),
    VeriffSplashView {

    private var _binding: FragmentKycVeriffSplashBinding? = null
    private val binding: FragmentKycVeriffSplashBinding
        get() = _binding!!

    private val presenter: VeriffSplashPresenter by scopedInject()
    private val stringUtils: StringUtils by inject()

    private val progressListener: KycProgressListener by ParentActivityDelegate(
        this
    )

    private val analytics: Analytics by inject()

    private val veriffResultHandler = VeriffResultHandler(
        onSuccess = {
            presenter.submitVerification()
        },
        onError = {
            analytics.logEvent(
                VeriffAnalytics.VerifSubmissionFailed(
                    tierUserIsAboutToUpgrade = Tier.GOLD,
                    failureReason = it
                )
            )
        }
    )

    override val countryCode by unsafeLazy {
        VeriffSplashFragmentArgs.fromBundle(arguments ?: Bundle()).countryCode
    }

    private val compositeDisposable = CompositeDisposable()
    private var progressDialog: MaterialProgressDialog? = null
    private var ctaClicked = false

    override val nextClick: Observable<Unit>
        get() = binding.btnNext.throttledClicks()

    override val swapClick: Observable<Unit>
        get() = binding.btnGotoSwap.throttledClicks()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKycVeriffSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logEvent(AnalyticsEvents.KycVerifyIdentity)
        analytics.logEvent(KYCAnalyticsEvents.MoreInfoViewed)

        checkCameraPermissions()
        setupTextLinks()
        onViewReady()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!ctaClicked) analytics.logEvent(KYCAnalyticsEvents.MoreInfoDismissed)
        _binding = null
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    private fun setupTextLinks() {

        val linksMap = mapOf<String, Uri>(
            "gold_error" to Uri.parse(URL_BLOCKCHAIN_GOLD_UNAVAILABLE_SUPPORT),
            "country_list" to Uri.parse(URL_BLOCKCHAIN_KYC_SUPPORTED_COUNTRIES_LIST)
        )

        // On the content view:
        val countriesText = stringUtils.getStringWithMappedAnnotations(
            R.string.kyc_veriff_splash_country_supported_subheader,
            linksMap,
            requireActivity()
        )

        with(binding) {
            textSupportedCountries.text = countriesText
            textSupportedCountries.movementMethod = LinkMovementMethod.getInstance()

            // On the error view:
            val supportText = stringUtils.getStringWithMappedAnnotations(
                R.string.kyc_gold_unavailable_text_support,
                linksMap,
                requireActivity()
            )
            textAction.text = supportText
            textAction.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun checkCameraPermissions() {
        val granted = ContextCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        with(binding) {
            textViewVeriffSplashEnableCameraTitle.goneIf(granted)
            textViewVeriffSplashEnableCameraBody.goneIf(granted)
        }
    }

    override fun showProgressDialog(cancelable: Boolean) {
        progressDialog = MaterialProgressDialog(requireContext()).apply {
            setOnCancelListener { presenter.onProgressCancelled() }
            setMessage(R.string.kyc_country_selection_please_wait)
            setCancelable(cancelable)
            show()
        }
    }

    override fun dismissProgressDialog() {
        progressDialog?.apply { dismiss() }
        progressDialog = null
    }

    @SuppressLint("InflateParams")
    override fun continueToVeriff(applicant: VeriffApplicantAndToken) {
        launchVeriff(applicant)
        ctaClicked = true
        analytics.logEvent(KYCAnalyticsEvents.MoreInfoCtaClicked)
        logEvent(KYCAnalyticsEvents.VeriffInfoStarted)
    }

    private fun launchVeriff(applicant: VeriffApplicantAndToken) {
        VeriffLauncher().launchVeriff(requireActivity(), applicant, REQUEST_CODE_VERIFF)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_VERIFF) {
            data?.let {
                veriffResultHandler.handleResult(it)
            } ?: run {
                if (resultCode == RESULT_OK) {
                    presenter.submitVerification()
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun showError(@StringRes message: Int) =
        BlockchainSnackbar.make(
            binding.root,
            getString(message),
            type = SnackbarType.Error
        ).show()

    override fun continueToCompletion() {
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.kyc_nav_xml, true)
            .build()
        findNavController(this).navigate(R.id.applicationCompleteFragment, null, navOptions)
    }

    override fun continueToSwap() =
        startActivity(
            TransactionFlowActivity.newIntent(
                context = requireActivity(),
                action = AssetAction.Swap
            )
        )

    override fun createPresenter(): VeriffSplashPresenter = presenter

    override fun getMvpView(): VeriffSplashView = this

    override fun supportedDocuments(documents: List<SupportedDocuments>) {
        val makeVisible = { id: Int -> view?.findViewById<View>(id)?.visible() }
        documents.forEach {
            when (it) {
                SupportedDocuments.PASSPORT -> makeVisible(R.id.text_view_document_passport)
                SupportedDocuments.DRIVING_LICENCE -> makeVisible(R.id.text_view_document_drivers_license)
                SupportedDocuments.NATIONAL_IDENTITY_CARD -> makeVisible(R.id.text_view_document_id_card)
                SupportedDocuments.RESIDENCE_PERMIT -> makeVisible(R.id.text_view_document_residence_permit)
            }
        }
    }

    override fun setUiState(@UiState.UiStateDef state: Int) {
        when (state) {
            LOADING -> showLoadingState()
            CONTENT -> showContentState()
            FAILURE -> showErrorState()
            EMPTY -> showEmptyState()
        }
    }

    private fun showLoadingState() {
        showProgressDialog(cancelable = false)
        with(binding) {
            errorLayout.gone()
            contentView.gone()
            loadingView.visible()
        }
    }

    private fun showContentState() {
        dismissProgressDialog()
        progressListener.setupHostToolbar(R.string.kyc_veriff_splash_title)
        with(binding) {
            errorLayout.gone()
            contentView.visible()
            loadingView.gone()
        }
    }

    private fun showErrorState() {
        dismissProgressDialog()
        progressListener.setupHostToolbar(R.string.kyc_veriff_splash_error_silver)
        with(binding) {
            errorLayout.visible()
            contentView.gone()
            loadingView.gone()
            btnGotoSwap.visibleIf {
                progressListener.campaignType != CampaignType.SimpleBuy
            }
        }
    }

    private fun showEmptyState() {
        throw IllegalStateException("UiState == EMPTY. This should never happen")
    }

    companion object {
        private const val REQUEST_CODE_VERIFF = 1440
    }
}
