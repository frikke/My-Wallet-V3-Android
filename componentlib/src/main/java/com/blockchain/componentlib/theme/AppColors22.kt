package com.blockchain.componentlib.theme

import androidx.compose.ui.graphics.Color

private val Title = Color(0XFF121D33)
private val TitleNight = Color(0XFFFFFFFF)
private val Body = Color(0XFF50596B)
private val BodyNight = Color(0XFF989BA1)
private val Muted = Color(0XFF98A1B2)
private val MutedNight = Color(0XFF98A1B2)

private val Primary = Color(0XFF0C6CF2)
private val PrimaryNight = Color(0XFF65A5FF)
private val Light = Color(0XFFF0F2F7)
private val LightNight = Color(0XFF2C3038)
private val Dark = Color(0XFFB1B8C7)
private val DarkNight = Color(0XFF98A1B2)
private val Success = Color(0XFF00B083)
private val SuccessNight = Color(0XFF00B083)
private val Negative = Color(0XFFF00699)
private val NegativeNight = Color(0XFFFF55B8)
private val WarningMuted = Color(0XFFFFA133)
private val WarningMutedNight = Color(0XFFFFA133)
private val Error = Color(0XFFCF1726)
private val ErrorNight = Color(0XFFFF3344)

private val Background = Color(0XFFF1F2F7)
private val BackgroundNight = Color(0XFF07080D)
private val BackgroundSecondary = Color(0XFFFFFFFF)
private val BackgroundSecondaryNight = Color(0XFF20242C)

val defLightColors2 = SemanticColors(
    title = Title, //
    body = Body, //
    overlay = Overlay600,
    muted = Muted, //
    dark = Dark, //
    medium = Grey100,
    light = Light, //
    background = Background, //
    backgroundSecondary = BackgroundSecondary, //
    primary = Primary, //
    primaryMuted = Blue400,
    success = Success, //
    successMuted = Green300,
    warning = Orange600,
    warningMuted = WarningMuted, //
    error = Error, //
    errorMuted = Red400,
    negative = Negative, //
    negativeMuted = Negative,
    semidark = Grey400,
    isLight = true
)

val defDarkColors2 = SemanticColors(
    title = TitleNight,
    body = BodyNight,
    overlay = Overlay600,
    muted = MutedNight,
    dark = DarkNight,
    medium = Dark600,
    light = LightNight,
    background = BackgroundNight,
    backgroundSecondary = BackgroundSecondaryNight,
    primary = PrimaryNight,
    primaryMuted = Blue400,
    success = SuccessNight,
    successMuted = WarningMutedNight,
    warning = Orange400,
    warningMuted = Orange400,
    error = ErrorNight,
    errorMuted = Red400,
    negative = NegativeNight,
    negativeMuted = NegativeNight,
    isLight = false,
    semidark = Grey800,
)
