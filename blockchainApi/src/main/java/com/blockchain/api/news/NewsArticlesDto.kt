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
    val image: String? = null,
    val date: String,
    val author: String? = null,
    val link: String,
    val assets: List<String>,
    val category: String,
    val datasource: String,
    val logo: String? = null,
    val source: String? = null
)
