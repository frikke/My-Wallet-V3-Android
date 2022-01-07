package com.blockchain.componentlib.tag

data class TagViewState(
    val value: String,
    val type: TagType
)

enum class TagType {
    Default, InfoAlt, Success, Warning, Error
}
