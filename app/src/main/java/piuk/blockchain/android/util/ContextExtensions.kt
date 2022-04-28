package piuk.blockchain.android.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import java.io.File
import piuk.blockchain.android.R
import piuk.blockchain.android.fileutils.domain.utils.getMimeType

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

fun Context.getFilePath(fileName: String, extension: String = "") =
    "$filesDir${File.separator}$fileName${if (extension.isNotBlank()) "." else ""}$extension"

fun Context?.openPdfFile(file: File) {
    this?.run {
        val contentUri = FileProvider.getUriForFile(this, "$packageName.fileProvider", file)

        val target = Intent(Intent.ACTION_VIEW)
        target.setDataAndType(contentUri, getMimeType(file))
        target.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY

        try {
            startActivity(target)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }
}
