package com.blockchain.presentation.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.utils.previewAnalytics
import org.koin.androidx.compose.get

@Composable
fun analyticsProvider(): Analytics {
    var analytics: Analytics by remember { mutableStateOf(previewAnalytics) }
    if (!LocalInspectionMode.current) {
        analytics = get()
    }
    return analytics
}
