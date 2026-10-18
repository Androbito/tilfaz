package com.androbito.tilfaz.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.androbito.tilfaz.core.model.TitleText

/**
 * The catalog tables are a cache, but favourites, watch progress and recent channels share the file and cannot be
 * downloaded again, so schema changes are migrated and never dropped.
 */
object Migrations {
    /** Adds the search text to each catalog row and swaps the indexes for the ones paging and search need. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            for (table in CATALOG_TABLES) {
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `nameNormalized` TEXT NOT NULL DEFAULT ''")
                db.execSQL("DROP INDEX IF EXISTS `index_${table}_categoryId`")
                db.execSQL("DROP INDEX IF EXISTS `index_${table}_name`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_${table}_categoryId_position` ON `$table` (`categoryId`, `position`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_${table}_position` ON `$table` (`position`)")
                fillSearchText(db, table)
            }
        }
    }

    /** Adds the watch list; nothing existing is touched. */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `watchlist` (`key` TEXT NOT NULL, `type` TEXT NOT NULL, `addedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`key`))"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_watchlist_type` ON `watchlist` (`type`)")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)

    private val CATALOG_TABLES = listOf("live_channels", "movies", "series")
    private const val BATCH = 2_000

    /** Batches keep the rows in memory few at a time: a catalog has well over a hundred thousand of them. */
    private fun fillSearchText(db: SupportSQLiteDatabase, table: String) {
        val update = db.compileStatement("UPDATE `$table` SET `nameNormalized` = ? WHERE `id` = ?")
        var lastId = Long.MIN_VALUE
        while (true) {
            val batch = ArrayList<Pair<Long, String>>(BATCH)
            db.query("SELECT `id`, `name` FROM `$table` WHERE `id` > $lastId ORDER BY `id` LIMIT $BATCH").use { cursor ->
                while (cursor.moveToNext()) batch += cursor.getLong(0) to cursor.getString(1)
            }
            if (batch.isEmpty()) break
            for ((id, name) in batch) {
                update.clearBindings()
                update.bindString(1, TitleText.searchKey(name))
                update.bindLong(2, id)
                update.executeUpdateDelete()
            }
            lastId = batch.last().first
        }
        update.close()
    }
}
