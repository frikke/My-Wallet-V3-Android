package com.blockchain.enviroment

interface EnvironmentConfig : EnvironmentUrls {
    val environment: Environment
    val bitpayUrl: String

    fun isRunningInDebugMode(): Boolean

    fun isCompanyInternalBuild(): Boolean
}
