package piuk.blockchain.android.ui.customviews

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewDashboardOnboardingBinding

class DashboardOnboardingCard @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle) {

    private val binding: ViewDashboardOnboardingBinding =
        ViewDashboardOnboardingBinding.inflate(LayoutInflater.from(context), this, true)

    private var totalSteps = 0
    fun setTotalSteps(totalSteps: Int) {
        this.totalSteps = totalSteps
    }

    fun setCompleteSteps(completeSteps: Int) {
        binding.progressSteps.setProgress((completeSteps.toFloat() / totalSteps.toFloat()) * 100f)
        binding.textSteps.text =
            context.getString(
                com.blockchain.stringResources.R.string.dashboard_onboarding_steps_counter,
                completeSteps,
                totalSteps
            )
    }
}
