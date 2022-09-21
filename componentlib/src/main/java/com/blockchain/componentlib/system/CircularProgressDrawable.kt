package com.blockchain.componentlib.system

import android.content.Context
import com.blockchain.componentlib.R
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec
import com.google.android.material.progressindicator.IndeterminateDrawable

fun CircularProgressDrawable(context: Context) = IndeterminateDrawable.createCircularDrawable(
    context,
    CircularProgressIndicatorSpec(
        context,
        null,
        0,
        R.style.CircularProgressIndicator
    )
)
