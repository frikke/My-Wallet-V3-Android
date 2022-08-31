package com.blockchain.addressverification.data.mapper

import com.blockchain.addressverification.domain.model.AutocompleteAddress
import com.blockchain.addressverification.domain.model.AutocompleteAddressType
import com.blockchain.addressverification.domain.model.CompleteAddress
import com.blockchain.api.addressverification.model.AutocompleteAddressDto
import com.blockchain.api.addressverification.model.AutocompleteAddressResponse
import com.blockchain.api.addressverification.model.CompleteAddressDto
import com.blockchain.extensions.enumValueOfOrNull

fun AutocompleteAddressResponse.toDomain(): List<AutocompleteAddress> = addresses.map {
    it.toDomain()
}

fun CompleteAddressDto.toDomain(): CompleteAddress = CompleteAddress(
    id = id,
    domesticId = domesticId,
    language = language,
    languageAlternatives = languageAlternatives,
    department = department,
    company = company,
    subBuilding = subBuilding,
    buildingNumber = buildingNumber,
    buildingName = buildingName,
    secondaryStreet = secondaryStreet,
    street = street,
    block = block,
    neighbourhood = neighbourhood,
    district = district,
    city = city,
    line1 = line1,
    line2 = line2,
    line3 = line3,
    line4 = line4,
    line5 = line5,
    adminAreaName = adminAreaName,
    adminAreaCode = adminAreaCode,
    province = province,
    provinceName = provinceName,
    provinceCode = provinceCode,
    postalCode = postalCode,
    countryName = countryName,
    countryIso2 = countryIso2,
    countryIso3 = countryIso3,
    countryIsoNumber = countryIsoNumber,
    sortingNumber1 = sortingNumber1,
    sortingNumber2 = sortingNumber2,
    barcode = barcode,
    poBoxNumber = poBoxNumber,
    label = label,
    type = type,
    dataLevel = dataLevel,
)

fun AutocompleteAddressDto.toDomain(): AutocompleteAddress {
    val (titleHighlightRangesRaw, descriptionHighlightRangesRaw) = highlight.highlightStringToRanges()

    // This is needed because Loqate will happily send us out of range highlights
    val titleHighlightRanges = titleHighlightRangesRaw
        .filter { range -> range.start < range.endInclusive }
        .map { range ->
            IntRange(range.start.coerceIn(text.indices), range.endInclusive.coerceIn(text.indices))
        }

    val descriptionHighlightRanges = descriptionHighlightRangesRaw
        .filter { range -> range.start < range.endInclusive }
        .map { range ->
            IntRange(range.start.coerceIn(description.indices), range.endInclusive.coerceIn(description.indices))
        }

    // <editor-fold desc="Editor Fold: TODO(aromano): move this logic to backend">
    val addressType = type.typeToDomain()
    var newDescription: String = description
    var containedAddressesCount: Int? = null

    if (addressType != AutocompleteAddressType.ADDRESS) {
        val addressesRegex = " - [0-9]+ Address".toRegex()
        val countRegex = "[0-9]+".toRegex()

        val addressesString = addressesRegex.find(description)
        if (addressesString != null) {
            val count = countRegex.find(addressesString.value)
            if (count != null) {
                containedAddressesCount = count.value.toIntOrNull()
                newDescription = description.substring(0, addressesString.range.start)
            }
        }
    }
    // </editor-fold>

    return AutocompleteAddress(
        id = id,
        type = addressType,
        title = text,
        titleHighlightRanges = titleHighlightRanges,
        description = newDescription,
        descriptionHighlightRanges = descriptionHighlightRanges,
        containedAddressesCount = containedAddressesCount
    )
}

private fun String.typeToDomain(): AutocompleteAddressType =
    enumValueOfOrNull<AutocompleteAddressType>(this, ignoreCase = true) ?: AutocompleteAddressType.OTHER

private fun String.highlightStringToRanges(): Pair<List<IntRange>, List<IntRange>> = try {
    // Original: "0-2,6-8;0-3"
    val groupString = this.split(";") // ["0-2,6-8", "0-3"]
    val titleGroup = groupString.getOrNull(0) // "0-2,6-8"
    val descriptionGroup = groupString.getOrNull(1) // "0-3"

    val titleRangesString = titleGroup?.split(",") // ["0-2", "6-8"]
    val descriptionRangesString = descriptionGroup?.split(",") // ["0-3"]

    val titleRanges = titleRangesString?.map { rangeString ->
        val raw = rangeString.split("-")
        val start = raw[0].toInt()
        val end = raw[1].toInt()
        start until end
    } ?: emptyList()

    val descriptionRanges = descriptionRangesString?.map { rangeString ->
        val raw = rangeString.split("-")
        val start = raw[0].toInt()
        val end = raw[1].toInt()
        start until end
    } ?: emptyList()

    titleRanges to descriptionRanges
} catch (ex: Exception) {
    emptyList<IntRange>() to emptyList()
}
