package com.blockchain.theme

interface ThemeService {
    fun currentTheme(): Theme
    fun setTheme(theme: Theme)
    fun applyCurrentTheme()
}

enum class Theme {
    LightMode,
    DarkMode,
    System
}