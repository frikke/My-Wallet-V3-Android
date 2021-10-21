package com.blockchain.componentlib.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

class SemanticColors(
    primary: Color,
    success: Color,
    warning: Color,
    error: Color,
    title: Color,
    body: Color,
    muted: Color,
    dark: Color,
    medium: Color,
    light: Color,
    isLight: Boolean
) {
    var primary by mutableStateOf(primary)
        private set
    var success by mutableStateOf(success)
        private set
    var warning by mutableStateOf(warning)
        private set
    var error by mutableStateOf(error)
        private set
    var title by mutableStateOf(title)
        private set
    var body by mutableStateOf(body)
        private set
    var muted by mutableStateOf(muted)
        private set
    var dark by mutableStateOf(dark)
        private set
    var medium by mutableStateOf(medium)
        private set
    var light by mutableStateOf(light)
        private set
    var isLight by mutableStateOf(isLight)
        private set

    fun copy(
        primary: Color = this.primary,
        success: Color = this.success,
        warning: Color = this.warning,
        error: Color = this.error,
        title: Color = this.title,
        body: Color = this.body,
        muted: Color = this.muted,
        dark: Color = this.dark,
        medium: Color = this.medium,
        light: Color = this.light,
        isLight: Boolean = this.isLight
    ) = SemanticColors(
        primary = primary,
        success = success,
        warning = warning,
        error = error,
        title = title,
        body = body,
        muted = muted,
        dark = dark,
        medium = medium,
        light = light,
        isLight = isLight
    )

    fun updateColorsFrom(colors: SemanticColors) {
        primary = colors.primary
        success = colors.success
        warning = colors.warning
        error = colors.error
        title = colors.title
        body = colors.body
        muted = colors.muted
        dark = colors.dark
        medium = colors.medium
        light = colors.light
        isLight = colors.isLight
    }
}