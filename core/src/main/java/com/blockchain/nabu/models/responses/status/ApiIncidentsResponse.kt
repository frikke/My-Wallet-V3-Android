package com.blockchain.nabu.models.responses.status

import kotlinx.serialization.Serializable

@Serializable
class ApiIncidentsResponse(val incidents: List<Incident>)

@Serializable
class Incident(val components: List<Component>)

@Serializable
class Component(val name: String, val status: String) {
    companion object {
        const val WALLET = "Wallet"
        const val OPERATIONAL = "Operational"
    }
}
