package com.blockchain.unifiedcryptowallet.data.activity.repository.mapper

import com.blockchain.api.selfcustody.activity.ActivityIconDto
import com.blockchain.image.LogoValue
import com.blockchain.image.LogoValueSource

internal fun ActivityIconDto?.toActivityIcon(): LogoValue = when (this) {
    is ActivityIconDto.OverlappingPair -> {
        LogoValue.OverlappingPair(
            front = LogoValueSource.Remote(front),
            back = LogoValueSource.Remote(back)
        )
    }

    is ActivityIconDto.SmallTag -> {
        LogoValue.SmallTag(
            main = LogoValueSource.Remote(main),
            tag = LogoValueSource.Remote(tag)
        )
    }

    is ActivityIconDto.SingleIcon -> {
        LogoValue.SingleIcon(
            icon = LogoValueSource.Remote(url)
        )
    }

    is ActivityIconDto.Unknown, null -> {
        LogoValue.None
    }
}
