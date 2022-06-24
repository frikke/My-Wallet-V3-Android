package piuk.blockchain.android.ui.customviews

import android.content.Context
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec
import com.google.android.material.progressindicator.IndeterminateDrawable
import piuk.blockchain.android.R

fun CircularProgressDrawable(context: Context) = IndeterminateDrawable.createCircularDrawable(
    context,
    CircularProgressIndicatorSpec(
        context,
        null,
        0,
        R.style.CircularProgressIndicator
    )
)
