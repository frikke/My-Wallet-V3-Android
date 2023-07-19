package com.blockchain.componentlib.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

class SemanticColors(
    title: Color,
    titleSecondary: Color,
    body: Color,
    muted: Color,
    dark: Color,
    semidark: Color,
    medium: Color,
    light: Color,
    background: Color,
    backgroundSecondary: Color,
    alertBackground: Color,
    primary: Color,
    primaryMuted: Color,
    primaryLight: Color,
    success: Color,
    successMuted: Color,
    warning: Color,
    warningMuted: Color,
    warningLight: Color,
    error: Color,
    errorMuted: Color,
    errorLight: Color,
    negative: Color,
    negativeMuted: Color,
    explorer: Color,
    scrim: Color,
    backgroundCustodialStart: Color,
    backgroundCustodialEnd: Color,
    backgroundDefiStart: Color,
    backgroundDefiEnd: Color,
    isLight: Boolean
) {

    var title by mutableStateOf(title)
        private set
    var titleSecondary by mutableStateOf(titleSecondary)
        private set
    var body by mutableStateOf(body)
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
    var alertBackground by mutableStateOf(alertBackground)
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
    var explorer by mutableStateOf(explorer)
        private set
    var scrim by mutableStateOf(scrim)
        private set
    var backgroundCustodialStart by mutableStateOf(backgroundCustodialStart)
        private set
    var backgroundCustodialEnd by mutableStateOf(backgroundCustodialEnd)
        private set
    var backgroundDefiStart by mutableStateOf(backgroundDefiStart)
        private set
    var backgroundDefiEnd by mutableStateOf(backgroundDefiEnd)
        private set
    var isLight by mutableStateOf(isLight)
        private set

    fun copy(
        title: Color = this.title,
        titleSecondary: Color = this.titleSecondary,
        body: Color = this.body,
        muted: Color = this.muted,
        dark: Color = this.dark,
        medium: Color = this.medium,
        light: Color = this.light,
        background: Color = this.backgroundSecondary,
        backgroundMuted: Color = this.background,
        alertBackground: Color = this.alertBackground,
        primary: Color = this.primary,
        primaryMuted: Color = this.primaryMuted,
        primaryLight: Color = this.primaryLight,
        success: Color = this.success,
        successMuted: Color = this.successMuted,
        warning: Color = this.warning,
        warningMuted: Color = this.warningMuted,
        warningLight: Color = this.warningLight,
        error: Color = this.error,
        errorMuted: Color = this.errorMuted,
        errorLight: Color = this.errorLight,
        negative: Color = this.negative,
        negativeMuted: Color = this.negativeMuted,
        explorer: Color = this.explorer,
        scrim: Color = this.scrim,
        backgroundCustodialStart: Color = this.backgroundCustodialStart,
        backgroundCustodialEnd: Color = this.backgroundCustodialEnd,
        backgroundDefiStart: Color = this.backgroundDefiStart,
        backgroundDefiEnd: Color = this.backgroundDefiEnd,
        isLight: Boolean = this.isLight
    ) = SemanticColors(
        title = title,
        titleSecondary = titleSecondary,
        body = body,
        muted = muted,
        dark = dark,
        medium = medium,
        light = light,
        semidark = semidark,
        backgroundSecondary = background,
        background = backgroundMuted,
        alertBackground = alertBackground,
        primary = primary,
        primaryMuted = primaryMuted,
        primaryLight = primaryLight,
        success = success,
        successMuted = successMuted,
        warning = warning,
        warningMuted = warningMuted,
        warningLight = warningLight,
        error = error,
        errorMuted = errorMuted,
        errorLight = errorLight,
        negative = negative,
        negativeMuted = negativeMuted,
        explorer = explorer,
        scrim = scrim,
        backgroundCustodialStart = backgroundCustodialStart,
        backgroundCustodialEnd = backgroundCustodialEnd,
        backgroundDefiStart = backgroundDefiStart,
        backgroundDefiEnd = backgroundDefiEnd,
        isLight = isLight,
    )

    fun updateColorsFrom(colors: SemanticColors) {
        title = colors.title
        titleSecondary = colors.titleSecondary
        body = colors.body
        muted = colors.muted
        dark = colors.dark
        medium = colors.medium
        light = colors.light
        backgroundSecondary = colors.backgroundSecondary
        background = colors.background
        alertBackground = colors.alertBackground
        primary = colors.primary
        primaryMuted = colors.primaryMuted
        primaryLight = colors.primaryLight
        success = colors.success
        successMuted = colors.successMuted
        warning = colors.warning
        warningMuted = colors.warningMuted
        warningLight = colors.warningLight
        error = colors.error
        errorMuted = colors.errorMuted
        errorLight = colors.errorLight
        negative = colors.negative
        negativeMuted = colors.negativeMuted
        explorer = colors.explorer
        scrim = colors.scrim
        backgroundCustodialStart = colors.backgroundCustodialStart
        backgroundCustodialEnd = colors.backgroundCustodialEnd
        backgroundDefiStart = colors.backgroundDefiStart
        backgroundDefiEnd = colors.backgroundDefiEnd

        isLight = colors.isLight
    }
}
