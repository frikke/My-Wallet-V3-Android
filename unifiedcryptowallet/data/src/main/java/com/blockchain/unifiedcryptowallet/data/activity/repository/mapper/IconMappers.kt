package com.blockchain.unifiedcryptowallet.data.activity.repository.mapper

import com.blockchain.api.selfcustody.activity.ActivityIconDto
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityIcon
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityIconSource

internal fun ActivityIconDto?.toActivityIcon(): ActivityIcon = when (this) {
    is ActivityIconDto.OverlappingPair -> {
        ActivityIcon.OverlappingPair(
            front = ActivityIconSource.Remote(front),
            back = ActivityIconSource.Remote(back)
        )
    }

    is ActivityIconDto.SmallTag -> {
        ActivityIcon.SmallTag(
            main = ActivityIconSource.Remote(main),
            tag = ActivityIconSource.Remote(tag)
        )
    }

    is ActivityIconDto.SingleIcon -> {
        ActivityIcon.SingleIcon(
            icon = ActivityIconSource.Remote(url)
        )
    }

    is ActivityIconDto.Unknown, null -> {
        ActivityIcon.None
    }
}
