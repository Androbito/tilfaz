package com.androbito.tilfaz.core.data.repository

import java.util.Collections

/** Tiny thread-safe LRU, enough to avoid re-fetching details when a user bounces between a list and a detail page. */
internal class LruCache<K, V>(private val maxSize: Int) {
    private val map = Collections.synchronizedMap(
        object : LinkedHashMap<K, V>(maxSize, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean = size > maxSize
        },
    )

    operator fun get(key: K): V? = map[key]
    operator fun set(key: K, value: V) { map[key] = value }
    fun clear() = map.clear()
}
