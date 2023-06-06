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
    semidark: Color,
    medium: Color,
    light: Color,
    background: Color,
    backgroundSecondary: Color,
    primary: Color,
    primaryMuted: Color,
    primaryLight: Color,
    success: Color,
    successMuted: Color,
    successLight: Color,
    warning: Color,
    warningMuted: Color,
    warningLight: Color,
    error: Color,
    errorMuted: Color,
    errorLight: Color,
    negative: Color,
    negativeMuted: Color,
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
    var semidark by mutableStateOf(semidark)
        private set
    var medium by mutableStateOf(medium)
        private set
    var light by mutableStateOf(light)
        private set
    var backgroundSecondary by mutableStateOf(backgroundSecondary)
        private set
    var background by mutableStateOf(background)
        private set
    var primary by mutableStateOf(primary)
        private set
    var primaryMuted by mutableStateOf(primaryMuted)
        private set
    var primaryLight by mutableStateOf(primaryLight)
        private set
    var success by mutableStateOf(success)
        private set
    var successMuted by mutableStateOf(successMuted)
        private set
    var successLight by mutableStateOf(successLight)
        private set
    var warning by mutableStateOf(warning)
        private set
    var warningMuted by mutableStateOf(warningMuted)
        private set
    var warningLight by mutableStateOf(warningLight)
        private set
    var error by mutableStateOf(error)
        private set
    var errorMuted by mutableStateOf(errorMuted)
        private set
    var errorLight by mutableStateOf(errorLight)
        private set
    var negative by mutableStateOf(negative)
        private set
    var negativeMuted by mutableStateOf(negativeMuted)
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
        background: Color = this.backgroundSecondary,
        backgroundMuted: Color = this.background,
        primary: Color = this.primary,
        primaryMuted: Color = this.primaryMuted,
        primaryLight: Color = this.primaryLight,
        success: Color = this.success,
        successMuted: Color = this.successMuted,
        successLight: Color = this.successLight,
        warning: Color = this.warning,
        warningMuted: Color = this.warningMuted,
        warningLight: Color = this.warningLight,
        error: Color = this.error,
        errorMuted: Color = this.errorMuted,
        errorLight: Color = this.errorLight,
        negative: Color = this.negative,
        negativeMuted: Color = this.negativeMuted,
        isLight: Boolean = this.isLight
    ) = SemanticColors(
        title = title,
        body = body,
        overlay = overlay,
        muted = muted,
        dark = dark,
        medium = medium,
        light = light,
        semidark = semidark,
        backgroundSecondary = background,
        background = backgroundMuted,
        primary = primary,
        primaryMuted = primaryMuted,
        primaryLight = primaryLight,
        success = success,
        successMuted = successMuted,
        successLight = successLight,
        warning = warning,
        warningMuted = warningMuted,
        warningLight = warningLight,
        error = error,
        errorMuted = errorMuted,
        errorLight = errorLight,
        negative = negative,
        negativeMuted = negativeMuted,
        isLight = isLight,
    )

    fun updateColorsFrom(colors: SemanticColors) {
        title = colors.title
        body = colors.body
        overlay = colors.overlay
        muted = colors.muted
        dark = colors.dark
        medium = colors.medium
        light = colors.light
        backgroundSecondary = colors.backgroundSecondary
        background = colors.background
        primary = colors.primary
        primaryMuted = colors.primaryMuted
        primaryLight = colors.primaryLight
        success = colors.success
        successMuted = colors.successMuted
        successLight = colors.successLight
        warning = colors.warning
        warningMuted = colors.warningMuted
        warningLight = colors.warningLight
        error = colors.error
        errorMuted = colors.errorMuted
        errorLight = colors.errorLight
        negative = colors.negative
        negativeMuted = colors.negativeMuted

        isLight = colors.isLight
    }
}
