package piuk.blockchain.android.ui.dashboard.onboarding

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemOnboardingStepBinding
import piuk.blockchain.android.domain.usecases.CompletableDashboardOnboardingStep
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.px
import piuk.blockchain.android.util.visibleIf

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
            binding.root.cardElevation = if (item.isCompleted) 0f else 2.px.toFloat()
            if (item.isCompleted) {
                binding.root.setOnClickListener(null)
            } else {
                binding.root.setOnClickListener { onClick(item) }
            }

            icon.setBackgroundResource(item.step.iconRes)

            textSubtitle.setText(
                if (item.isCompleted) R.string.dashboard_onboarding_step_complete
                else item.step.subtitleRes
            )
            textSubtitle.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (item.isCompleted) R.color.paletteBaseSuccess
                    else R.color.grey_600
                )
            )
            textTitle.setText(item.step.titleRes)
            actionIcon.visibleIf { !item.isCompleted }
            completedIcon.visibleIf { item.isCompleted }
            if (!item.isCompleted) {
                actionIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, item.step.colorRes))
            }
        }
    }
}
