package com.blockchain.utils

import com.blockchain.logging.ILogger
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.mock.MockProvider
import org.koin.test.mock.declareMock
import org.mockito.Mockito

class MockLoggerKoinTestRule : TestRule, KoinTest {
    private val koinTestRule by lazy {
        KoinTestRule.create {
            declareMock<ILogger>()
        }
    }

    override fun apply(base: Statement, description: Description): Statement {
        MockProvider.register { clazz ->
            Mockito.mock(clazz.java)
        }
        return koinTestRule.apply(base, description)
    }
}
