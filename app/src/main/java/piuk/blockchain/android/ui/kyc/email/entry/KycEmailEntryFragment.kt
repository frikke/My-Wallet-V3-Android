package piuk.blockchain.android.ui.kyc.email.entry

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.EmailVerificationArgs
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentKycAddEmailBinding
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.androidcore.data.settings.Email

class KycEmailEntryFragment :
    MviFragment<EmailVerificationModel, EmailVerificationIntent, EmailVerificationState, FragmentKycAddEmailBinding>(),
    SlidingModalBottomDialog.Host,
    ResendOrChangeEmailBottomSheet.ResendOrChangeEmailHost {

    private val emailEntryHost: EmailEntryHost by ParentActivityDelegate(
        this
    )

    private val emailMustBeValidated by lazy {
        when {
            arguments?.containsKey("mustBeValidated") == true ->
                EmailVerificationArgs.fromBundle(arguments ?: Bundle()).mustBeValidated
            arguments?.containsKey(CAN_SKIP) == true -> !requireArguments().getBoolean(CAN_SKIP)
            else -> false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (emailMustBeValidated && savedInstanceState == null) {
            model.process(EmailVerificationIntent.ResendEmail)
        }
        model.process(EmailVerificationIntent.StartEmailVerification)

        emailEntryHost.onEmailEntryFragmentUpdated(shouldShowButton = true) {
            emailEntryHost.onEmailVerificationSkipped()
            emailEntryHost.onEmailEntryFragmentUpdated(shouldShowButton = false)
        }
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentKycAddEmailBinding =
        FragmentKycAddEmailBinding.inflate(inflater, container, false)

    override val model: EmailVerificationModel by scopedInject()

    override fun render(newState: EmailVerificationState) {
        when {
            !newState.email.isVerified -> drawUnVerifiedEmailUi(newState.email)
            newState.email.isVerified -> drawVerifiedEmailUi()
            newState.emailChanged -> drawUnVerifiedEmailUi(newState.email)
            newState.hasError -> drawErrorMessage()
            else -> throw IllegalStateException("Not a valid state")
        }
    }

    private fun drawErrorMessage() {
        with(binding) {
            txStateIndicator.setImageResource(R.drawable.ic_alert_white_bkgd)
            emailStatusText.text = getString(R.string.error_email_veriff_title)
            emailInstructions.text = getString(R.string.error_email_veriff)
            emailEntryHost.onEmailEntryFragmentUpdated(shouldShowButton = false)
        }
        setUpPrimaryCta()
        setUpSecondaryCta()
        model.process(EmailVerificationIntent.StartEmailVerification)
    }

    private fun drawVerifiedEmailUi() {
        with(binding) {
            emailInstructions.text = getString(R.string.success_email_veriff)
            emailStatusText.text = getString(R.string.email_verified)
            emailEntryHost.onEmailEntryFragmentUpdated(shouldShowButton = false)

            txStateIndicator.setImageResource(R.drawable.ic_check_circle)
            txStateIndicator.visible()
            ctaPrimary.apply {
                visible()
                text = getString(R.string.common_next)
                setOnClickListener {
                    emailEntryHost.onEmailVerified()
                }
            }
            ctaSecondary.gone()
        }
    }

    private fun drawUnVerifiedEmailUi(email: Email) {
        val boldText = email.address.takeIf { it.isNotEmpty() } ?: return
        val partOne = getString(R.string.email_verification_part_1)
        val partTwo = getString(R.string.email_verification_part_2)
        val sb = SpannableStringBuilder()
            .append(partOne)
            .append(boldText)
            .append(partTwo)

        sb.setSpan(
            StyleSpan(Typeface.BOLD),
            partOne.length,
            partOne.length + boldText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        with(binding) {
            emailInstructions.setText(sb, TextView.BufferType.SPANNABLE)
            emailStatusText.text = getString(R.string.email_verify)
            txStateIndicator.gone()

            if (emailMustBeValidated) {
                emailEntryHost.onEmailEntryFragmentUpdated(shouldShowButton = false)
            }
        }
        setUpPrimaryCta()
        setUpSecondaryCta()
    }

    private fun setUpPrimaryCta() {
        binding.ctaPrimary.apply {
            visible()
            text = getString(R.string.check_my_inbox)
            setOnClickListener {
                openInbox()
            }
        }
    }

    private fun setUpSecondaryCta() {
        binding.ctaSecondary.apply {
            visible()
            text = getString(R.string.did_not_get_email)
            setOnClickListener {
                model.process(EmailVerificationIntent.CancelEmailVerification)
                ResendOrChangeEmailBottomSheet().show(childFragmentManager, BOTTOM_SHEET)
            }
        }
    }

    private fun openInbox() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_APP_EMAIL)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(Intent.createChooser(intent, getString(R.string.security_centre_email_check)))
    }

    override fun resendEmail() {
        model.process(EmailVerificationIntent.ResendEmail)
    }

    override fun editEmail() {
        EditEmailAddressBottomSheet().show(childFragmentManager, BOTTOM_SHEET)
    }

    override fun onSheetClosed() {
        model.process(EmailVerificationIntent.StartEmailVerification)
    }

    companion object {
        const val BOTTOM_SHEET = "BOTTOM_SHEET"
        private const val CAN_SKIP = "CAN_SKIP"

        fun newInstance(isSkippable: Boolean): KycEmailEntryFragment =
            KycEmailEntryFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(CAN_SKIP, isSkippable)
                }
            }
    }
}

interface EmailEntryHost {
    fun onEmailEntryFragmentUpdated(shouldShowButton: Boolean, buttonAction: () -> Unit = {})
    fun onEmailVerified()
    fun onEmailVerificationSkipped()
}
