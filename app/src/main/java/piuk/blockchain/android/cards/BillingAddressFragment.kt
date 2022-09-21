package piuk.blockchain.android.cards

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.addressverification.ui.USState
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.domain.paymentmethods.model.BillingAddress
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.models.responses.nabu.NabuUser
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.util.Locale
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentBillingAddressBinding
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.util.AfterTextChangedWatcher

class BillingAddressFragment :
    MviFragment<CardModel, CardIntent, CardState, FragmentBillingAddressBinding>(),
    PickerItemListener,
    AddCardFlowFragment,
    SlidingModalBottomDialog.Host {

    private var usSelected = false
    private val userService: UserService by scopedInject()

    private val compositeDisposable = CompositeDisposable()

    override val cardDetailsPersistence: CardDetailsPersistence
        get() = (activity as? CardDetailsPersistence)
            ?: throw IllegalStateException("Parent must implement CardDetailsPersistence")

    private var countryPickerItem: CountryPickerItem? = null
    private var statePickerItem: StatePickerItem? = null

    private val textWatcher = object : AfterTextChangedWatcher() {
        override fun afterTextChanged(s: Editable?) {
            binding.btnNext.isEnabled = addressIsValid()
        }
    }

    private fun addressIsValid(): Boolean =
        binding.fullName.text.isNullOrBlank().not() &&
            binding.addressLine1.text.isNullOrBlank().not() &&
            binding.city.text.isNullOrBlank().not() &&
            (
                if (usSelected) binding.zipUsa.text.isNullOrBlank().not() && binding.state.text.isNullOrBlank().not()
                else binding.postcode.text.isNullOrBlank().not()
                )

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentBillingAddressBinding =
        FragmentBillingAddressBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            billingHeader.setOnClickListener {
                showBottomSheet(
                    SearchPickerItemBottomSheet.newInstance(
                        Locale.getISOCountries().toList().map {
                            CountryPickerItem(it)
                        }
                    )
                )
            }

            fullName.addTextChangedListener(textWatcher)
            addressLine1.addTextChangedListener(textWatcher)
            addressLine2.addTextChangedListener(textWatcher)
            city.addTextChangedListener(textWatcher)
            zipUsa.addTextChangedListener(textWatcher)
            state.addTextChangedListener(textWatcher)
            postcode.addTextChangedListener(textWatcher)

            compositeDisposable += userService.getUser()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onError = {}, onSuccess = { user ->
                    setupCountryDetails(user.address?.countryCode ?: "")
                    setupUserDetails(user)
                })

            btnNext.setOnClickListener {
                model.process(
                    CardIntent.UpdateBillingAddress(
                        BillingAddress(
                            countryCode = countryPickerItem?.code
                                ?: throw java.lang.IllegalStateException("No country selected"),
                            postCode = (if (usSelected) zipUsa.text else postcode.text).toString(),
                            state = if (usSelected) state.text.toString() else null,
                            city = city.text.toString(),
                            addressLine1 = addressLine1.text.toString(),
                            addressLine2 = addressLine2.text.toString(),
                            fullName = fullName.text.toString()
                        )
                    )
                )
                model.process(CardIntent.ReadyToAddNewCard)

                analytics.logEvent(SimpleBuyAnalytics.CARD_BILLING_ADDRESS_SET)
            }
        }
        activity.updateToolbarTitle(getString(R.string.add_card_address_title))

        model.process(CardIntent.LoadListOfUsStates)
    }

    private fun setupUserDetails(user: NabuUser) {
        with(binding) {
            fullName.setText(getString(R.string.common_spaced_strings, user.firstName, user.lastName))
            user.address?.let {
                addressLine1.setText(it.line1)
                addressLine2.setText(it.line2)
                city.setText(it.city)
                if (it.countryCode == "US") {
                    zipUsa.setText(it.postCode)
                    val stateName = it.stateIso?.let {
                        USState.findStateByIso(it)?.displayName ?: it.substringAfter("US-")
                    }
                    state.setText(stateName)
                } else {
                    postcode.setText(it.postCode)
                }
            }
        }
    }

    private fun setupCountryDetails(countryCode: String) {
        onItemPicked(CountryPickerItem(countryCode))
        configureUiForCountry(countryCode == "US")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.clear()
    }

    override fun onItemPicked(item: PickerItem) {
        when (item) {
            is CountryPickerItem -> {
                binding.countryText.text = item.label
                binding.flagIcon.text = item.icon
                configureUiForCountry(item.code == "US")
                countryPickerItem = item
            }
            is StatePickerItem -> {
                binding.state.setText(item.code)
                statePickerItem = item
            }
        }
    }

    private fun configureUiForCountry(usSelected: Boolean) {
        binding.postcodeInput.visibleIf { usSelected.not() }
        binding.statesFields.visibleIf { usSelected }
        this.usSelected = usSelected
    }

    override val model: CardModel by scopedInject()
    override val navigator: AddCardNavigator
        get() = (activity as? AddCardNavigator)
            ?: throw IllegalStateException("Parent must implement AddCardNavigator")

    override fun render(newState: CardState) {
        if (newState.addCard) {
            navigator.navigateToCardVerification()
        }

        newState.usStateList?.let { stateList ->
            if (stateList.isNotEmpty()) {
                binding.state.setOnClickListener {
                    showBottomSheet(
                        SearchPickerItemBottomSheet.newInstance(
                            stateList.map { state ->
                                StatePickerItem(state.stateCode, state.name)
                            }
                        )
                    )
                }
            } else {
                BlockchainSnackbar.make(
                    view = binding.root,
                    message = getString(R.string.unable_to_load_list_of_states),
                    type = SnackbarType.Error,
                    actionLabel = getString(R.string.common_try_again),
                    onClick = {
                        model.process(CardIntent.LoadListOfUsStates)
                    }
                ).show()
            }
        }
    }

    override fun onSheetClosed() {
        // do nothing
    }
}
