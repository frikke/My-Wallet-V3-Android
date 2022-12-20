package com.blockchain.enviroment

interface EnvironmentConfig : EnvironmentUrls {
    val environment: Environment
    val bitpayUrl: String
    val applicationId: String

    fun isRunningInDebugMode(): Boolean

    fun isCompanyInternalBuild(): Boolean

    fun isAlphaBuild(): Boolean
}
