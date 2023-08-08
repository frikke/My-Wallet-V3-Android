package piuk.blockchain.android.ui.customviews.recovery

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.DecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.presentation.getResolvedColor
import com.blockchain.presentation.getResolvedDrawable
import info.blockchain.wallet.util.PasswordUtil
import kotlin.math.roundToInt
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewRecoveryPasswordStrengthBinding

private enum class PasswordStrengthLevel(val label: Int, val drawable: Int, val color: Int) {
    Weak(
        label = com.blockchain.stringResources.R.string.strength_weak,
        drawable = R.drawable.progress_red,
        color = com.blockchain.componentlib.R.color.error
    ),
    Medium(
        label = com.blockchain.stringResources.R.string.strength_medium,
        drawable = R.drawable.progress_orange,
        color = com.blockchain.componentlib.R.color.warning
    ),
    Strong(
        label = com.blockchain.stringResources.R.string.strength_strong,
        drawable = R.drawable.progress_green,
        color = com.blockchain.componentlib.R.color.success
    )
}

class RecoveryPasswordStrengthView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    private val binding: ViewRecoveryPasswordStrengthBinding =
        ViewRecoveryPasswordStrengthBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        with(binding.passStrengthBar) {
            max = MAX_STRENGTH
            interpolator = DecelerateInterpolator()
        }
    }

    fun updatePassword(password: String) {
        val passwordStrength = PasswordUtil.getStrength(password).roundToInt()
        setStrengthProgress(passwordStrength)

        when (passwordStrength) {
            in LowerRange -> updateLevelUI(PasswordStrengthLevel.Weak)
            in MidRange -> updateLevelUI(PasswordStrengthLevel.Medium)
            in UpperRange -> updateLevelUI(PasswordStrengthLevel.Strong)
        }
    }

    private fun setStrengthProgress(score: Int) {
        with(binding.passStrengthBar) {
            setProgress(score, true)
        }
    }

    private fun updateLevelUI(level: PasswordStrengthLevel) {
        with(binding) {
            passStrengthVerdict.setText(level.label)
            passStrengthBar.progressDrawable = context.getResolvedDrawable(level.drawable)
            passStrengthVerdict.setTextColor(context.getResolvedColor(level.color))
        }
    }

    companion object {
        private const val MAX_STRENGTH = 100
        private const val INTERVAL = 33 // MAX_STRENGTH / 3
        private const val STRENGTH_LOW_START = 0
        private const val STRENGTH_MEDIUM_START = 34
        private const val STRENGTH_STRONG_START = 68
        private val LowerRange: IntRange =
            IntRange(
                start = STRENGTH_LOW_START,
                endInclusive = INTERVAL
            )
        private val MidRange: IntRange =
            IntRange(
                start = STRENGTH_MEDIUM_START,
                endInclusive = STRENGTH_MEDIUM_START + INTERVAL
            )
        private val UpperRange: IntRange =
            IntRange(
                start = STRENGTH_STRONG_START,
                endInclusive = MAX_STRENGTH
            )
    }
}
