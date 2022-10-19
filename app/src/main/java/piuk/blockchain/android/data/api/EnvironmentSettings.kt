package piuk.blockchain.android.data.api

import com.blockchain.bitpay.BITPAY_LIVE_BASE
import com.blockchain.enviroment.Environment
import com.blockchain.enviroment.EnvironmentConfig
import piuk.blockchain.android.BuildConfig

class EnvironmentSettings : EnvironmentConfig {

    override fun isRunningInDebugMode(): Boolean = BuildConfig.DEBUG

    override fun isCompanyInternalBuild(): Boolean = BuildConfig.INTERNAL

    override fun isAlphaBuild(): Boolean = BuildConfig.ALPHA

    override val environment: Environment = Environment.fromString(BuildConfig.ENVIRONMENT)!!

    override val apiUrl: String = BuildConfig.API_URL
    override val everypayHostUrl: String = BuildConfig.EVERYPAY_HOST_URL
    override val statusUrl: String = BuildConfig.STATUS_API_URL

    override val bitpayUrl: String = BITPAY_LIVE_BASE
    override val applicationId: String = BuildConfig.APPLICATION_ID
}
