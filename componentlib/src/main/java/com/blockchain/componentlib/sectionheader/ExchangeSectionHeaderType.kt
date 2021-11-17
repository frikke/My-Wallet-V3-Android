package com.blockchain.componentlib.sectionheader

import com.blockchain.componentlib.image.ImageResource

sealed class ExchangeSectionHeaderType(val title: String) {

    class Default(title: String) : ExchangeSectionHeaderType(title)

    class Icon(
        title: String,
        val icon: ImageResource,
        val onIconClicked: () -> Unit,
    ) : ExchangeSectionHeaderType(title)

    class Filter(
        title: String,
        val options: List<String>,
        val onOptionSelected: (index: Int) -> Unit,
        val optionIndexSelected: Int,
    ) : ExchangeSectionHeaderType(title)
}
