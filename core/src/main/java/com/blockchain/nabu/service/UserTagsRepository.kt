package com.blockchain.nabu.service

import com.blockchain.domain.tags.TagsService
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import java.io.Serializable

class UserTagsRepository(
    private val walletModeService: WalletModeService
) : TagsService {
    override fun tags(currentTags: Set<String>): Map<String, Serializable> {
        val isSuperAppMvpTagged = currentTags.contains("superapp_mvp_true")
        return when {
            isSuperAppMvpTagged && walletModeService.enabledWalletMode() == WalletMode.UNIVERSAL -> mapOf(
                "superapp_mvp" to false
            )
            !isSuperAppMvpTagged && walletModeService.enabledWalletMode() != WalletMode.UNIVERSAL ->
                mapOf(
                    "superapp_mvp" to true
                )
            else -> emptyMap()
        }
    }
}
