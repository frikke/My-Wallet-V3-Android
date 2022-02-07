package com.blockchain.componentlib.tag

data class TagViewState(
    val value: String,
    val type: TagType
)

enum class TagSize {
    Primary, Large
}

sealed class TagType(open val size: TagSize = TagSize.Primary) {
    data class Default(override val size: TagSize = TagSize.Primary) : TagType()
    data class InfoAlt(override val size: TagSize = TagSize.Primary) : TagType()
    data class Success(override val size: TagSize = TagSize.Primary) : TagType()
    data class Warning(override val size: TagSize = TagSize.Primary) : TagType()
    data class Error(override val size: TagSize = TagSize.Primary) : TagType()
}
