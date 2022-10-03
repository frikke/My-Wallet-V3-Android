package piuk.blockchain.android.util

import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import piuk.blockchain.android.R

fun TextView.animateChange(onAnimationEnd: () -> Unit) {
    pivotX = this.measuredWidth * 0.5f
    pivotY = this.measuredHeight * 0.5f

    setTextColor(ContextCompat.getColor(context, R.color.blue_600))

    animate()
        .scaleX(1.1f)
        .scaleY(1.1f)
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

fun TextView.animateColor(onAnimationEnd: () -> Unit) {
    pivotX = this.measuredWidth * 0.5f
    pivotY = this.measuredHeight * 0.5f

    setTextColor(ContextCompat.getColor(context, R.color.blue_600))

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
