package com.blockchain.presentation.theme

import androidx.appcompat.app.AppCompatDelegate
import com.blockchain.preferences.ThemePrefs
import com.blockchain.theme.Theme
import com.blockchain.theme.ThemeService

class ThemeRepository(
    private val themePrefs: ThemePrefs
) : ThemeService {
    override fun currentTheme(): Theme {
        return try {
            Theme.valueOf(themePrefs.currentTheme)
        } catch (_: Exception) {
            Theme.System
        }
    }

    override fun setTheme(theme: Theme) {
        themePrefs.currentTheme = theme.name

        when (theme) {
            Theme.LightMode -> AppCompatDelegate.MODE_NIGHT_NO
            Theme.DarkMode -> AppCompatDelegate.MODE_NIGHT_YES
            Theme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }.run {
            AppCompatDelegate.setDefaultNightMode(this)
        }
    }

    override fun applyCurrentTheme() {
        setTheme(currentTheme())
    }
}