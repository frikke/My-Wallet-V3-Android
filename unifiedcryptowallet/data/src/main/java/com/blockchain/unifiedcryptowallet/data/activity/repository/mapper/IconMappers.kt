package com.blockchain.unifiedcryptowallet.data.activity.repository.mapper

import com.blockchain.api.selfcustody.activity.ActivityIconDto
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityIcon

internal fun ActivityIconDto?.toActivityIcon(): ActivityIcon = when (this) {
    is ActivityIconDto.OverlappingPair -> ActivityIcon.OverlappingPair(front = front, back = back)
    is ActivityIconDto.SmallTag -> ActivityIcon.SmallTag(main = main, tag = tag)
    is ActivityIconDto.SingleIcon -> ActivityIcon.SingleIcon(url = url)
    is ActivityIconDto.Unknown, null -> ActivityIcon.None
}
