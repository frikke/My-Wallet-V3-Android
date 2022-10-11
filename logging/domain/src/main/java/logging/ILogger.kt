package com.blockchain.logging

import org.jetbrains.annotations.NonNls
import org.koin.core.component.KoinComponent
import org.koin.java.KoinJavaComponent.get

/**
 * Loggers like Android's and Timber depend on Android, so are unsuitable for plain Kotlin modules.
 * Use Timber where possible, but where not, inject and use [ILogger].
 */
interface ILogger {
    fun v(@NonNls message: String?, vararg args: Any?)
    fun v(t: Throwable?, @NonNls message: String?, vararg args: Any?)
    fun v(t: Throwable?)
    fun d(@NonNls message: String?, vararg args: Any?)
    fun d(t: Throwable?, @NonNls message: String?, vararg args: Any?)
    fun d(t: Throwable?)
    fun i(@NonNls message: String?, vararg args: Any?)
    fun i(t: Throwable?, @NonNls message: String?, vararg args: Any?)
    fun i(t: Throwable?)
    fun w(@NonNls message: String?, vararg args: Any?)
    fun w(t: Throwable?, @NonNls message: String?, vararg args: Any?)
    fun w(t: Throwable?)
    fun e(@NonNls message: String?, vararg args: Any?)
    fun e(t: Throwable?, @NonNls message: String?, vararg args: Any?)
    fun e(t: Throwable?)
    fun wtf(@NonNls message: String?, vararg args: Any?)
    fun wtf(t: Throwable?, @NonNls message: String?, vararg args: Any?)
    fun wtf(t: Throwable?)
}

object Logger : KoinComponent, ILogger by get(ILogger::class.java)

object NullLogger : ILogger {
    override fun v(@NonNls message: String?, vararg args: Any?) {}
    override fun v(t: Throwable?, @NonNls message: String?, vararg args: Any?) {}
    override fun v(t: Throwable?) {}
    override fun d(@NonNls message: String?, vararg args: Any?) {}
    override fun d(t: Throwable?, @NonNls message: String?, vararg args: Any?) {}
    override fun d(t: Throwable?) {}
    override fun i(@NonNls message: String?, vararg args: Any?) {}
    override fun i(t: Throwable?, @NonNls message: String?, vararg args: Any?) {}
    override fun i(t: Throwable?) {}
    override fun w(@NonNls message: String?, vararg args: Any?) {}
    override fun w(t: Throwable?, @NonNls message: String?, vararg args: Any?) {}
    override fun w(t: Throwable?) {}
    override fun e(@NonNls message: String?, vararg args: Any?) {}
    override fun e(t: Throwable?, @NonNls message: String?, vararg args: Any?) {}
    override fun e(t: Throwable?) {}
    override fun wtf(@NonNls message: String?, vararg args: Any?) {}
    override fun wtf(t: Throwable?, @NonNls message: String?, vararg args: Any?) {}
    override fun wtf(t: Throwable?) {}
}
