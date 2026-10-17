package com.androbito.tilfaz.core.data.repository

/** How many items are held between reading the provider's list and saving them. */
internal object CatalogSync {
    const val CHUNK_SIZE = 500
}
