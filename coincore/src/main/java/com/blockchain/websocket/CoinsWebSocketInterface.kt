package com.blockchain.websocket

import piuk.blockchain.androidcore.data.events.ActionEvent

interface MessagesSocketHandler {
    fun triggerNotification(title: String, marquee: String, text: String)
    fun sendBroadcast(event: ActionEvent)
}

interface CoinsWebSocketInterface {
    fun subscribeToXpubBtc(xpub: String)
    fun subscribeToExtraBtcAddress(address: String)
}
