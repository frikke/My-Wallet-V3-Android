package com.blockchain.logging.data

import com.blockchain.logging.ILogger
import org.jetbrains.annotations.NonNls
import timber.log.Timber

internal class TimberLogger : ILogger {

    override fun v(@NonNls message: String?, vararg args: Any?) = Timber.v(message, *args)

    override fun v(t: Throwable?, @NonNls message: String?, vararg args: Any?) = Timber.v(t, message, *args)

    override fun v(t: Throwable?) = Timber.v(t)

    override fun d(@NonNls message: String?, vararg args: Any?) = Timber.d(message, *args)

    override fun d(t: Throwable?, @NonNls message: String?, vararg args: Any?) = Timber.d(t, message, *args)

    override fun d(t: Throwable?) = Timber.d(t)

    override fun i(@NonNls message: String?, vararg args: Any?) = Timber.i(message, *args)

    override fun i(t: Throwable?, @NonNls message: String?, vararg args: Any?) = Timber.i(t, message, *args)

    override fun i(t: Throwable?) = Timber.i(t)

    override fun w(@NonNls message: String?, vararg args: Any?) = Timber.w(message, *args)

    override fun w(t: Throwable?, @NonNls message: String?, vararg args: Any?) = Timber.w(t, message, *args)

    override fun w(t: Throwable?) = Timber.w(t)

    override fun e(@NonNls message: String?, vararg args: Any?) = Timber.e(message, *args)

    override fun e(t: Throwable?, @NonNls message: String?, vararg args: Any?) = Timber.e(t, message, *args)

    override fun e(t: Throwable?) = Timber.e(t)

    override fun wtf(@NonNls message: String?, vararg args: Any?) = Timber.wtf(message, *args)

    override fun wtf(t: Throwable?, @NonNls message: String?, vararg args: Any?) = Timber.wtf(t, message, *args)

    override fun wtf(t: Throwable?) = Timber.wtf(t)
}
