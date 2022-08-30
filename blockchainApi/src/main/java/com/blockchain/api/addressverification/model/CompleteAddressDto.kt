package com.blockchain.api.addressverification.model

import kotlinx.serialization.Serializable

@Serializable
data class CompleteAddressDto(
    val id: String,
    val domesticId: String,
    val language: String,
    val languageAlternatives: String,
    val department: String,
    val company: String,
    val subBuilding: String,
    val buildingNumber: String,
    val buildingName: String,
    val secondaryStreet: String,
    val street: String,
    val block: String,
    val neighbourhood: String,
    val district: String,
    val city: String,
    val line1: String,
    val line2: String,
    val line3: String,
    val line4: String,
    val line5: String,
    val adminAreaName: String,
    val adminAreaCode: String,
    val province: String,
    val provinceName: String,
    val provinceCode: String,
    val postalCode: String,
    val countryName: String,
    val countryIso2: String,
    val countryIso3: String,
    val countryIsoNumber: String,
    val sortingNumber1: String,
    val sortingNumber2: String,
    val barcode: String,
    val poBoxNumber: String,
    val label: String,
    val type: String,
    val dataLevel: String
)
