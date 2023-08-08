package piuk.blockchain.android.ui.kyc.mobile.entry

import android.os.Bundle
import android.telephony.PhoneNumberFormattingTextWatcher
import android.telephony.PhoneNumberUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.legacy.MaterialProgressDialog
import com.blockchain.componentlib.viewextensions.getTextString
import com.blockchain.core.settings.PhoneNumber
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.utils.unsafeLazy
import com.jakewharton.rxbinding4.widget.afterTextChangeEvents
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.Observables
import io.reactivex.rxjava3.kotlin.plusAssign
import java.util.Locale
import java.util.concurrent.TimeUnit
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentKycAddPhoneNumberBinding
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.android.ui.kyc.extensions.skipFirstUnless
import piuk.blockchain.android.ui.kyc.mobile.entry.models.PhoneDisplayModel
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener
import piuk.blockchain.android.ui.kyc.navigate
import piuk.blockchain.android.util.throttledClicks

class KycMobileEntryFragment :
    BaseFragment<KycMobileEntryView, KycMobileEntryPresenter>(),
    KycMobileEntryView {

    private var _binding: FragmentKycAddPhoneNumberBinding? = null
    private val binding: FragmentKycAddPhoneNumberBinding
        get() = _binding!!

    private val presenter: KycMobileEntryPresenter by scopedInject()
    private val progressListener: KycProgressListener by ParentActivityDelegate(
        this
    )
    private val compositeDisposable = CompositeDisposable()
    private val countryCode by unsafeLazy {
        KycMobileEntryFragmentArgs.fromBundle(
            arguments ?: Bundle()
        ).countryCode
    }
    private val phoneNumberObservable
        get() = binding.editTextKycMobileNumber.afterTextChangeEvents()
            .skipInitialValue()
            .debounce(300, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                val string = it.editable.toString()
                // Force plus sign even if user deletes it
                if (string.firstOrNull() != '+') {
                    binding.editTextKycMobileNumber.apply {
                        setText("+$string")
                        setSelection(getTextString().length)
                    }
                }
            }
            .map { PhoneNumber(binding.editTextKycMobileNumber.getTextString()) }

    override val uiStateObservable: Observable<Pair<PhoneNumber, Unit>>
        get() = Observables.combineLatest(
            phoneNumberObservable.cache(),
            binding.buttonKycPhoneNumberNext.throttledClicks()
        )

    private var progressDialog: MaterialProgressDialog? = null
    private val prefixGuess by unsafeLazy {
        "+" + PhoneNumberUtil.createInstance(context)
            .getCountryCodeForRegion(countryCode)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKycAddPhoneNumberBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressListener.setupHostToolbar(com.blockchain.stringResources.R.string.kyc_phone_number_title)

        with(binding.editTextKycMobileNumber) {
            addTextChangedListener(PhoneNumberFormattingTextWatcher())
            setOnFocusChangeListener { _, hasFocus ->
                binding.inputLayoutKycMobileNumber.hint = if (hasFocus) {
                    getString(com.blockchain.stringResources.R.string.kyc_phone_number_hint_focused)
                } else {
                    getString(com.blockchain.stringResources.R.string.kyc_phone_number_hint_unfocused)
                }

                // Insert our best guess for the device's dialling code
                if (hasFocus && getTextString().isEmpty()) {
                    setText(prefixGuess)
                }
            }
        }

        onViewReady()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()

        compositeDisposable +=
            binding.editTextKycMobileNumber
                .onDelayedChange()
                .subscribe()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun preFillPhoneNumber(phoneNumber: String) {
        val formattedNumber = PhoneNumberUtils.formatNumber(phoneNumber, Locale.getDefault().isO3Country)
        binding.editTextKycMobileNumber.setText(formattedNumber)
    }

    override fun showErrorSnackbar(@StringRes message: Int) {
        BlockchainSnackbar.make(
            binding.root,
            getString(message),
            type = SnackbarType.Error
        ).show()
    }

    override fun continueSignUp(displayModel: PhoneDisplayModel) {
        navigate(KycMobileEntryFragmentDirections.actionMobileCodeEntry(countryCode, displayModel))
    }

    override fun showProgressDialog() {
        progressDialog = MaterialProgressDialog(requireContext()).apply {
            setOnCancelListener { presenter.onProgressCancelled() }
            setMessage(com.blockchain.stringResources.R.string.kyc_country_selection_please_wait)
            show()
        }
    }

    override fun dismissProgressDialog() {
        progressDialog?.apply { dismiss() }
        progressDialog = null
    }

    private fun TextView.onDelayedChange(): Observable<Boolean> =
        this.afterTextChangeEvents()
            .debounce(300, TimeUnit.MILLISECONDS)
            .map { it.editable.toString() }
            .skipFirstUnless { it.isNotEmpty() }
            .observeOn(AndroidSchedulers.mainThread())
            .map { mapToCompleted(it) }
            .distinctUntilChanged()
            .doOnNext {
                binding.buttonKycPhoneNumberNext.isEnabled = it
            }

    private fun mapToCompleted(text: String): Boolean = PhoneNumber(text).isValid

    override fun createPresenter(): KycMobileEntryPresenter = presenter

    override fun getMvpView(): KycMobileEntryView = this
}
