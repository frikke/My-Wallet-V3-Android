package com.blockchain.koin.modules

import android.os.Build
import com.blockchain.versions.VersionsInfo
import org.koin.dsl.module
import piuk.blockchain.android.BuildConfig

val versionsModule = module {

    single<VersionsInfo> {
        object : VersionsInfo {
            override val versionName: String = BuildConfig.VERSION_NAME
            override val versionCode: Int = BuildConfig.VERSION_CODE
            override val osVersion: Int = Build.VERSION.SDK_INT
        }
    }
}
