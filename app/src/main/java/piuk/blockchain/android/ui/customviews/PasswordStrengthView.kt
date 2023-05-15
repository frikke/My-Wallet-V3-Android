package piuk.blockchain.android.ui.customviews

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
import piuk.blockchain.android.databinding.ViewPasswordStrengthBinding

class PasswordStrengthView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {
    private val binding: ViewPasswordStrengthBinding =
        ViewPasswordStrengthBinding.inflate(LayoutInflater.from(context), this, true)

    private val strengthVerdicts = intArrayOf(
        com.blockchain.stringResources.R.string.strength_weak,
        com.blockchain.stringResources.R.string.strength_medium,
        com.blockchain.stringResources.R.string.strength_normal,
        com.blockchain.stringResources.R.string.strength_strong
    )

    private val strengthProgressDrawables = intArrayOf(
        R.drawable.progress_red,
        R.drawable.progress_orange,
        R.drawable.progress_blue,
        R.drawable.progress_green
    )

    private val strengthColors = intArrayOf(
        com.blockchain.common.R.color.product_red_medium,
        com.blockchain.common.R.color.product_orange_medium,
        com.blockchain.common.R.color.primary_blue_medium,
        com.blockchain.common.R.color.product_green_medium
    )

    init {
        with(binding.passStrengthBar) {
            max = 100
            interpolator = DecelerateInterpolator()
        }
    }

    fun updatePassword(password: String): Int {
        val passwordStrength = PasswordUtil.getStrength(password).roundToInt()
        setStrengthProgress(passwordStrength)

        when (passwordStrength) {
            in 0..25 -> updateLevelUI(0)
            in 26..50 -> updateLevelUI(1)
            in 51..75 -> updateLevelUI(2)
            in 76..100 -> updateLevelUI(3)
        }

        return passwordStrength
    }

    private fun setStrengthProgress(score: Int) {
        with(binding.passStrengthBar) {
            setProgress(score * 10, true)
        }
    }

    private fun updateLevelUI(level: Int) {
        with(binding) {
            passStrengthVerdict.setText(strengthVerdicts[level])
            passStrengthBar.progressDrawable = context.getResolvedDrawable(strengthProgressDrawables[level])
            passStrengthVerdict.setTextColor(context.getResolvedColor(strengthColors[level]))
        }
    }
}
