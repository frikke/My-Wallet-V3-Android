package com.blockchain.rx

import io.reactivex.rxjava3.core.Scheduler
import org.koin.core.component.KoinComponent
import org.koin.java.KoinJavaComponent.get

interface IMainScheduler {
    fun main(): Scheduler
}

object MainRxJavaPlugins {
    @get:Synchronized
    @set:Synchronized
    var overriddenMain: Scheduler? = null

    fun reset() {
        overriddenMain = null
    }
}

object MainScheduler :
    KoinComponent,
    IMainScheduler by try {
        val overriddenMainScheduler = MainRxJavaPlugins.overriddenMain?.let { scheduler ->
            object : IMainScheduler {
                override fun main(): Scheduler = scheduler
            }
        }
        overriddenMainScheduler ?: get(IMainScheduler::class.java)
    } catch (ex: Exception) {
        throw Exception("Seems like you forgot rxInit in the Test", ex)
    }
