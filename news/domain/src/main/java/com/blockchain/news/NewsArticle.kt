package com.blockchain.news

data class NewsArticle(
    val id: Int,
    val title: String,
    val image: String,
    val date: String,
    val author: String,
    val link: String,
)
