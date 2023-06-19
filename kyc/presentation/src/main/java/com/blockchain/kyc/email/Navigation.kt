package com.blockchain.kyc.email

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent

sealed class Navigation : NavigationEvent {
    data class EditEmailSheet(val currentEmail: String) : Navigation()
}
