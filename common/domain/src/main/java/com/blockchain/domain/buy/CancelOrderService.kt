package com.blockchain.domain.buy

import io.reactivex.rxjava3.core.Completable

interface CancelOrderService {
    fun cancelOrder(orderId: String): Completable
}
