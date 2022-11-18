package com.blockchain.componentlib.utils

import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

/**
 * Text can either come as string or resource with args
 */
sealed interface TextValue {
    data class StringValue(val value: String) : TextValue
    data class IntResValue(
        @StringRes val value: Int,
        val args: List<Any> = emptyList()
    ) : TextValue
}

@Composable
fun TextValue.value(): String {
    return when (this) {
        is TextValue.IntResValue -> stringResource(
            value,
            *(
                args.map {
                    when (it) {
                        is Int -> {
                            LocalContext.current.getStringMaybe(it)
                        }
                        else -> it.toString()
                    }
                }.toTypedArray()
                )
        )
        is TextValue.StringValue -> value
    }
}

fun Context.getStringMaybe(resId: Int): String {
    return try {
        getString(resId)
    } catch (e: Resources.NotFoundException) {
        resId.toString()
    }
}
