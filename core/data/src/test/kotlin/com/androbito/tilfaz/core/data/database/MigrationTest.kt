package com.androbito.tilfaz.core.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.androbito.tilfaz.core.database.Migrations
import com.androbito.tilfaz.core.database.TilfazDatabase
import java.io.File
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The catalog is only a cache, but favourites, watch progress and recent channels live in the same file and cannot be
 * re-downloaded, so an update must keep them. The version 1 database is built from the schema Room exported for it.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-test.db"

    @Before
    fun cleanUp() {
        context.deleteDatabase(name)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(name)
    }

    private fun createVersion1(fill: (SupportSQLiteDatabase) -> Unit) = createVersion(1, fill)

    private fun createVersion(version: Int, fill: (SupportSQLiteDatabase) -> Unit) {
        val schema = JSONObject(
            File("../database/schemas/com.androbito.tilfaz.core.database.TilfazDatabase/$version.json").readText()
        ).getJSONObject("database")
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    val setup = schema.getJSONArray("setupQueries")
                    for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
                    val entities = schema.getJSONArray("entities")
                    for (i in 0 until entities.length()) {
                        val entity = entities.getJSONObject(i)
                        val table = entity.getString("tableName")
                        db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                        val indices = entity.optJSONArray("indices") ?: continue
                        for (j in 0 until indices.length()) db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                    }
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build()
        )
        helper.writableDatabase.use { fill(it) }
        helper.close()
    }

    private fun openMigrated(): TilfazDatabase =
        Room.databaseBuilder(context, TilfazDatabase::class.java, name).addMigrations(*Migrations.ALL).allowMainThreadQueries().build()

    private fun SupportSQLiteDatabase.strings(sql: String): List<String> = query(sql).use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }

    @Test
    fun `favourites, progress and recent channels survive the update`() {
        createVersion1 { db ->
            db.execSQL("INSERT INTO favorites (`key`, type, addedAtMillis) VALUES ('vod:7', 'VOD', 1000)")
            db.execSQL(
                "INSERT INTO watch_progress (playableKey, type, title, posterUrl, positionMs, durationMs, updatedAtMillis, reference) VALUES ('vod:7', 'VOD', 'Scream 4', 'http://img/p.jpg', 600000, 6000000, 2000, '7')"
            )
            db.execSQL("INSERT INTO recent_channels (channelId, viewedAtMillis) VALUES (101, 3000)")
            db.execSQL("INSERT INTO sync_status (contentType, lastSyncedAtMillis, itemCount) VALUES ('VOD', 4000, 1)")
        }

        val database = openMigrated()
        val db = database.openHelper.writableDatabase

        assertThat(db.strings("SELECT `key` FROM favorites")).containsExactly("vod:7")
        assertThat(db.strings("SELECT title FROM watch_progress")).containsExactly("Scream 4")
        assertThat(db.strings("SELECT channelId FROM recent_channels")).containsExactly("101")
        assertThat(db.strings("SELECT lastSyncedAtMillis FROM sync_status")).containsExactly("4000")
        database.close()
    }

    @Test
    fun `every cached title gets its search text, headings get none`() {
        createVersion1 { db ->
            db.execSQL("INSERT INTO movies VALUES (1, '|FR| Scream 4 (2011)', 'http://img/1.jpg', '10', 7.1, 2011, 5000, 'mkv', 0)")
            db.execSQL("INSERT INTO movies VALUES (2, 'Éléctrique & Co', NULL, '10', NULL, NULL, NULL, 'mp4', 1)")
            db.execSQL("INSERT INTO series VALUES (3, '| The Mysterious Sound! | 2026 | ده صوت', NULL, '20', NULL, NULL, NULL, NULL, NULL, 0)")
            db.execSQL("INSERT INTO live_channels VALUES (4, 1, '|AR| AL JAZEERA HD', NULL, '30', NULL, 0, 0, 0)")
            db.execSQL("INSERT INTO live_channels VALUES (5, 2, '-----| BEIN SPORTS |-----', NULL, '30', NULL, 0, 0, 1)")
        }

        val db = openMigrated().also { it.openHelper.writableDatabase }.openHelper.writableDatabase

        assertThat(db.strings("SELECT nameNormalized FROM movies ORDER BY id")).containsExactly("scream 4 2011", "electrique co")
        assertThat(db.strings("SELECT nameNormalized FROM series")).containsExactly("the mysterious sound 2026 ده صوت")
        assertThat(db.strings("SELECT nameNormalized FROM live_channels ORDER BY id")).containsExactly("al jazeera hd", "")
    }

    @Test
    fun `the old name indexes are gone and the paging ones exist`() {
        createVersion1 { }

        val db = openMigrated().also { it.openHelper.writableDatabase }.openHelper.writableDatabase
        val indexes = db.strings("SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'movies'")

        assertThat(indexes.contains("index_movies_name")).isEqualTo(false)
        assertThat(indexes.contains("index_movies_categoryId_position")).isEqualTo(true)
        assertThat(indexes.contains("index_movies_position")).isEqualTo(true)
    }

    @Test
    fun `the watch list table is added and favourites and progress are left alone`() {
        createVersion(2) { db ->
            db.execSQL("INSERT INTO favorites (`key`, type, addedAtMillis) VALUES ('vod:7', 'VOD', 1000)")
            db.execSQL(
                "INSERT INTO watch_progress (playableKey, type, title, posterUrl, positionMs, durationMs, updatedAtMillis, reference) VALUES ('vod:7', 'VOD', 'Scream 4', NULL, 600000, 6000000, 2000, '7')"
            )
        }

        val database = openMigrated()
        val db = database.openHelper.writableDatabase

        assertThat(db.strings("SELECT count(*) FROM watchlist")).containsExactly("0")
        db.execSQL("INSERT INTO watchlist (`key`, type, addedAtMillis) VALUES ('series:9', 'SERIES', 5000)")
        assertThat(db.strings("SELECT `key` FROM watchlist")).containsExactly("series:9")
        assertThat(db.strings("SELECT `key` FROM favorites")).containsExactly("vod:7")
        assertThat(db.strings("SELECT title FROM watch_progress")).containsExactly("Scream 4")
        database.close()
    }

    @Test
    fun `the date indexes of movies and series now end with the position, and the titles are kept`() {
        createVersion(3) { db ->
            db.execSQL("INSERT INTO movies VALUES (1, 'Old', 'old', NULL, '10', NULL, NULL, 100, 'mkv', 0)")
            db.execSQL("INSERT INTO series VALUES (2, 'Show', 'show', NULL, '20', NULL, NULL, 200, NULL, NULL, 0)")
        }

        val database = openMigrated()
        val db = database.openHelper.writableDatabase

        val movieIndexes = db.strings("SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'movies'")
        val seriesIndexes = db.strings("SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'series'")
        assertThat(movieIndexes.contains("index_movies_addedAtMillis")).isEqualTo(false)
        assertThat(movieIndexes.contains("index_movies_addedAtMillis_position")).isEqualTo(true)
        assertThat(seriesIndexes.contains("index_series_lastModifiedMillis")).isEqualTo(false)
        assertThat(seriesIndexes.contains("index_series_lastModifiedMillis_position")).isEqualTo(true)
        assertThat(db.strings("SELECT name FROM movies")).containsExactly("Old")
        assertThat(db.strings("SELECT name FROM series")).containsExactly("Show")
        database.close()
    }
}
