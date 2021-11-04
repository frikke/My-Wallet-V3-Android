package com.blockchain.componentlib.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val Blue600 = Color(0XFF0C6CF2)
private val Blue400 = Color(0XFF619FF7)
private val Blue000 = Color(0XFFECF5FE)

private val Green600 = Color(0XFF00994C)
private val Green400 = Color(0XFF17CE73)
private val Green100 = Color(0XFFD1F0DB)

private val Red600 = Color(0XFFCF1726)
private val Red400 = Color(0XFFFF3344)
private val Red100 = Color(0XFFFFD9D6)

private val Orange600 = Color(0XFFD46A00)
private val Orange400 = Color(0XFFFFA133)
private val Orange100 = Color(0XFFFFECD6)

private val Grey900 = Color(0XFF121D33)
private val Grey800 = Color(0XFF353F52)
private val Grey700 = Color(0XFF50596B)
private val Grey600 = Color(0XFF677184)
private val Grey500 = Color(0XFF828B9E)
private val Grey400 = Color(0XFF98A1B2)
private val Grey300 = Color(0XFFB1B8C7)
private val Grey200 = Color(0XFFCCD2DE)
private val Grey100 = Color(0XFFDFE3EB)
private val Grey000 = Color(0XFFF0F2F7)

private val Dark900 = Color(0XFF0E121B)
private val Dark800 = Color(0XFF20242C)
private val Dark700 = Color(0XFF2C3038)
private val Dark600 = Color(0XFF3B3E46)
private val Dark500 = Color(0XFF4D515B)
private val Dark400 = Color(0XFF63676F)
private val Dark300 = Color(0XFF797D84)
private val Dark200 = Color(0XFF989BA1)
private val Dark100 = Color(0XFFB8B9BD)
private val Dark000 = Color(0XFFD2D4D6)

private val TierGold = Color(0XFFF5B73D)
private val TierSilver = Color(0XFFC2C9D6)

private val Overlay800 = Dark800.copy(0.8f)
private val Overlay600 = Dark600.copy(0.64f)
private val Overlay400 = Dark400.copy(0.4f)

fun getLightColors() = SemanticColors(
    title = Grey900,
    body = Grey800,
    overlay = Overlay600,
    muted = Grey400,
    dark = Grey300,
    medium = Grey100,
    light = Grey000,
    background = Color.White,
    primary = Blue600,
    success = Green600,
    warning = Orange600,
    error = Red600,

    tagDefaultBackground = Grey000,
    tagDefaultText = Grey900,
    tagInfoAltBackground = Blue000,
    tagInfoAltText = Blue600,
    tagSuccessBackground = Green100,
    tagSuccessText = Green600,
    tagWarningBackground = Orange100,
    tagWarningText = Orange600,
    tagErrorBackground = Red100,
    tagErrorText = Red600,

    isLight = true
)

fun getDarkColors() = SemanticColors(
    title = Color.White,
    body = Dark200,
    overlay = Overlay600,
    muted = Dark400,
    dark = Dark700,
    medium = Dark600,
    light = Dark800,
    background = Dark900,
    primary = Blue400,
    success = Green400,
    warning = Orange400,
    error = Red400,

    tagDefaultBackground = Dark600,
    tagDefaultText = Color.White,
    tagInfoAltBackground = Dark600,
    tagInfoAltText = Blue400,
    tagSuccessBackground = Green400,
    tagSuccessText = Dark900,
    tagWarningBackground = Orange400,
    tagWarningText = Dark900,
    tagErrorBackground = Red400,
    tagErrorText = Dark900,

    isLight = false
)

val LocalColors = staticCompositionLocalOf { getLightColors() }
