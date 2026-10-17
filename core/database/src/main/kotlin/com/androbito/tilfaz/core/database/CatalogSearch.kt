package com.androbito.tilfaz.core.database

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

/**
 * The search over a catalog table, done by the database so the app never has to hold the catalog in memory. It matches the
 * normalised title column ([com.androbito.tilfaz.core.model.TitleText]) and ranks like this, best first:
 * the whole title, a title starting with the typed words, a title starting with the typed letters, a title with a word
 * starting that way, and last a match inside a word. Ties go to the shorter title, then to the provider's order.
 */
object CatalogSearch {
    /** [table] is one of our own table names, never user input; [normalizedQuery] is already normalised, so it is only letters, digits and spaces. */
    fun query(table: String, normalizedQuery: String, limit: Int): SupportSQLiteQuery {
        val tokens = normalizedQuery.split(' ').filter { it.isNotEmpty() }
        require(tokens.isNotEmpty()) { "an empty query matches nothing and must not reach the database" }
        val whole = tokens.joinToString(" ")

        val args = ArrayList<Any>()
        val wordStarts = tokens.joinToString(" AND ") { "(' ' || nameNormalized) LIKE ?" }
        val contains = tokens.joinToString(" AND ") { "nameNormalized LIKE ?" }

        val rank = StringBuilder("CASE WHEN nameNormalized = ? THEN 0 WHEN nameNormalized LIKE ? THEN 1 WHEN nameNormalized LIKE ? THEN 2 WHEN ")
            .append(wordStarts).append(" THEN 3 ELSE 4 END")
        args += whole
        args += "$whole %"
        args += "$whole%"
        tokens.forEach { args += "% $it%" }
        val rankArgs = ArrayList(args)

        val filter = ArrayList<Any>()
        tokens.forEach { filter += "%$it%" }

        val sql = "SELECT * FROM `$table` WHERE nameNormalized != '' AND $contains ORDER BY $rank, LENGTH(nameNormalized), position LIMIT ?"
        // The placeholders appear in the text in this order: filter, then rank, then limit.
        return SimpleSQLiteQuery(sql, (filter + rankArgs + limit).toTypedArray())
    }
}
