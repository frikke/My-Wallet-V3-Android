package com.blockchain.componentlib.theme

import kotlinx.coroutines.flow.Flow

interface AppThemeProvider {
    val appTheme: Flow<Theme>
}
