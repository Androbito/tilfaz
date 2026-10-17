package com.androbito.tilfaz.core.data.repository

import androidx.paging.PagingConfig

/** One window setting for every catalog grid: about ten rows of four posters at a time, and never more than a few dozen rows held. */
internal object CatalogPaging {
    val config = PagingConfig(pageSize = 40, initialLoadSize = 80, prefetchDistance = 20, maxSize = 240, enablePlaceholders = true)
}
