package com.blockchain.domain.tags

import java.io.Serializable

interface TagsService {
    fun tags(currentTags: Set<String>): Map<String, Serializable>
}
