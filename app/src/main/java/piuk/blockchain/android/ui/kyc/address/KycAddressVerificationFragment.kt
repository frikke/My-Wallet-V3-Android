package piuk.blockchain.android.ui.kyc.address

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.addressverification.ui.AddressDetails
import com.blockchain.addressverification.ui.AddressVerificationFragment
import com.blockchain.addressverification.ui.AddressVerificationSavingError
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.data.logEvent
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.analytics.events.KYCAnalyticsEvents
import com.blockchain.componentlib.legacy.MaterialProgressDialog
import com.blockchain.componentlib.viewextensions.hideKeyboard
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.koin.scopedInject
import org.koin.android.ext.android.inject
import piuk.blockchain.android.KycNavXmlDirections
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewFragmentContainerBinding
import piuk.blockchain.android.support.SupportCentreActivity
import piuk.blockchain.android.ui.base.BaseMvpFragment
import piuk.blockchain.android.ui.cowboys.CowboysAnalytics
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener
import piuk.blockchain.android.ui.kyc.navigate
import piuk.blockchain.android.ui.kyc.profile.models.ProfileModel
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class KycAddressVerificationFragment :
    BaseMvpFragment<KycHomeAddressView, KycHomeAddressPresenter>(),
    KycHomeAddressView,
    AddressVerificationFragment.Host {

    override val profileModel: ProfileModel by unsafeLazy {
        KycAddressVerificationFragmentArgs.fromBundle(arguments ?: Bundle()).profileModel
    }

    private val presenter: KycHomeAddressPresenter by scopedInject()
    private val analytics: Analytics by inject()

    private val progressListener: KycProgressListener by ParentActivityDelegate(this)
    private var progressDialog: MaterialProgressDialog? = null

    private lateinit var binding: ViewFragmentContainerBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = ViewFragmentContainerBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val addressVerificationFragment: AddressVerificationFragment?
        get() = childFragmentManager.findFragmentById(R.id.fragment_container) as? AddressVerificationFragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        logEvent(AnalyticsEvents.KycAddress)
        progressListener.setupHostToolbar(R.string.kyc_address_title)
        if (addressVerificationFragment == null) {
            childFragmentManager.beginTransaction()
                .add(
                    R.id.fragment_container,
                    AddressVerificationFragment.newInstance(
                        profileModel.countryCode, profileModel.stateCode, allowManualOverride = true
                    )
                ).commitAllowingStateLoss()
        }
    }

    override fun launchContactSupport() {
        startActivity(SupportCentreActivity.newIntent(requireContext()))
    }

    override fun addressVerifiedSuccessfully(address: AddressDetails) {
        if ((requireActivity() as? KycNavHostActivity)?.isCowboysUser == true) {
            analytics.logEvent(CowboysAnalytics.KycAddressConfirmed)
        }

        presenter.onContinueClicked(progressListener.campaignType, address)
        analytics.logEvent(KYCAnalyticsEvents.AddressChanged)
    }

    override fun showErrorWhileSaving(error: AddressVerificationSavingError) {
        addressVerificationFragment?.errorWhileSaving(error)
    }

    override fun dismissProgressDialog() {
        progressDialog?.apply { dismiss() }
        progressDialog = null
    }

    override fun showProgressDialog() {
        progressDialog = MaterialProgressDialog(requireContext()).apply {
            setOnCancelListener { presenter.onProgressCancelled() }
            setMessage(R.string.kyc_country_selection_please_wait)
            show()
        }
    }

    override fun continueToVeriffSplash(countryCode: String) {
        requireActivity().hideKeyboard()
        navigate(KycNavXmlDirections.actionStartVeriff(countryCode))
    }

    override fun continueToTier2MoreInfoNeeded(countryCode: String) {
        requireActivity().hideKeyboard()
        navigate(KycNavXmlDirections.actionStartTier2NeedMoreInfo(countryCode))
    }

    override fun continueToQuestionnaire(questionnaire: Questionnaire, countryCode: String) {
        requireActivity().hideKeyboard()
        navigate(
            KycAddressVerificationFragmentDirections.actionKycAddressVerificationFragmentToKycQuestionnaireFragment(
                questionnaire,
                countryCode
            )
        )
    }

    override fun tier1Complete() {
        requireActivity().hideKeyboard()
        activity?.setResult(KycNavHostActivity.RESULT_KYC_FOR_TIER_COMPLETE)
        activity?.finish()
    }

    override fun onSddVerified() {
        activity?.setResult(KycNavHostActivity.RESULT_KYC_FOR_SDD_COMPLETE)
        activity?.finish()
    }

    override fun createPresenter(): KycHomeAddressPresenter = presenter

    override fun getMvpView(): KycHomeAddressView = this
}
