package com.blockchain.nabu.service

import com.blockchain.domain.tags.TagsService
import java.io.Serializable

class UserTagsRepository : TagsService {
    override fun tags(currentTags: Set<String>): Map<String, Serializable> {
        val isSuperAppMvpTagged = currentTags.contains("superapp_mvp_true")
        return when {
            !isSuperAppMvpTagged ->
                mapOf(
                    "superapp_mvp" to true
                )
            else -> emptyMap()
        }
    }
}
