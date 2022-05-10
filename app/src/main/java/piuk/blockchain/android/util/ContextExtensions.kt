package piuk.blockchain.android.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import piuk.blockchain.android.R

fun Context.loadInterMedium(): Typeface =
    ResourcesCompat.getFont(this, R.font.inter_medium)!!

fun Context.loadInterSemibold(): Typeface =
    ResourcesCompat.getFont(this, R.font.inter_semibold)!!

fun Context.getResolvedColor(@ColorRes color: Int): Int = ContextCompat.getColor(this, color)

/**
 * Returns a color associated with a particular resource ID.
 *
 * @param color The Res ID of the color.
 */
fun Fragment.getResolvedColor(@ColorRes color: Int): Int =
    ContextCompat.getColor(requireContext(), color)

/**
 * Returns a nullable Drawable associated with a particular resource ID.
 *
 * @param drawable The Res ID of the Drawable.
 */
fun Context.getResolvedDrawable(@DrawableRes drawable: Int): Drawable? =
    ContextCompat.getDrawable(this, drawable)

fun Context?.openUrl(url: String) {
    openUrl(Uri.parse(url))
}

fun Context?.openUrl(url: Uri) {
    this?.run { startActivity(Intent(Intent.ACTION_VIEW, url)) }
}

fun Context.copyToClipboard(label: String, text: String) {
    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).apply {
        ClipData.newPlainText(label, text).also { clipData ->
            setPrimaryClip(clipData)
        }
    }
}

fun Context.shareText(text: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, null)
    startActivity(shareIntent)
}
