package com.androbito.tilfaz.core.model

data class Category(
    val id: String,
    val name: String,
    val type: ContentType,
    val parentId: String? = null
)
