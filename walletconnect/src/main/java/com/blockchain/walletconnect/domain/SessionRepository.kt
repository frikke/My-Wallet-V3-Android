package com.blockchain.walletconnect.domain

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface SessionRepository {
    fun contains(session: WalletConnectSession): Single<Boolean>
    fun store(session: WalletConnectSession): Completable
    fun remove(session: WalletConnectSession): Completable
    fun retrieve(): Single<List<WalletConnectSession>>
    fun removeAll(): Completable
}
