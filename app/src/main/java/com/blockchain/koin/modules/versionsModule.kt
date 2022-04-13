package com.blockchain.koin.modules

import android.os.Build
import com.blockchain.koin.currentAppVersionCode
import com.blockchain.koin.currentOsVersion
import org.koin.dsl.module
import piuk.blockchain.android.BuildConfig

val versionsModule = module {
    single(currentAppVersionCode) {
        BuildConfig.VERSION_CODE
    }

    single(currentOsVersion) {
        Build.VERSION.SDK_INT
    }
}
