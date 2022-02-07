package com.blockchain.componentlib.viewextensions

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.annotation.IntDef
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

// In window/screen co-ordinates
val View.windowRect: Rect
    get() {
        val loc = IntArray(2)
        getLocationInWindow(loc)
        return Rect(loc[0], loc[1], loc[0] + width, loc[1] + height)
    }

/**
 * Sets the visibility of a [View] to [View.VISIBLE]
 */
fun View?.visible() {
    if (this != null) visibility = View.VISIBLE
}

fun View?.isVisible(): Boolean =
    this?.let { visibility == View.VISIBLE } ?: false

/**
 * Sets the visibility of a [View] to [View.INVISIBLE]
 */
fun View?.invisible() {
    if (this != null) visibility = View.INVISIBLE
}

/**
 * Sets the visibility of a [View] to [View.GONE]
 */
fun View?.gone() {
    if (this != null) visibility = View.GONE
}

/**
 * Sets the visibility of a [View] to [View.VISIBLE] depending on a value
 */
fun View?.visibleIf(func: () -> Boolean) {
    if (this != null) {
        visibility = if (func()) View.VISIBLE else View.GONE
    }
}

/**
 * Sets the visibility of a [View] to [View.GONE] depending on a predicate
 *
 * @param func If true, the visibility of the [View] will be set to [View.GONE], else [View.VISIBLE]
 */
fun View?.goneIf(func: () -> Boolean) {
    if (this != null) {
        visibility = if (func()) View.GONE else View.VISIBLE
    }
}

/**
 * Sets the visibility of a [View] to [View.GONE] depending on a value
 *
 * @param value If true, the visibility of the [View] will be set to [View.GONE], else [View.VISIBLE]
 */
fun View?.goneIf(value: Boolean) {
    if (this != null) {
        visibility = if (value) View.GONE else View.VISIBLE
    }
}

/**
 * Sets the visibility of a [View] to [View.INVISIBLE] depending on a predicate
 *
 * @param func If true, the visibility of the [View] will be set to [View.INVISIBLE], else [View.VISIBLE]
 */
fun View?.invisibleIf(func: () -> Boolean) {
    if (this != null) {
        visibility = if (func()) View.INVISIBLE else View.VISIBLE
    }
}

/**
 * Sets the visibility of a [View] to [View.INVISIBLE] depending on a value
 *
 * @param value If true, the visibility of the [View] will be set to [View.INVISIBLE], else [View.VISIBLE]
 */
fun View?.invisibleIf(value: Boolean) {
    if (this != null) {
        visibility = if (value) View.INVISIBLE else View.VISIBLE
    }
}

/**
 * Allows a [ViewGroup] to inflate itself without all of the unneeded ceremony of getting a
 * [LayoutInflater] and always passing the [ViewGroup] + false. True can optionally be passed if
 * needed.
 *
 * @param layoutId The layout ID as an [Int]
 * @return The inflated [View]
 */
fun ViewGroup.inflate(@LayoutRes layoutId: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutId, this, attachToRoot)
}

/**
 * Returns the current [String] entered into an [EditText]. Non-null, ie can return an empty String.
 */
fun EditText?.getTextString(): String {
    return this?.text?.toString() ?: ""
}

/**
 * This disables the soft keyboard as an input for a given [EditText]. The method
 * [EditText.setShowSoftInputOnFocus] is officially only available on >API21, but is actually hidden
 * from >API16. Here, we attempt to set that field to false, and catch any exception that might be
 * thrown if the Android implementation doesn't include it for some reason.
 */
@SuppressLint("NewApi")
fun EditText.disableSoftKeyboard() {
    try {
        showSoftInputOnFocus = false
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Debounced onClickListener
 *
 * Filter out fast double taps
 */
private class DebouncingOnClickListener(private val onClickListener: (View?) -> Unit) : View.OnClickListener {
    private var lastClick = 0L
    override fun onClick(v: View?) {
        val now = System.currentTimeMillis()
        if (now > lastClick + DEBOUNCE_TIMEOUT) {
            lastClick = now
            onClickListener(v)
        }
    }

    companion object {
        private const val DEBOUNCE_TIMEOUT = 500L
    }
}

fun RecyclerView.configureWithPinnedView(pinnedView: View, isViewVisible: Boolean) {

    pinnedView.visibleIf { isViewVisible }
    when {
        isViewVisible && this.paddingBottom == 0 -> {
            pinnedView.afterMeasured { button ->
                val bottomMargin =
                    (pinnedView.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
                with(this) {
                    setPadding(
                        paddingLeft,
                        paddingTop,
                        paddingRight,
                        button.height + bottomMargin
                    )
                }
            }
        }
        !isViewVisible && this.paddingBottom != 0 -> {
            with(this) {
                setPadding(
                    paddingLeft,
                    paddingTop,
                    paddingRight,
                    0
                )
            }
        }
    }
}

fun View.setOnClickListenerDebounced(onClickListener: (View?) -> Unit) =
    this.setOnClickListener(DebouncingOnClickListener(onClickListener = onClickListener))

fun View.afterMeasured(f: (View) -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (measuredWidth > 0 && measuredHeight > 0) {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                f(this@afterMeasured)
            }
        }
    })
}

val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Int.dp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

/**
 * Hides the keyboard in a specified [AppCompatActivity]
 */
fun Activity.hideKeyboard() {
    val view = this.currentFocus
    if (view != null) {
        val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

/**
 * Converts dp unit to equivalent pixels, depending on device density.
 *
 * @param dp A value in dp to convert to pixels
 * @param context Context to get resources and device specific display metrics
 * @return A float value to represent px equivalent to dp depending on device density
 */
private fun convertDpToPixel(dp: Float, context: Context): Float {
    val resources = context.resources
    val metrics = resources.displayMetrics
    return dp * (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}

/**
 * Returns a properly padded FrameLayout which wraps a [View]. Once wrapped,
 * the view will conform to the Material Design guidelines for spacing within a Dialog.
 *
 * @param view A [View] that you wish to wrap
 * @return A correctly padded FrameLayout containing the AppCompatEditText
 */
fun Context.getAlertDialogPaddedView(view: View?): FrameLayout {
    val frameLayout = FrameLayout(this)
    val params = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )
    val marginInPixels = convertDpToPixel(20f, this).toInt()
    params.setMargins(marginInPixels, 0, marginInPixels, 0)
    frameLayout.addView(view, params)
    return frameLayout
}

/**
 * These annotations are hidden in the Android Jar for some reason. Defining them here instead
 * for use in View interfaces etc.
 */
@IntDef(View.VISIBLE, View.INVISIBLE, View.GONE)
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
annotation class Visibility
