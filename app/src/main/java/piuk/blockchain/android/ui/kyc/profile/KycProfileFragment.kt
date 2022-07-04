package piuk.blockchain.android.ui.kyc.profile

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.data.logEvent
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.analytics.events.KYCAnalyticsEvents
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.legacy.MaterialProgressDialog
import com.blockchain.componentlib.viewextensions.getTextString
import com.blockchain.componentlib.viewextensions.hideKeyboard
import com.blockchain.koin.scopedInject
import com.jakewharton.rxbinding4.widget.afterTextChangeEvents
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.concurrent.TimeUnit
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentKycProfileBinding
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.android.ui.kyc.extensions.skipFirstUnless
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener
import piuk.blockchain.android.ui.kyc.navhost.models.KycStep
import piuk.blockchain.android.ui.kyc.navigate
import piuk.blockchain.android.ui.kyc.profile.models.ProfileModel
import piuk.blockchain.android.util.throttledClicks
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import timber.log.Timber

class KycProfileFragment : BaseFragment<KycProfileView, KycProfilePresenter>(), KycProfileView {

    private var _binding: FragmentKycProfileBinding? = null
    private val binding: FragmentKycProfileBinding
        get() = _binding!!

    private val presenter: KycProfilePresenter by scopedInject()
    private val analytics: Analytics by inject()

    private val progressListener: KycProgressListener by ParentActivityDelegate(
        this
    )
    private val compositeDisposable = CompositeDisposable()
    override val firstName: String
        get() = binding.editTextKycFirstName.getTextString()
    override val lastName: String
        get() = binding.editTextKycLastName.getTextString()
    override val countryCode: String by lazy {
        KycProfileFragmentArgs.fromBundle(
            arguments ?: Bundle()
        ).countryCode
    }

    override val stateCode: String?
        get() = KycProfileFragmentArgs.fromBundle(
            arguments ?: Bundle()
        ).stateCode.takeIf { it.isNotEmpty() }

    override val stateName: String?
        get() = KycProfileFragmentArgs.fromBundle(
            arguments ?: Bundle()
        ).stateName.takeIf { it.isNotEmpty() }

    override var dateOfBirth: Calendar? = null
    private var progressDialog: MaterialProgressDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKycProfileBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logEvent(AnalyticsEvents.KycProfile)

        progressListener.setupHostToolbar(R.string.kyc_profile_title)

        with(binding) {
            editTextKycFirstName.setOnEditorActionListener { _, i, _ ->
                consume { if (i == EditorInfo.IME_ACTION_NEXT) binding.editTextKycLastName.requestFocus() }
            }

            editTextKycLastName.setOnEditorActionListener { _, i, _ ->
                consume {
                    if (i == EditorInfo.IME_ACTION_NEXT) {
                        binding.editTextKycLastName.clearFocus()
                        binding.inputLayoutKycDateOfBirth.performClick()
                    }
                }
            }

            inputLayoutKycDateOfBirth.setOnClickListener { onDateOfBirthClicked() }
            editTextDateOfBirth.setOnClickListener { onDateOfBirthClicked() }
        }
        onViewReady()
    }

    override fun onResume() {
        super.onResume()
        compositeDisposable += binding.buttonKycProfileNext
            .throttledClicks()
            .subscribeBy(
                onNext = {
                    presenter.onContinueClicked(progressListener.campaignType)
                    analytics.logEvent(
                        KYCAnalyticsEvents.PersonalDetailsSet(
                            "${binding.editTextKycFirstName.text}," +
                                "${binding.editTextKycLastName.text}," +
                                "${binding.editTextDateOfBirth.text}"
                        )
                    )
                },
                onError = { Timber.e(it) }
            )

        compositeDisposable += binding.editTextKycFirstName
            .onDelayedChange(KycStep.FirstName) { presenter.firstNameSet = it }
            .subscribe()

        compositeDisposable += binding.editTextKycLastName
            .onDelayedChange(KycStep.LastName) { presenter.lastNameSet = it }
            .subscribe()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun continueSignUp(profileModel: ProfileModel) {
        navigate(
            KycProfileFragmentDirections.actionKycProfileFragmentToKycAutocompleteAddressFragment(
                profileModel
            )
        )
    }

    override fun showErrorSnackbar(message: String) {
        BlockchainSnackbar.make(
            binding.root,
            message,
            type = SnackbarType.Error
        ).show()
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

    override fun restoreUiState(
        firstName: String,
        lastName: String,
        displayDob: String,
        dobCalendar: Calendar
    ) {
        with(binding) {
            editTextKycFirstName.setText(firstName)
            editTextKycLastName.setText(lastName)
            editTextDateOfBirth.setText(displayDob)
        }
        dateOfBirth = dobCalendar
        presenter.dateSet = true
    }

    private fun TextView.onDelayedChange(
        kycStep: KycStep,
        presenterPropAssignment: (Boolean) -> Unit
    ): Observable<Boolean> =
        this.afterTextChangeEvents()
            .debounce(300, TimeUnit.MILLISECONDS)
            .map { it.editable.toString() }
            .skipFirstUnless { it.isNotEmpty() && it.length >= MIN_LENGTH_ALLOWED }
            .observeOn(AndroidSchedulers.mainThread())
            .map { mapToCompleted(it) }
            .doOnNext(presenterPropAssignment)
            .distinctUntilChanged()

    private fun onDateOfBirthClicked() {
        (requireActivity() as? AppCompatActivity)?.let {
            it.hideKeyboard()
        }

        val calendar = Calendar.getInstance().apply { add(Calendar.YEAR, -18) }
        DatePickerDialog.newInstance(
            datePickerCallback,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            maxDate = calendar
            showYearPickerFirst(true)
            show(requireActivity().fragmentManager, tag)
        }
    }

    private val datePickerCallback: DatePickerDialog.OnDateSetListener
        @SuppressLint("SimpleDateFormat")
        get() = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->

            presenter.dateSet = true
            dateOfBirth = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }.also {
                val format = SimpleDateFormat("MMMM dd, yyyy")
                val dateString = format.format(it.time)
                binding.editTextDateOfBirth.setText(dateString)
            }
        }

    private fun mapToCompleted(text: String): Boolean = text.isNotEmpty() && text.length >= MIN_LENGTH_ALLOWED

    override fun setButtonEnabled(enabled: Boolean) {
        binding.buttonKycProfileNext.isEnabled = enabled
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun createPresenter(): KycProfilePresenter = presenter

    override fun getMvpView(): KycProfileView = this

    companion object {
        private const val MIN_LENGTH_ALLOWED = 2
    }
}
