package piuk.blockchain.android.ui.kyc.address

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment.findNavController
import com.blockchain.commonarch.presentation.base.MaterialProgressDialog
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.hideKeyboard
import com.blockchain.extensions.nextAfterOrNull
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.KYCAnalyticsEvents
import com.blockchain.notifications.analytics.logEvent
import com.jakewharton.rx3.replayingShare
import com.jakewharton.rxbinding4.widget.afterTextChangeEvents
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.Locale
import java.util.concurrent.TimeUnit
import org.koin.android.ext.android.inject
import piuk.blockchain.android.KycNavXmlDirections
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentKycHomeAddressBinding
import piuk.blockchain.android.ui.base.BaseMvpFragment
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.toast
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.android.ui.kyc.address.models.AddressDialog
import piuk.blockchain.android.ui.kyc.address.models.AddressIntent
import piuk.blockchain.android.ui.kyc.address.models.AddressModel
import piuk.blockchain.android.ui.kyc.extensions.skipFirstUnless
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener
import piuk.blockchain.android.ui.kyc.navhost.models.KycStep
import piuk.blockchain.android.ui.kyc.navigate
import piuk.blockchain.android.ui.kyc.profile.models.ProfileModel
import piuk.blockchain.android.util.AfterTextChangedWatcher
import piuk.blockchain.android.util.throttledClicks
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber

class KycHomeAddressFragment :
    BaseMvpFragment<KycHomeAddressView, KycHomeAddressPresenter>(),
    KycHomeAddressView {

    private var _binding: FragmentKycHomeAddressBinding? = null
    private val binding: FragmentKycHomeAddressBinding
        get() = _binding!!

    private val presenter: KycHomeAddressPresenter by scopedInject()
    private val analytics: Analytics by inject()
    private val progressListener: KycProgressListener by ParentActivityDelegate(
        this
    )
    private val compositeDisposable = CompositeDisposable()
    private var progressDialog: MaterialProgressDialog? = null
    override val profileModel: ProfileModel by unsafeLazy {
        KycHomeAddressFragmentArgs.fromBundle(arguments ?: Bundle()).profileModel
    }
    private val initialState by unsafeLazy {
        AddressModel(
            profileModel.addressDetails?.address.orEmpty(),
            null,
            profileModel.addressDetails?.locality.orEmpty(),
            profileModel.stateCode.orEmpty(),
            profileModel.addressDetails?.postalCode.orEmpty(),
            profileModel.countryCode
        )
    }
    private val addressSubject = PublishSubject.create<AddressIntent>()
    override val address: Observable<AddressModel> by unsafeLazy {
        AddressDialog(addressSubject, initialState).viewModel
            .replayingShare()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKycHomeAddressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logEvent(AnalyticsEvents.KycAddress)
        progressListener.setHostTitle(R.string.kyc_address_title)
        binding.editTextKycAddressZipCode.addTextChangedListener(textWatcher)

        setupImeOptions()
        localiseUi()

        onViewReady()
    }

    private val textWatcher = object : AfterTextChangedWatcher() {
        override fun afterTextChanged(s: Editable?) {
            binding.inputLayoutKycAddressZipCode.error = null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @Suppress("ConstantConditionIf")
    override fun continueToVeriffSplash(countryCode: String) {
        closeKeyboard()
        navigate(KycNavXmlDirections.actionStartVeriff(countryCode))
    }

    override fun tier1Complete() {
        closeKeyboard()
        activity?.setResult(KycNavHostActivity.RESULT_KYC_FOR_TIER_COMPLETE)
        activity?.finish()
    }

    override fun onSddVerified() {
        activity?.setResult(KycNavHostActivity.RESULT_KYC_FOR_SDD_COMPLETE)
        activity?.finish()
    }

    override fun continueToTier2MoreInfoNeeded(countryCode: String) {
        closeKeyboard()
        navigate(KycNavXmlDirections.actionStartTier2NeedMoreInfo(countryCode))
    }

    override fun restoreUiState(
        line1: String,
        line2: String?,
        city: String,
        state: String?,
        postCode: String,
        countryName: String
    ) {
        with(binding) {
            editTextKycAddressFirstLine.setText(line1)
            editTextKycAddressAptName.setText(line2)
            editTextKycAddressCity.setText(city)
            editTextKycAddressState.setText(state)
            editTextKycAddressZipCode.setText(postCode)
            editTextKycAddressCountry.setText(countryName)
        }
    }

    override fun onResume() {
        super.onResume()
        subscribeToViewObservables()
        analytics.logEvent(KYCAnalyticsEvents.AddressScreenSeen)
    }

    private fun subscribeToViewObservables() {
        if (compositeDisposable.size() == 0) {
            with(binding) {
                compositeDisposable += buttonKycAddressNext
                    .throttledClicks()
                    .subscribeBy(
                        onNext = {
                            presenter.onContinueClicked(progressListener.campaignType)
                            analytics.logEvent(KYCAnalyticsEvents.AddressChanged)
                        },
                        onError = { Timber.e(it) }
                    )

                compositeDisposable += editTextKycAddressFirstLine
                    .onDelayedChange(KycStep.AddressFirstLine)
                    .doOnNext { addressSubject.onNext(AddressIntent.FirstLine(it)) }
                    .subscribe()
                compositeDisposable += editTextKycAddressAptName
                    .onDelayedChange(KycStep.AptNameOrNumber)
                    .doOnNext { addressSubject.onNext(AddressIntent.SecondLine(it)) }
                    .subscribe()
                compositeDisposable += editTextKycAddressCity
                    .onDelayedChange(KycStep.City)
                    .doOnNext { addressSubject.onNext(AddressIntent.City(it)) }
                    .subscribe()

                compositeDisposable += editTextKycAddressZipCode
                    .onDelayedChange(KycStep.ZipCode)
                    .doOnNext { addressSubject.onNext(AddressIntent.PostCode(it)) }
                    .subscribe()

                addressSubject.onNext(AddressIntent.State(profileModel.stateCode ?: ""))

                compositeDisposable += editTextKycAddressState
                    .onDelayedChange(KycStep.State)
                    .filter { !profileModel.isInUs() }
                    .doOnNext { addressSubject.onNext(AddressIntent.State(it)) }
                    .subscribe()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun finishPage() {
        findNavController(this).popBackStack()
    }

    override fun setButtonEnabled(enabled: Boolean) {
        binding.buttonKycAddressNext.isEnabled = enabled
    }

    override fun showErrorToast(message: Int) {
        toast(message, ToastCustom.TYPE_ERROR)
    }

    override fun showInvalidPostcode() {
        binding.apply {
            inputLayoutKycAddressZipCode.error = getString(R.string.kyc_postcode_error)
        }
    }

    override fun showProgressDialog() {
        progressDialog = MaterialProgressDialog(requireContext()).apply {
            setOnCancelListener { presenter.onProgressCancelled() }
            setMessage(R.string.kyc_country_selection_please_wait)
            show()
        }
    }

    override fun dismissProgressDialog() {
        progressDialog?.apply { dismiss() }
        progressDialog = null
    }

    private fun ProfileModel.isInUs() =
        countryCode.equals("US", ignoreCase = true)

    private fun localiseUi() {
        with(binding) {
            if (profileModel.isInUs()) {
                inputLayoutKycAddressFirstLine.hint = getString(R.string.kyc_address_address_line_1)
                inputLayoutKycAddressAptName.hint = getString(R.string.kyc_address_address_line_2)
                inputLayoutKycAddressCity.hint = getString(R.string.kyc_address_address_city_hint)
                inputLayoutKycAddressState.hint = getString(R.string.kyc_address_address_state_hint)
                inputLayoutKycAddressZipCode.hint = getString(R.string.kyc_address_address_zip_code_hint_1)
                inputLayoutKycAddressState.editText?.isEnabled = false
            } else {
                inputLayoutKycAddressFirstLine.hint = getString(R.string.kyc_address_address_line_1)
                inputLayoutKycAddressAptName.hint = getString(R.string.kyc_address_address_line_2)
                inputLayoutKycAddressCity.hint = getString(R.string.address_city)
                inputLayoutKycAddressState.gone()
                inputLayoutKycAddressZipCode.hint = getString(R.string.kyc_address_postal_code)
                inputLayoutKycAddressState.editText?.isEnabled = true
            }

            editTextKycAddressCountry.setText(
                Locale(
                    Locale.getDefault().displayLanguage,
                    profileModel.countryCode
                ).displayCountry
            )

            editTextKycAddressState.setText(
                profileModel.stateName ?: ""
            )

            editTextKycAddressFirstLine.setText(initialState.firstLine)
            editTextKycAddressCity.setText(initialState.city)
            editTextKycAddressZipCode.setText(initialState.postCode)
        }
    }

    private fun TextView.onDelayedChange(kycStep: KycStep): Observable<String> =
        this.afterTextChangeEvents()
            .debounce(300, TimeUnit.MILLISECONDS)
            .map { it.editable.toString() }
            .skipFirstUnless { !it.isEmpty() }
            .observeOn(AndroidSchedulers.mainThread())
            .distinctUntilChanged()

    private fun setupImeOptions() {
        with(binding) {
            val editTexts = listOf(
                editTextKycAddressFirstLine,
                editTextKycAddressAptName,
                editTextKycAddressCity,
                editTextKycAddressState,
                editTextKycAddressZipCode
            )

            editTexts.forEach { editText ->
                editText.setOnEditorActionListener { _, i, _ ->
                    consume {
                        when (i) {
                            EditorInfo.IME_ACTION_NEXT ->
                                editTexts.nextAfterOrNull { it === editText }?.requestFocus()
                            EditorInfo.IME_ACTION_DONE ->
                                closeKeyboard()
                        }
                    }
                }
            }
        }
    }

    private fun closeKeyboard() {
        (requireActivity() as? AppCompatActivity)?.let {
            it.hideKeyboard()
        }
    }

    override fun createPresenter(): KycHomeAddressPresenter = presenter

    override fun getMvpView(): KycHomeAddressView = this
}
