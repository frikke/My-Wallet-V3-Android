package piuk.blockchain.android.ui.createwallet

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintSet
import com.blockchain.api.services.Geolocation
import com.blockchain.core.CountryIso
import com.blockchain.koin.scopedInject
import com.blockchain.wallet.DefaultLabels
import com.jakewharton.rxbinding4.widget.afterTextChangeEvents
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.cards.CountryPickerItem
import piuk.blockchain.android.cards.PickerItem
import piuk.blockchain.android.cards.PickerItemListener
import piuk.blockchain.android.cards.SearchPickerItemBottomSheet
import piuk.blockchain.android.cards.StatePickerItem
import piuk.blockchain.android.databinding.ActivityCreateWalletBinding
import piuk.blockchain.android.databinding.ToolbarGeneralBinding
import piuk.blockchain.android.databinding.ViewPasswordStrengthBinding
import piuk.blockchain.android.ui.auth.PinEntryActivity
import piuk.blockchain.android.ui.base.BaseMvpActivity
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.dialogs.MaterialProgressDialog
import piuk.blockchain.android.ui.customviews.toast
import piuk.blockchain.android.ui.kyc.email.entry.KycEmailEntryFragment
import piuk.blockchain.android.urllinks.URL_BACKUP_INFO
import piuk.blockchain.android.urllinks.URL_PRIVACY_POLICY
import piuk.blockchain.android.urllinks.URL_TOS_POLICY
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.US
import piuk.blockchain.android.util.ViewUtils
import piuk.blockchain.android.util.getTextString
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class CreateWalletActivity :
    BaseMvpActivity<CreateWalletView, CreateWalletPresenter>(),
    CreateWalletView,
    PickerItemListener,
    SlidingModalBottomDialog.Host,
    View.OnFocusChangeListener {

    private val defaultLabels: DefaultLabels by inject()
    private val createWalletPresenter: CreateWalletPresenter by scopedInject()
    private var progressDialog: MaterialProgressDialog? = null
    private var applyConstraintSet: ConstraintSet = ConstraintSet()
    private var countryPickerItem: CountryPickerItem? = null
    private var statePickerItem: StatePickerItem? = null

    override val alwaysDisableScreenshots: Boolean
        get() = true

    private val recoveryPhrase: String by unsafeLazy {
        intent.getStringExtra(RECOVERY_PHRASE).orEmpty()
    }

    private val binding: ActivityCreateWalletBinding by lazy {
        ActivityCreateWalletBinding.inflate(layoutInflater)
    }

    private val passwordStrengthBinding: ViewPasswordStrengthBinding by lazy {
        ViewPasswordStrengthBinding.inflate(layoutInflater, binding.root, false)
    }

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupCta()
        updateToolbar(
            toolbarTitle = if (recoveryPhrase.isNotEmpty()) {
                getString(R.string.recover_funds)
            } else {
                getString(R.string.new_account_title_1)
            },
            backAction = { onBackPressed() }
        )
        applyConstraintSet.clone(binding.mainConstraintLayout)

        createWalletPresenter.getUserGeolocation()

        initializeStatesSpinner()

        with(binding) {
            passwordStrengthBinding.passStrengthBar.max = 100 * 10

            walletPass.afterTextChangeEvents()
                .doOnNext {
                    showEntropyContainer()
                    createWalletPresenter.logEventPasswordOneClicked()
                    binding.entropyContainerNew.updatePassword(it.editable.toString())
                    updateCreateButtonState(
                        it.editable.toString().length,
                        walletPassConfirm.getTextString().length,
                        walletPasswordCheckbox.isChecked
                    )
                }
                .emptySubscribe()

            walletPassConfirm.afterTextChangeEvents()
                .doOnNext {
                    createWalletPresenter.logEventPasswordTwoClicked()
                    updateCreateButtonState(
                        walletPass.getTextString().length,
                        it.editable.toString().length,
                        walletPasswordCheckbox.isChecked
                    )
                }
                .emptySubscribe()

            walletPasswordCheckbox.setOnCheckedChangeListener { _, isChecked ->
                updateCreateButtonState(
                    walletPass.getTextString().length,
                    walletPassConfirm.getTextString().length,
                    isChecked
                )
            }

            emailAddress.setOnClickListener { createWalletPresenter.logEventEmailClicked() }
            commandNext.setOnClickListener { onNextClicked() }

            updatePasswordDisclaimer()

            walletPassConfirm.setOnEditorActionListener { _, i, _ ->
                consume { if (i == EditorInfo.IME_ACTION_GO) onNextClicked() }
            }

            hideEntropyContainer()

            onViewReady()
        }
    }

    private fun setupCta() {
        if (recoveryPhrase.isNotEmpty()) {
            binding.commandNext.setText(R.string.dialog_continue)
        } else {
            binding.commandNext.setText(R.string.new_account_cta_text)
        }
    }

    private fun initializeStatesSpinner() {
        binding.state.setOnClickListener {
            SearchPickerItemBottomSheet.newInstance(
                US.values()
                    .map {
                        StatePickerItem(it.iSOAbbreviation, it.unabbreviated)
                    }
            ).show(supportFragmentManager, KycEmailEntryFragment.BOTTOM_SHEET)
        }
    }

    private fun updatePasswordDisclaimer() {
        val linksMap = mapOf<String, Uri>(
            "backup" to Uri.parse(URL_BACKUP_INFO),
            "terms" to Uri.parse(URL_TOS_POLICY),
            "privacy" to Uri.parse(URL_PRIVACY_POLICY)
        )

        val disclaimerText = StringUtils.getStringWithMappedAnnotations(
            this,
            R.string.password_disclaimer_1,
            linksMap
        )

        binding.walletPasswordBlurb.apply {
            text = disclaimerText
            movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun updateCreateButtonState(password1Length: Int, password2Length: Int, isChecked: Boolean) {
        val areFieldsFilled = (password1Length > 0 && password1Length == password2Length && isChecked)
        binding.commandNext.isEnabled = areFieldsFilled
    }

    override fun getView() = this

    override fun createPresenter() = createWalletPresenter

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun hideEntropyContainer() = binding.entropyContainerNew.gone()

    private fun showEntropyContainer() = binding.entropyContainerNew.visible()

    override fun onFocusChange(v: View?, hasFocus: Boolean) = when {
        hasFocus -> showEntropyContainer()
        else -> hideEntropyContainer()
    }

    override fun showError(message: Int) =
        toast(message, ToastCustom.TYPE_ERROR)

    override fun warnWeakPassword(email: String, password: String) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(R.string.weak_password)
            .setPositiveButton(R.string.common_retry) { _, _ ->
                binding.apply {
                    walletPass.setText("")
                    walletPassConfirm.setText("")
                    walletPass.requestFocus()
                }
            }.show()
    }

    override fun startPinEntryActivity() {
        ViewUtils.hideKeyboard(this)
        PinEntryActivity.startAfterWalletCreation(this)
    }

    override fun showProgressDialog(message: Int) {
        dismissProgressDialog()
        progressDialog = MaterialProgressDialog(this).apply {
            setCancelable(false)
            setMessage(getString(message))
            if (!isFinishing) show()
        }
    }

    override fun getDefaultAccountName(): String = defaultLabels.getDefaultNonCustodialWalletLabel()

    override fun setEligibleCountries(countries: List<CountryIso>) {
        binding.country.setOnClickListener {
            SearchPickerItemBottomSheet.newInstance(
                countries.map { countryCode ->
                    CountryPickerItem(countryCode)
                }
            ).show(supportFragmentManager, KycEmailEntryFragment.BOTTOM_SHEET)
        }
    }

    override fun setGeolocationInCountrySpinner(geolocation: Geolocation) {
        if (countryPickerItem == null) {
            val countryGeo = CountryPickerItem(geolocation.countryCode)
            onItemPicked(countryGeo)
        }
        if (statePickerItem == null) {
            geolocation.state?.let { stateCode ->
                val stateGeo = createStateItemFromIsoCode(stateCode)
                onItemPicked(stateGeo)
            }
        }
    }

    private fun createStateItemFromIsoCode(isoCode: String): StatePickerItem {
        val stateItem = US.values().first { it.iSOAbbreviation == isoCode }
        return StatePickerItem(stateItem.iSOAbbreviation, stateItem.unabbreviated)
    }

    private fun onNextClicked() {
        with(binding) {
            val email = emailAddress.text.toString().trim()
            val password1 = walletPass.text.toString()
            val password2 = walletPassConfirm.text.toString()
            val countryCode = countryPickerItem?.code
            val stateCode = statePickerItem?.code

            if (walletPasswordCheckbox.isChecked &&
                createWalletPresenter.validateCredentials(email, password1, password2) &&
                createWalletPresenter.validateGeoLocation(countryCode, stateCode)
            ) {
                countryCode?.let {
                    createWalletPresenter.createOrRestoreWallet(email, password1, recoveryPhrase, it, stateCode)
                }
            }
        }
    }

    override fun onItemPicked(item: PickerItem) {
        when (item) {
            is CountryPickerItem -> {
                countryPickerItem = item
                binding.country.setText(item.label)
                changeStatesSpinnerVisibility(item.code == CODE_US)
                WalletCreationAnalytics.CountrySelectedOnSignUp(item.code)
            }
            is StatePickerItem -> {
                statePickerItem = item
                binding.state.setText(item.label)
                WalletCreationAnalytics.StateSelectedOnSignUp(item.code)
            }
        }
        ViewUtils.hideKeyboard(this)
    }

    private fun changeStatesSpinnerVisibility(showStateSpinner: Boolean) {
        if (showStateSpinner) {
            binding.selectState.visible()
        } else {
            binding.selectState.gone()
            statePickerItem = null
        }
    }

    override fun onSheetClosed() {
        // do nothing
    }

    companion object {
        const val CODE_US = "US"
        const val RECOVERY_PHRASE = "RECOVERY_PHRASE"

        fun start(context: Context) {
            context.startActivity(Intent(context, CreateWalletActivity::class.java))
        }
    }
}
