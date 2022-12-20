package com.blockchain.presentation.rx

import com.blockchain.rx.IMainScheduler
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler

object AndroidMainScheduler : IMainScheduler {
    override fun main(): Scheduler = AndroidSchedulers.mainThread()
}
