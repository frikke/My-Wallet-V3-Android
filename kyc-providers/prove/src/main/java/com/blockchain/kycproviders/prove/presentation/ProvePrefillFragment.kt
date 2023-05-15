package com.blockchain.kycproviders.prove.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import com.blockchain.commonarch.presentation.mvi_v2.MVIFragment
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.componentlib.viewextensions.hideKeyboard
import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.common.model.StateIso
import com.blockchain.koin.payloadScope
import com.blockchain.kycproviders.prove.R
import com.blockchain.nabu.models.responses.nabu.KycState
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import java.util.Calendar
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope

class ProvePrefillFragment :
    MVIFragment<ProvePrefillViewState>(),
    NavigationRouter<Navigation>,
    AndroidScopeComponent {

    interface Host {
        fun launchContactSupport()
        fun navigateToProfileInfo(countryIso: CountryIso, stateIso: StateIso?)
        fun navigateToTierStatus(kycState: KycState)
        fun navigateToVeriff(countryIso: CountryIso)
    }

    override val scope: Scope = payloadScope

    private val model: ProvePrefillModel by viewModel()

    private val host: Host by lazy {
        parentFragment as? Host ?: requireActivity() as Host
    }

    private val countryIso: CountryIso by lazy {
        requireArguments().getString(ARG_COUNTRY_ISO)!!
    }
    private val stateIso: StateIso? by lazy {
        requireArguments().getString(ARG_STATE_ISO)
    }

    private lateinit var backPressCallback: OnBackPressedCallback

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        ComposeView(requireContext()).apply {
            setContent {
                ProvePrefillScreen(
                    countryIso = countryIso,
                    stateIso = stateIso,
                    showDatePicker = ::showDatePicker,
                    launchContactSupport = host::launchContactSupport
                )
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backPressCallback = requireActivity().onBackPressedDispatcher.addCallback(owner = viewLifecycleOwner) {
            model.onIntent(ProvePrefillIntent.BackClicked)
        }
        bindViewModel(model, this, Args(countryIso, stateIso))
    }

    override fun onStateUpdated(state: ProvePrefillViewState) {
    }

    override fun route(navigationEvent: Navigation) {
        when (navigationEvent) {
            Navigation.Back -> {
                AlertDialog.Builder(requireContext(), com.blockchain.componentlib.R.style.AlertDialogStyle)
                    .setMessage(com.blockchain.stringResources.R.string.prove_leave_warning)
                    .setCancelable(true)
                    .setNegativeButton(com.blockchain.stringResources.R.string.common_cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton(com.blockchain.stringResources.R.string.prove_leave_button) { dialog, _ ->
                        backPressCallback.remove()
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                        dialog.dismiss()
                    }
                    .show()
            }
            Navigation.ExitToProfileInfo -> host.navigateToProfileInfo(countryIso, stateIso)
            is Navigation.ExitToTierStatus -> host.navigateToTierStatus(navigationEvent.kycState)
            Navigation.ExitToVeriff -> host.navigateToVeriff(countryIso)
        }
    }

    private fun showDatePicker() {
        (requireActivity() as? AppCompatActivity)?.hideKeyboard()

        val calendar = Calendar.getInstance().apply { add(Calendar.YEAR, -18) }
        DatePickerDialog.newInstance(
            datePickerCallback,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle(requireContext().getString(com.blockchain.stringResources.R.string.kyc_profile_dob_hint))
            maxDate = calendar
            showYearPickerFirst(true)
            show(requireActivity().fragmentManager, tag)
        }
    }

    private val datePickerCallback: DatePickerDialog.OnDateSetListener by lazy {
        DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            val dateOfBirth = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            model.onIntent(ProvePrefillIntent.DobInputChanged(dateOfBirth))
        }
    }

    companion object {
        private const val ARG_COUNTRY_ISO = "countryIso"
        private const val ARG_STATE_ISO = "stateIso"
    }
}
