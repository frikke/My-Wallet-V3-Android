package piuk.blockchain.android.ui.dashboard.onboarding

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.componentlib.viewextensions.px
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.domain.onboarding.CompletableDashboardOnboardingStep
import com.blockchain.domain.onboarding.DashboardOnboardingStepState
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemOnboardingStepBinding
import piuk.blockchain.android.util.context

class OnboardingStepAdapter(
    private val onStepClicked: (CompletableDashboardOnboardingStep) -> Unit
) : ListAdapter<CompletableDashboardOnboardingStep, OnboardingStepViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingStepViewHolder =
        OnboardingStepViewHolder(
            ItemOnboardingStepBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: OnboardingStepViewHolder, position: Int) =
        holder.bind(getItem(position), onStepClicked)

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CompletableDashboardOnboardingStep>() {
            override fun areItemsTheSame(
                oldItem: CompletableDashboardOnboardingStep,
                newItem: CompletableDashboardOnboardingStep
            ): Boolean = oldItem.step == newItem.step

            override fun areContentsTheSame(
                oldItem: CompletableDashboardOnboardingStep,
                newItem: CompletableDashboardOnboardingStep
            ): Boolean = oldItem == newItem
        }
    }
}

class OnboardingStepViewHolder(
    private val binding: ItemOnboardingStepBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: CompletableDashboardOnboardingStep, onClick: (CompletableDashboardOnboardingStep) -> Unit) {
        with(binding) {
            binding.root.cardElevation =
                if (item.state == DashboardOnboardingStepState.INCOMPLETE) 2.px.toFloat()
                else 0f
            if (item.isCompleted) {
                binding.root.setOnClickListener(null)
            } else {
                binding.root.setOnClickListener { onClick(item) }
            }

            icon.setBackgroundResource(item.step.iconRes)

            textSubtitle.setText(
                when (item.state) {
                    DashboardOnboardingStepState.INCOMPLETE -> item.step.subtitleRes
                    DashboardOnboardingStepState.PENDING -> R.string.dashboard_onboarding_step_pending
                    DashboardOnboardingStepState.COMPLETE -> R.string.dashboard_onboarding_step_complete
                }
            )
            textSubtitle.setTextColor(
                ContextCompat.getColor(
                    context,
                    when (item.state) {
                        DashboardOnboardingStepState.INCOMPLETE -> R.color.grey_600
                        DashboardOnboardingStepState.PENDING -> R.color.grey_800
                        DashboardOnboardingStepState.COMPLETE -> R.color.paletteBaseSuccess
                    }
                )
            )
            textTitle.setText(item.step.titleRes)
            actionIcon.visibleIf { item.state == DashboardOnboardingStepState.INCOMPLETE }
            progressAction.visibleIf { item.state == DashboardOnboardingStepState.PENDING }
            completedIcon.visibleIf { item.isCompleted }
            if (item.state == DashboardOnboardingStepState.INCOMPLETE) {
                actionIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, item.step.colorRes))
            }
        }
    }
}
