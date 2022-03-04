package com.blockchain.componentlib.sectionheader

import com.blockchain.componentlib.basic.ImageResource

sealed class LargeSectionHeaderType(val title: String) {

    class Default(title: String) : LargeSectionHeaderType(title)

    class Icon(
        title: String,
        val icon: ImageResource,
        val onIconClicked: () -> Unit,
    ) : LargeSectionHeaderType(title)

    class Filter(
        title: String,
        val options: List<String>,
        val onOptionSelected: (index: Int) -> Unit,
        val optionIndexSelected: Int,
    ) : LargeSectionHeaderType(title)
}
