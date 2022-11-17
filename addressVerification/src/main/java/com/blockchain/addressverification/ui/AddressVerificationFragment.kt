package com.blockchain.addressverification.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.platform.ComposeView
import com.blockchain.addressverification.R
import com.blockchain.commonarch.presentation.mvi_v2.MVIFragment
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.viewextensions.hideKeyboard
import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.common.model.StateIso
import com.blockchain.koin.payloadScope
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope

class AddressVerificationFragment :
    MVIFragment<AddressVerificationState>(),
    NavigationRouter<Navigation>,
    AndroidScopeComponent {

    interface Host {
        fun launchContactSupport()
        fun addressVerifiedSuccessfully(address: AddressDetails)
    }

    private val host: Host by lazy {
        (activity as? Host) ?: (parentFragment as? Host) ?: throw IllegalStateException(
            "Host is not a AddressVerificationFragment.Host"
        )
    }

    private lateinit var onBackPressedCallback: OnBackPressedCallback

    private val argCountryIso: CountryIso by lazy {
        arguments?.getString(ARG_COUNTRY_ISO)!!
    }

    private val argStateIso: StateIso? by lazy {
        arguments?.getString(ARG_STATE_ISO)
    }

    private val argPrefilledAddress: AddressDetails? by lazy {
        arguments?.getParcelable(ARG_PREFILLED_ADDRESS)
    }

    private val argAllowManualOverride: Boolean by lazy {
        arguments?.getBoolean(ARG_ALLOW_MANUAL_OVERRIDE) ?: false
    }

    override var scope: Scope? = payloadScope

    private val viewModel: AddressVerificationModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        bindViewModel(viewModel, this, Args(argCountryIso, argStateIso, argPrefilledAddress, argAllowManualOverride))

        onBackPressedCallback = requireActivity().onBackPressedDispatcher.addCallback(owner = this) {
            viewModel.onIntent(AddressVerificationIntent.BackClicked)
        }

        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    AddressVerificationScreen(viewState = viewModel.viewState, onIntent = viewModel::onIntent)
                }
            }
        }
    }

    override fun onStateUpdated(state: AddressVerificationState) {
        if (state.error != null) {
            requireActivity().hideKeyboard()
            when (state.error) {
                is AddressVerificationError.Unknown -> BlockchainSnackbar.make(
                    requireView(),
                    state.error.message ?: getString(R.string.common_error),
                    type = SnackbarType.Error
                ).show()
                AddressVerificationError.InvalidState -> AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
                    .setTitle(R.string.address_verification_invalid_state_title)
                    .setMessage(R.string.address_verification_invalid_state_message)
                    .setPositiveButton(R.string.contact_support) { _, _ ->
                        host.launchContactSupport()
                    }
                    .setNegativeButton(R.string.common_cancel) { _, _ ->
                        // no op
                    }
                    .show()
            }
            viewModel.onIntent(AddressVerificationIntent.ErrorHandled)
        }
    }

    fun errorWhileSaving(error: AddressVerificationSavingError) {
        viewModel.onIntent(AddressVerificationIntent.ErrorWhileSaving(error))
    }

    override fun route(navigationEvent: Navigation) {
        when (navigationEvent) {
            is Navigation.FinishSuccessfully -> host.addressVerifiedSuccessfully(navigationEvent.address)
            Navigation.Back -> {
                onBackPressedCallback.remove()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    companion object {
        private const val ARG_COUNTRY_ISO = "ARG_COUNTRY_ISO"
        private const val ARG_STATE_ISO = "ARG_STATE_ISO"
        private const val ARG_PREFILLED_ADDRESS = "ARG_PREFILLED_ADDRESS"
        private const val ARG_ALLOW_MANUAL_OVERRIDE = "ARG_ALLOW_MANUAL_OVERRIDE"

        fun newInstance(
            countryIso: CountryIso,
            stateIso: StateIso?,
            allowManualOverride: Boolean,
        ): AddressVerificationFragment = AddressVerificationFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_COUNTRY_ISO, countryIso)
                putString(ARG_STATE_ISO, stateIso)
                putBoolean(ARG_ALLOW_MANUAL_OVERRIDE, allowManualOverride)
            }
        }

        fun newInstanceEditMode(
            address: AddressDetails,
            allowManualOverride: Boolean,
        ): AddressVerificationFragment = AddressVerificationFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_COUNTRY_ISO, address.countryIso)
                putString(ARG_STATE_ISO, address.stateIso)
                putParcelable(ARG_PREFILLED_ADDRESS, address)
                putBoolean(ARG_ALLOW_MANUAL_OVERRIDE, allowManualOverride)
            }
        }
    }
}
