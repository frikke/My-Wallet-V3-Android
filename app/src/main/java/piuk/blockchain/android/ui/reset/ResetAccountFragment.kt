package piuk.blockchain.android.ui.reset

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.commonarch.presentation.base.addTransactionAnimation
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.presentation.koin.scopedInject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentAccountResetBinding
import piuk.blockchain.android.ui.recover.AccountRecoveryAnalytics
import piuk.blockchain.android.ui.reset.password.ResetPasswordFragment

class ResetAccountFragment :
    MviFragment<ResetAccountModel, ResetAccountIntents, ResetAccountState, FragmentAccountResetBinding>() {

    override val model: ResetAccountModel by scopedInject()

    private var isInitialLoop = false

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAccountResetBinding =
        FragmentAccountResetBinding.inflate(inflater, container, false)

    override fun render(newState: ResetAccountState) {
        when (newState.status) {
            ResetAccountStatus.SHOW_INFO -> showInfoScreen()
            ResetAccountStatus.SHOW_WARNING -> showWarningScreen()
            ResetAccountStatus.RETRY -> onBackPressed()
            ResetAccountStatus.RESET -> {
                if (isInitialLoop) {
                    // To handle navigating back to this screen.
                    model.process(ResetAccountIntents.UpdateStatus(ResetAccountStatus.SHOW_WARNING))
                } else {
                    launchResetPasswordFlow()
                }
            }
        }
        binding.contentLayout.visible()

        if (isInitialLoop) {
            isInitialLoop = false
        }
    }

    override fun onStart() {
        super.onStart()
        isInitialLoop = true
    }

    private fun showInfoScreen() {
        with(binding) {
            resetImage.setImageResource(R.drawable.ic_reset_round)
            resetAccountLabel.text = getString(com.blockchain.stringResources.R.string.reset_account_title)
            resetAccountDesc.text = getString(com.blockchain.stringResources.R.string.reset_account_description_1)
            resetButton.apply {
                text = getString(com.blockchain.stringResources.R.string.reset_account_cta)
                onClick = {
                    model.process(ResetAccountIntents.UpdateStatus(ResetAccountStatus.SHOW_WARNING))
                }
            }
            retryButton.apply {
                text = getString(com.blockchain.stringResources.R.string.retry_recovery_phrase_cta)
                onClick = {
                    model.process(ResetAccountIntents.UpdateStatus(ResetAccountStatus.RETRY))
                }
            }
            backButton.setOnClickListener { onBackPressed() }
        }
    }

    private fun showWarningScreen() {
        with(binding) {
            resetImage.setImageResource(R.drawable.ic_triangle_warning_circle)
            resetAccountLabel.text = getString(com.blockchain.stringResources.R.string.reset_account_warning_title)
            resetAccountDesc.text = getString(com.blockchain.stringResources.R.string.reset_account_warning_description)
            resetButton.apply {
                text = getString(com.blockchain.stringResources.R.string.reset_account_cta)
                onClick = {
                    model.process(ResetAccountIntents.UpdateStatus(ResetAccountStatus.RESET))
                }
            }
            retryButton.apply {
                text = getString(com.blockchain.stringResources.R.string.common_go_back)
                onClick = {
                    model.process(ResetAccountIntents.UpdateStatus(ResetAccountStatus.SHOW_INFO))
                }
            }
            backButton.setOnClickListener {
                analytics.logEvent(AccountRecoveryAnalytics.ResetCancelled(isCustodialAccount = true))
                model.process(ResetAccountIntents.UpdateStatus(ResetAccountStatus.SHOW_INFO))
            }
        }
    }

    private fun onBackPressed() {
        analytics.logEvent(AccountRecoveryAnalytics.ResetCancelled(isCustodialAccount = true))
        parentFragmentManager.popBackStack()
    }

    private fun launchResetPasswordFlow() {
        parentFragmentManager.beginTransaction()
            .addTransactionAnimation()
            .replace(
                binding.fragmentContainer.id,
                ResetPasswordFragment.newInstance(
                    shouldResetKyc = true,
                    email = arguments?.getString(ResetPasswordFragment.EMAIL) ?: "",
                    userId = arguments?.getString(ResetPasswordFragment.USER_ID) ?: "",
                    recoveryToken = arguments?.getString(ResetPasswordFragment.RECOVERY_TOKEN) ?: ""
                ),
                ResetPasswordFragment::class.simpleName
            )
            .addToBackStack(ResetPasswordFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    companion object {
        fun newInstance(email: String, userId: String, recoveryToken: String): ResetAccountFragment {
            return ResetAccountFragment().apply {
                arguments = Bundle().apply {
                    putString(ResetPasswordFragment.EMAIL, email)
                    putString(ResetPasswordFragment.USER_ID, userId)
                    putString(ResetPasswordFragment.RECOVERY_TOKEN, recoveryToken)
                }
            }
        }
    }
}
