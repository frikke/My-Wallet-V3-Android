package com.blockchain.api.news

import kotlinx.serialization.Serializable

@Serializable
data class NewsArticlesDto(
    val articles: List<Article>
)

@Serializable
data class Article(
    val id: Int,
    val title: String,
    val image: String?,
    val date: String,
    val author: String?,
    val link: String,
    val assets: List<String>,
    val category: String,
    val datasource: String,
    val logo: String?,
    val source: String? = null
)
