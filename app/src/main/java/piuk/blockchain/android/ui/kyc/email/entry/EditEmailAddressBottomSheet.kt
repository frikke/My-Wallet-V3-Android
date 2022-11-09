package piuk.blockchain.android.ui.kyc.email.entry

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.util.Patterns
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.DialogFragment
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.viewextensions.hideKeyboard
import com.google.android.material.textfield.TextInputEditText
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ChangeEmailBottomSheetBinding
import piuk.blockchain.android.ui.customviews.KeyPreImeEditText
import piuk.blockchain.android.util.AfterTextChangedWatcher

class EditEmailAddressBottomSheet : SlidingModalBottomDialog<ChangeEmailBottomSheetBinding>() {

    interface Host : SlidingModalBottomDialog.Host {
        fun updateEmail(email: String)
    }

    private val argCurrentEmail: String by lazy {
        arguments?.getString(ARG_CURRENT_EMAIL)!!
    }

    override val host: Host
        get() = super.host as Host

    override fun initControls(binding: ChangeEmailBottomSheetBinding) {
        showKeyboard()
        with(binding) {
            save.isEnabled = false
            editEmailInput.setText(argCurrentEmail)
            editEmailInput.setSelection(argCurrentEmail.length)

            editEmailInput.keyImeChangeListener = object : KeyPreImeEditText.KeyImeChange {
                override fun onKeyIme(keyCode: Int, event: KeyEvent?) {
                    if (keyCode == KEYCODE_BACK) {
                        dismiss()
                    }
                }
            }
            editEmailInput.addTextChangedListener(object : AfterTextChangedWatcher() {
                override fun afterTextChanged(textEntered: Editable?) {
                    save.isEnabled = canUpdateEmail(textEntered?.toString().orEmpty())
                }
            })

            save.setOnClickListener {
                host.updateEmail(editEmailInput.text?.toString().orEmpty())
                dismiss()
                requireActivity().hideKeyboard()
            }
        }
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): ChangeEmailBottomSheetBinding {
        return ChangeEmailBottomSheetBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FloatingBottomSheet)
    }

    private val imm: InputMethodManager by lazy {
        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    private fun showKeyboard() {
        val inputView = binding.root.findViewById<TextInputEditText>(
            R.id.edit_email_input
        )
        inputView?.run {
            requestFocus()
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun canUpdateEmail(emailInput: String): Boolean =
        emailInputIsValid(emailInput) && argCurrentEmail != emailInput

    private fun emailInputIsValid(emailInput: String): Boolean =
        !TextUtils.isEmpty(emailInput) && Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()

    companion object {
        private const val ARG_CURRENT_EMAIL = "ARG_CURRENT_EMAIL"

        fun newInstance(
            currentEmail: String
        ): EditEmailAddressBottomSheet = EditEmailAddressBottomSheet().apply {
            arguments = Bundle().apply {
                putString(ARG_CURRENT_EMAIL, currentEmail)
            }
        }
    }
}
