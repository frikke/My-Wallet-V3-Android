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

    // The following are not defined in the design system yet
    tagDefaultBackground: Color,
    tagDefaultText: Color,
    tagInfoAltBackground: Color,
    tagInfoAltText: Color,
    tagSuccessBackground: Color,
    tagSuccessText: Color,
    tagWarningBackground: Color,
    tagWarningText: Color,
    tagErrorBackground: Color,
    tagErrorText: Color,

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

    var tagDefaultBackground by mutableStateOf(tagDefaultBackground)
        private set
    var tagDefaultText by mutableStateOf(tagDefaultText)
        private set
    var tagInfoAltBackground by mutableStateOf(tagInfoAltBackground)
        private set
    var tagInfoAltText by mutableStateOf(tagInfoAltText)
        private set
    var tagSuccessBackground by mutableStateOf(tagSuccessBackground)
        private set
    var tagSuccessText by mutableStateOf(tagSuccessText)
        private set
    var tagWarningBackground by mutableStateOf(tagWarningBackground)
        private set
    var tagWarningText by mutableStateOf(tagWarningText)
        private set
    var tagErrorBackground by mutableStateOf(tagErrorBackground)
        private set
    var tagErrorText by mutableStateOf(tagErrorText)
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

        tagDefaultBackground: Color = this.tagDefaultBackground,
        tagDefaultText: Color = this.tagDefaultText,
        tagInfoAltBackground: Color = this.tagInfoAltBackground,
        tagInfoAltText: Color = this.tagInfoAltText,
        tagSuccessBackground: Color = this.tagSuccessBackground,
        tagSuccessText: Color = this.tagSuccessText,
        tagWarningBackground: Color = this.tagWarningBackground,
        tagWarningText: Color = this.tagWarningText,
        tagErrorBackground: Color = this.tagErrorBackground,
        tagErrorText: Color = this.tagErrorText,

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

        tagDefaultBackground = tagDefaultBackground,
        tagDefaultText = tagDefaultText,
        tagInfoAltBackground = tagInfoAltBackground,
        tagInfoAltText = tagInfoAltText,
        tagSuccessBackground = tagSuccessBackground,
        tagSuccessText = tagSuccessText,
        tagWarningBackground = tagWarningBackground,
        tagWarningText = tagWarningText,
        tagErrorBackground = tagErrorBackground,
        tagErrorText = tagErrorText,

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

        tagDefaultBackground = colors.tagDefaultBackground
        tagDefaultText = colors.tagDefaultText
        tagInfoAltBackground = colors.tagInfoAltBackground
        tagInfoAltText = colors.tagInfoAltText
        tagSuccessBackground = colors.tagSuccessBackground
        tagSuccessText = colors.tagSuccessText
        tagWarningBackground = colors.tagWarningBackground
        tagWarningText = colors.tagWarningText
        tagErrorBackground = colors.tagErrorBackground
        tagErrorText = colors.tagErrorText

        isLight = colors.isLight
    }
}
