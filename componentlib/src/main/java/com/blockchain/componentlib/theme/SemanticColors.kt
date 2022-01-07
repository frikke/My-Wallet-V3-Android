package com.blockchain.componentlib.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

class SemanticColors(
    title: Color,
    body: Color,
    overlay: Color,
    muted: Color,
    dark: Color,
    medium: Color,
    light: Color,
    background: Color,
    primary: Color,
    success: Color,
    warning: Color,
    error: Color,

    isLight: Boolean
) {

    var title by mutableStateOf(title)
        private set
    var body by mutableStateOf(body)
        private set
    var overlay by mutableStateOf(overlay)
        private set
    var muted by mutableStateOf(muted)
        private set
    var dark by mutableStateOf(dark)
        private set
    var medium by mutableStateOf(medium)
        private set
    var light by mutableStateOf(light)
        private set
    var background by mutableStateOf(background)
        private set
    var primary by mutableStateOf(primary)
        private set
    var success by mutableStateOf(success)
        private set
    var warning by mutableStateOf(warning)
        private set
    var error by mutableStateOf(error)
        private set

    var isLight by mutableStateOf(isLight)
        private set

    fun copy(
        title: Color = this.title,
        body: Color = this.body,
        overlay: Color = this.overlay,
        muted: Color = this.muted,
        dark: Color = this.dark,
        medium: Color = this.medium,
        light: Color = this.light,
        background: Color = this.background,
        primary: Color = this.primary,
        success: Color = this.success,
        warning: Color = this.warning,
        error: Color = this.error,

        isLight: Boolean = this.isLight
    ) = SemanticColors(
        title = title,
        body = body,
        overlay = overlay,
        muted = muted,
        dark = dark,
        medium = medium,
        light = light,
        background = background,
        primary = primary,
        success = success,
        warning = warning,
        error = error,

        isLight = isLight
    )

    fun updateColorsFrom(colors: SemanticColors) {
        title = colors.title
        body = colors.body
        overlay = colors.overlay
        muted = colors.muted
        dark = colors.dark
        medium = colors.medium
        light = colors.light
        background = colors.background
        primary = colors.primary
        success = colors.success
        warning = colors.warning
        error = colors.error

        isLight = colors.isLight
    }
}
