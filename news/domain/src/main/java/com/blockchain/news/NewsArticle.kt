package com.blockchain.news

import java.util.Date

data class NewsArticle(
    val id: Int,
    val title: String,
    val image: String?,
    val date: Date?,
    val author: String?,
    val link: String
)
