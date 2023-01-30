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
    ) : TextValue {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as IntResValue
            return value == other.value && args.toSet() == other.args.toSet()
        }

        override fun hashCode(): Int {
            var result = value
            result = 31 * result + args.toSet().hashCode()
            return result
        }
    }
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
                        is TextValue -> {
                            it.value()
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
