package com.blockchain.testutils

import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.mp.KoinPlatformTools

/**
 * Copy of org.koin.test.KoinTestRule, which we can't seem to use because it's compiled for JAVA 11
 */
class KoinTestRule private constructor(private val appDeclaration: KoinAppDeclaration) : TestWatcher() {

    private var _koin: Koin? = null
    val koin: Koin
        get() = _koin ?: error("No Koin application found")

    override fun starting(description: Description?) {
        closeExistingInstance()
        _koin = startKoin(appDeclaration = appDeclaration).koin
        koin.logger.info("Koin Rule - starting")
    }

    private fun closeExistingInstance() {
        KoinPlatformTools.defaultContext().getOrNull()?.let { koin ->
            koin.logger.info("Koin Rule - closing existing instance")
            koin.close()
        }
    }

    override fun finished(description: Description?) {
        koin.logger.info("Koin Rule - finished")
        stopKoin()
        _koin = null
    }

    companion object {
        fun create(appDeclaration: KoinAppDeclaration = {}): KoinTestRule {
            return KoinTestRule(appDeclaration)
        }
    }
}
