package com.blockchain.preferences

interface IterableAnnouncementsPrefs {
    fun markAsSeen(id: String)
    fun markAsDeleted(id: String)

    fun seenAnnouncements(): List<String>
    fun deletedAnnouncements(): List<String>

    /**
     * update seen list with the new one from api
     */
    fun updateSeenAnnouncements(ids: List<String>)

    /**
     * discard any deleted announcements that api didn't return
     */
    fun syncDeletedAnnouncements(allAnnouncements: List<String>)
}
