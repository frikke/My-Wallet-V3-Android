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
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.domain.paymentmethods.model.BillingAddress
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.payments.core.CardBillingAddress
import com.blockchain.payments.vgs.VgsCardTokenizerService
import com.blockchain.presentation.koin.scopedInject
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.util.Locale
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentBillingAddressBinding
import piuk.blockchain.android.fraud.domain.service.FraudFlow
import piuk.blockchain.android.fraud.domain.service.FraudService
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.util.AfterTextChangedWatcher

class BillingAddressFragment :
    MviFragment<CardModel, CardIntent, CardState, FragmentBillingAddressBinding>(),
    PickerItemListener,
    AddCardFlowFragment,
    SlidingModalBottomDialog.Host {

    private var usSelected = false
    private var isVgsEnabled = false
    private val userService: UserService by scopedInject()
    private val cardTokenizerService: VgsCardTokenizerService by scopedInject()
    private val fraudService: FraudService by inject()

    private val compositeDisposable = CompositeDisposable()

    override val cardDetailsPersistence: CardDetailsPersistence
        get() = (activity as? CardDetailsPersistence)
            ?: throw IllegalStateException("Parent must implement CardDetailsPersistence")

    private var countryPickerItem: CountryPickerItem? = null
    private var statePickerItem: StatePickerItem? = null

    private val textWatcher: (String) -> Unit = {
        binding.btnNext.buttonState = if (addressIsValid()) ButtonState.Enabled else ButtonState.Disabled
    }

    private fun addressIsValid(): Boolean =
        binding.fullNameInput.value.isNotBlank() &&
            binding.addressLine1Input.value.isNotBlank() &&
            binding.cityInput.value.isNotBlank() &&
            (
                if (usSelected) {
                    binding.zipUsaInput.value.isNotBlank() && binding.stateInput.value.isNotBlank()
                } else binding.postcodeInput.value.isNotBlank()
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

            fullNameInput.apply {
                labelText = getString(com.blockchain.stringResources.R.string.full_name)
                onValueChange = textWatcher
            }
            addressLine1Input.apply {
                labelText = getString(com.blockchain.stringResources.R.string.address_line_1)
                onValueChange = textWatcher
            }
            addressLine2Input.apply {
                labelText = getString(com.blockchain.stringResources.R.string.address_line_2)
                onValueChange = textWatcher
            }
            cityInput.apply {
                labelText = getString(com.blockchain.stringResources.R.string.address_city)
                onValueChange = textWatcher
            }
            zipUsaInput.apply {
                labelText = getString(com.blockchain.stringResources.R.string.address_zip)
                onValueChange = textWatcher
            }
            stateInput.apply {
                labelText = getString(com.blockchain.stringResources.R.string.billing_address_state)
                onValueChange = textWatcher
            }
            postcodeInput.apply {
                labelText = getString(com.blockchain.stringResources.R.string.address_postcode)
                onValueChange = textWatcher
            }

            compositeDisposable += userService.getUser()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onError = {}, onSuccess = { user ->
                    setupCountryDetails(user.address?.countryCode ?: "")
                    setupUserDetails(user)
                })

            btnNext.text = getString(com.blockchain.stringResources.R.string.common_next)
            btnNext.onClick = {
                fraudService.endFlow(FraudFlow.CARD_LINK)

                val billingAddress = BillingAddress(
                    countryCode = countryPickerItem?.code
                        ?: throw java.lang.IllegalStateException("No country selected"),
                    postCode = if (usSelected) zipUsaInput.value else postcodeInput.value,
                    state = if (usSelected) stateInput.value else null,
                    city = cityInput.value,
                    addressLine1 = addressLine1Input.value,
                    addressLine2 = addressLine2Input.value,
                    fullName = fullNameInput.value
                )

                if (isVgsEnabled) {
                    cardTokenizerService.bindAddressDetails(
                        CardBillingAddress(
                            city = billingAddress.city,
                            countryCode = billingAddress.countryCode,
                            addressLine1 = billingAddress.addressLine1,
                            addressLine2 = billingAddress.addressLine2,
                            postalCode = billingAddress.postCode,
                            state = billingAddress.state
                        )
                    )
                    model.process(CardIntent.SubmitVgsCardInfo)
                } else {
                    model.process(CardIntent.UpdateBillingAddress(billingAddress))
                    model.process(CardIntent.ReadyToAddNewCard)
                }

                analytics.logEvent(SimpleBuyAnalytics.CARD_BILLING_ADDRESS_SET)
            }
        }
        activity.updateToolbarTitle(getString(com.blockchain.stringResources.R.string.add_card_address_title))

        model.process(CardIntent.LoadListOfUsStates)
    }

    private fun setupUserDetails(user: NabuUser) {
        with(binding) {
            fullNameInput.value =
                getString(com.blockchain.stringResources.R.string.common_spaced_strings, user.firstName, user.lastName)
            user.address?.let {
                addressLine1Input.value = it.line1.orEmpty()
                addressLine2Input.value = it.line2.orEmpty()
                cityInput.value = it.city.orEmpty()
                if (it.countryCode == "US") {
                    zipUsaInput.value = it.postCode.orEmpty()
                    val stateName = it.stateIso?.let {
                        USState.findStateByIso(it)?.displayName ?: it.substringAfter("US-")
                    }
                    stateInput.value = stateName.orEmpty()
                } else {
                    postcodeInput.value = it.postCode.orEmpty()
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
                binding.stateInput.value = item.code
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
        isVgsEnabled = newState.isVgsEnabled
        newState.vgsTokenResponse?.let {
            navigator.navigateToCardVerification()
        }

        if (newState.addCard) {
            navigator.navigateToCardVerification()
        }

        if (newState.showCardCreationError) {
            BlockchainSnackbar.make(
                view = binding.root,
                message = com.blockchain.stringResources.R.string.something_went_wrong_try_again,
                type = SnackbarType.Error
            ).show()
            model.process(CardIntent.ErrorHandled)
        }

        newState.usStateList?.let { stateList ->
            if (stateList.isNotEmpty()) {
                binding.stateInputSelect.setOnClickListener {
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
                    message = getString(com.blockchain.stringResources.R.string.unable_to_load_list_of_states),
                    type = SnackbarType.Error,
                    actionLabel = getString(com.blockchain.stringResources.R.string.common_try_again),
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
