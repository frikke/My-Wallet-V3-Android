package com.blockchain.componentlib.utils

import android.content.Context
import android.content.res.Configuration
import com.blockchain.componentlib.theme.SemanticColors
import com.blockchain.componentlib.theme.defDarkColors
import com.blockchain.componentlib.theme.defLightColors

// This class is here to help the ViewSystem adapt without composables; note this means changes made won't update values
// as they do in the compose world
object ViewSystemUtils {

    fun getSemanticColors(context: Context): SemanticColors {
        val uiMode = context.resources.configuration.uiMode
        val isInDarkMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        return if (isInDarkMode) defDarkColors else defLightColors
    }
}
