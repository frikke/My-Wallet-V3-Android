package piuk.blockchain.android.util

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import piuk.blockchain.android.R

fun TextView.animateChange(
    @ColorRes startColor: Int = com.blockchain.componentlib.R.color.title,
    @ColorRes endColor: Int = com.blockchain.componentlib.R.color.primary
) {
    clearAnimation()
    val color1 = ContextCompat.getColor(this.context, startColor)
    val color2 = ContextCompat.getColor(this.context, endColor)

    val colorAnimator = ObjectAnimator.ofArgb(this, "textColor", color1, color2, color1)

    val scaleAnimatorX = ObjectAnimator.ofFloat(this, View.SCALE_X, 1f, 1.1f, 1f)

    val scaleAnimatorY = ObjectAnimator.ofFloat(this, View.SCALE_Y, 1f, 1.1f, 1f)

    AnimatorSet().apply {
        playTogether(colorAnimator, scaleAnimatorX, scaleAnimatorY)
        duration = 666
        interpolator = AccelerateDecelerateInterpolator()
        start()
    }
}

fun TextView.animateColor(onAnimationEnd: () -> Unit) {
    pivotX = this.measuredWidth * 0.5f
    pivotY = this.measuredHeight * 0.5f

    setTextColor(ContextCompat.getColor(context, com.blockchain.componentlib.R.color.primary))

    animate()
        .scaleX(1f)
        .scaleY(1f)
        .setDuration(300)
        .setInterpolator(AccelerateDecelerateInterpolator())
        .withEndAction {
            this.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    onAnimationEnd()
                }
        }
}
