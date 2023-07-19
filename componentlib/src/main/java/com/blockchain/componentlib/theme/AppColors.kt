package com.blockchain.componentlib.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val Grey900 = Color(0XFF121D33)
val Grey800 = Color(0XFF353F52)
val Grey700 = Color(0XFF50596B)
val Grey600 = Color(0XFF677184)
val Grey500 = Color(0XFF828B9E)
val Grey400 = Color(0XFF98A1B2)
val Grey300 = Color(0XFFB1B8C7)
val Grey200 = Color(0XFFCCD2DE)
val Grey100 = Color(0XFFDFE3EB)
val Grey000 = Color(0XFFF0F2F7)

val White = Color(0xFFFFFFFF)

val Dark900 = Color(0XFF0E121B)
val Dark800 = Color(0XFF20242C)
val Dark700 = Color(0XFF2C3038)
val Dark600 = Color(0XFF3B3E46)
val Dark500 = Color(0XFF4D515B)
val Dark400 = Color(0XFF63676F)
val Dark300 = Color(0XFF797D84)
val Dark200 = Color(0XFF989BA1)
val Dark100 = Color(0XFFB8B9BD)
val Dark000 = Color(0XFFD2D4D6)

val Overlay800 = Dark800.copy(0.8f)
val Overlay600 = Dark600.copy(0.64f)
val Overlay400 = Dark400.copy(0.4f)

val UltraLight = Color(0XFFFAFBFF)
val CowboysDark = Color(0XFF07080D)

val LocalLightColors = compositionLocalOf { defLightColors }
val LocalDarkColors = compositionLocalOf { defDarkColors }
