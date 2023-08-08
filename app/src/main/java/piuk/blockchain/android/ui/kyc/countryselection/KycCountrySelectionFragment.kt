package piuk.blockchain.android.ui.kyc.countryselection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.data.logEvent
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.analytics.events.KYCAnalyticsEvents
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.legacy.MaterialProgressDialog
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.utils.unsafeLazy
import com.jakewharton.rxbinding4.appcompat.queryTextChanges
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.subjects.ReplaySubject
import java.util.concurrent.TimeUnit
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentKycCountrySelectionBinding
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.android.ui.kyc.countryselection.adapter.CountryCodeAdapter
import piuk.blockchain.android.ui.kyc.countryselection.models.CountrySelectionState
import piuk.blockchain.android.ui.kyc.countryselection.util.CountryDisplayModel
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener
import piuk.blockchain.android.ui.kyc.navigate
import piuk.blockchain.android.ui.kyc.search.filterCountries

internal class KycCountrySelectionFragment :
    BaseFragment<KycCountrySelectionView, KycCountrySelectionPresenter>(), KycCountrySelectionView {

    private var _binding: FragmentKycCountrySelectionBinding? = null
    private val binding: FragmentKycCountrySelectionBinding
        get() = _binding!!

    override val regionType by unsafeLazy {
        arguments?.getSerializable(ARGUMENT_STATE_OR_COUNTRY) as? RegionType ?: RegionType.Country
    }

    private val presenter: KycCountrySelectionPresenter by scopedInject()
    private val analytics: Analytics by inject()
    private val progressListener: KycProgressListener by ParentActivityDelegate(
        this
    )
    private val countryCodeAdapter = CountryCodeAdapter {
        presenter.onRegionSelected(it)
    }
    private var countryList = ReplaySubject.create<List<CountryDisplayModel>>(1)
    private var progressDialog: MaterialProgressDialog? = null
    private val compositeDisposable = CompositeDisposable()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKycCountrySelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.countrySelection.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter = countryCodeAdapter
        }

        when (regionType) {
            RegionType.Country -> {
                logEvent(AnalyticsEvents.KycCountry)
                progressListener.setupHostToolbar(com.blockchain.stringResources.R.string.kyc_country_selection_title_1)
            }
            RegionType.State -> {
                logEvent(AnalyticsEvents.KycStates)
                progressListener.setupHostToolbar(
                    com.blockchain.stringResources.R.string.kyc_country_selection_state_title
                )
            }
        }
        onViewReady()
    }

    override fun onResume() {
        super.onResume()

        compositeDisposable += countryList
            .filterCountries(
                binding.searchView.queryTextChanges().debounce(100, TimeUnit.MILLISECONDS)
            )
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                countryCodeAdapter.items = it
                binding.countrySelection.scrollToPosition(0)
            }
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun continueFlow(countryCode: String, stateCode: String?) {
        analytics.logEvent(KYCAnalyticsEvents.CountrySelected)
        navigate(
            KycCountrySelectionFragmentDirections.actionKycCountrySelectionFragmentToKycProfileFragment(
                countryCode,
                stateCode ?: "",
                stateCode ?: ""
            )
        )
    }

    override fun invalidCountry(displayModel: CountryDisplayModel) {
        navigate(
            KycCountrySelectionFragmentDirections.actionKycCountrySelectionFragmentToKycInvalidCountryFragment(
                displayModel
            )
        )
    }

    override fun requiresStateSelection() {
        val args = bundleArgs(RegionType.State)
        findNavController(this).navigate(R.id.kycCountrySelectionFragment, args)
    }

    override fun renderUiState(state: CountrySelectionState) {
        when (state) {
            CountrySelectionState.Loading -> showProgress()
            is CountrySelectionState.Error -> showErrorSnackbar(state.errorMessage)
            is CountrySelectionState.Data -> renderCountriesList(state)
        }
    }

    private fun renderCountriesList(state: CountrySelectionState.Data) {
        countryList.onNext(state.countriesList)
        hideProgress()
    }

    private fun showErrorSnackbar(@StringRes errorMessage: Int) {
        hideProgress()
        BlockchainSnackbar.make(
            binding.root,
            getString(errorMessage),
            type = SnackbarType.Error
        ).show()
    }

    private fun showProgress() {
        progressDialog = MaterialProgressDialog(
            requireContext()
        ).apply {
            setMessage(com.blockchain.stringResources.R.string.kyc_country_selection_please_wait)
            setOnCancelListener { presenter.onRequestCancelled() }
            show()
        }
    }

    private fun hideProgress() {
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog?.dismiss()
        }
    }

    override fun createPresenter(): KycCountrySelectionPresenter = presenter

    override fun getMvpView(): KycCountrySelectionView = this

    companion object {

        private const val ARGUMENT_STATE_OR_COUNTRY = "ARGUMENT_STATE_OR_COUNTRY"

        internal fun bundleArgs(regionType: RegionType): Bundle = Bundle().apply {
            putSerializable(ARGUMENT_STATE_OR_COUNTRY, regionType)
        }
    }
}

internal enum class RegionType {
    Country,
    State
}
