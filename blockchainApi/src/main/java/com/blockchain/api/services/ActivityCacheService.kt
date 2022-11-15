package com.blockchain.api.services

import com.blockchain.api.selfcustody.activity.ActivityResponse

interface ActivityCacheService {
    fun addOrUpdateActivityItems(items: ActivityResponse)
}
