package com.blockchain.componentlib.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

private val Title = Color(0XFF121D33)
private val TitleNight = Color(0XFFFFFFFF)
private val TitleSecondary = Color(0XFFFFFFFF)
private val TitleSecondaryNight = Color(0XFF121D33)
private val Body = Color(0XFF50596B)
private val BodyNight = Color(0XFF989BA1)
private val Muted = Color(0XFF98A1B2)
private val MutedNight = Color(0XFF63676F)

private val Primary = Color(0XFF0C6CF2)
private val PrimaryNight = Color(0XFF65A5FF)
private val PrimaryMuted = Color(0XFF65A5FF)
private val PrimaryMutedNight = Color(0XFF1656B9)
private val PrimaryLight = Color(0XFFECF5FE)
private val PrimaryLightNight = Color(0XFF65A5FF) // missing
private val Light = Color(0XFFF0F2F7)
private val LightNight = Color(0XFF2C3038)
private val Dark = Color(0XFFB1B8C7)
private val DarkNight = Color(0XFF98A1B2)
private val SemiDark = Color(0XFF98A1B2)
private val SemiDarkNight = Color(0XFF353F52)
private val Medium = Color(0XFFDFE3EB)
private val MediumNight = Color(0XFF3B3E46)
private val Success = Color(0XFF00B083)
private val SuccessNight = Color(0XFF69ECCA)
private val SuccessMuted = Color(0XFF69ECCA)
private val SuccessMutedNight = Color(0XFF69ECCA) // missing
private val Negative = Color(0XFFF00699)
private val NegativeNight = Color(0XFFFF55B8)
private val Warning = Color(0XFFD46A00)
private val WarningNight = Color(0XFFFFA133)
private val WarningMuted = Color(0XFFFFA133)
private val WarningMutedNight = Color(0XFFFFA133) // missing
private val WarningLight = Color(0XFFFFECD6)
private val WarningLightNight = Color(0XFFFFECD6) // missing
private val Error = Color(0XFFCF1726)
private val ErrorNight = Color(0XFFFF3344)
private val ErrorLight = Color(0XFFFFECEB)
private val ErrorLightNight = Color(0XFF790606)
private val ErrorMuted = Color(0XFFFF3344)
private val ErrorMutedNight = Color(0XFFFF3344) // missing

private val ExplorerLight = Color(0XFF5322E5)
private val ExplorerNight = Color(0XFF9080FF) // missing

private val Background = Color(0XFFF1F2F7)
private val BackgroundNight = Color(0XFF07080D)
private val BackgroundSecondary = Color(0XFFFFFFFF)
private val BackgroundSecondaryNight = Color(0XFF20242C)

private val AlertBackground = Color(0XFF20242C)
private val AlertBackgroundNight = Color(0XFF000000)

private val Scrim = Color(0XA3121D33)
private val ScrimNight = Color(0XCC121D33)

private val BackgroundCustodialStart = Color(0XFFFF0095)
private val BackgroundCustodialStartNight = Color(0XFFFF0095)
private val BackgroundCustodialEnd = Color(0XFF7C33B9)
private val BackgroundCustodialEndNight = Color(0XFF7C33B9)
private val BackgroundDefiStart = Color(0XFF7137BB)
private val BackgroundDefiStartNight = Color(0XFF7137BB)
private val BackgroundDefiEnd = Color(0XFF2960D0)
private val BackgroundDefiEndNight = Color(0XFF2960D0)

val defLightColors = SemanticColors(
    title = Title, //
    titleSecondary = TitleSecondary, //
    body = Body, //
    muted = Muted, //
    dark = Dark, //
    medium = Medium, //
    light = Light, //
    background = Background, //
    backgroundSecondary = BackgroundSecondary, //
    alertBackground = AlertBackground, //
    primary = Primary, //
    primaryMuted = PrimaryMuted, //
    primaryLight = PrimaryLight, // missing dark
    success = Success, //
    successMuted = SuccessMuted,
    warning = Warning, //
    warningMuted = WarningMuted, // missing dark
    warningLight = WarningLight, // missing dark
    error = Error, // missing dark
    errorMuted = ErrorMuted,
    errorLight = ErrorLight,
    negative = Negative, //
    negativeMuted = Negative,
    semidark = SemiDark,
    explorer = ExplorerLight,
    scrim = Scrim, //
    backgroundCustodialStart = BackgroundCustodialStart, //
    backgroundCustodialEnd = BackgroundCustodialEnd, //
    backgroundDefiStart = BackgroundDefiStart, //
    backgroundDefiEnd = BackgroundDefiEnd, //
    isLight = true
)

val defDarkColors = SemanticColors(
    title = TitleNight,
    titleSecondary = TitleSecondaryNight,
    body = BodyNight,
    muted = MutedNight,
    dark = DarkNight,
    medium = MediumNight,
    light = LightNight,
    background = BackgroundNight,
    backgroundSecondary = BackgroundSecondaryNight,
    alertBackground = AlertBackgroundNight,
    primary = PrimaryNight,
    primaryMuted = PrimaryMutedNight,
    primaryLight = PrimaryLightNight,
    success = SuccessNight,
    successMuted = SuccessMutedNight,
    warning = WarningNight,
    warningMuted = WarningMutedNight,
    warningLight = WarningLightNight,
    error = ErrorNight,
    errorMuted = ErrorMutedNight,
    errorLight = ErrorLightNight,
    negative = NegativeNight,
    negativeMuted = NegativeNight,
    semidark = SemiDarkNight,
    explorer = ExplorerNight,
    scrim = ScrimNight,
    backgroundCustodialStart = BackgroundCustodialStartNight,
    backgroundCustodialEnd = BackgroundCustodialEndNight,
    backgroundDefiStart = BackgroundDefiStartNight,
    backgroundDefiEnd = BackgroundDefiEndNight,
    isLight = false,
)

val LocalLightColors = compositionLocalOf { defLightColors }
val LocalDarkColors = compositionLocalOf { defDarkColors }
