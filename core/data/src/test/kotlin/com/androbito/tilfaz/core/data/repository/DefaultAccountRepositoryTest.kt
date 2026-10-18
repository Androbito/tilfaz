package com.androbito.tilfaz.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.androbito.tilfaz.core.data.store.AccountStore
import com.androbito.tilfaz.core.data.store.ActiveAccountHolder
import com.androbito.tilfaz.core.data.store.PlaybackSettingsStore
import com.androbito.tilfaz.core.data.store.SecretCodec
import com.androbito.tilfaz.core.database.TilfazDatabase
import com.androbito.tilfaz.core.database.entity.FavoriteEntity
import com.androbito.tilfaz.core.database.entity.MovieEntity
import com.androbito.tilfaz.core.database.entity.RecentChannelEntity
import com.androbito.tilfaz.core.database.entity.WatchProgressEntity
import com.androbito.tilfaz.core.database.entity.WatchlistEntity
import com.androbito.tilfaz.core.model.Account
import com.androbito.tilfaz.core.model.AccountStatus
import com.androbito.tilfaz.core.model.PlaybackSettings
import com.androbito.tilfaz.core.network.XtreamApi
import java.io.File
import java.lang.reflect.Proxy
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Signing out must leave the app exactly as after a fresh install: nothing of the previous account stays on the device. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class DefaultAccountRepositoryTest {
    private lateinit var database: TilfazDatabase
    private lateinit var accountStore: AccountStore
    private lateinit var settingsStore: PlaybackSettingsStore
    private lateinit var repository: DefaultAccountRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        database = Room.inMemoryDatabaseBuilder(context, TilfazDatabase::class.java).allowMainThreadQueries().build()
        accountStore = AccountStore(
            PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(context.cacheDir, "account-${System.nanoTime()}.preferences_pb") }),
            object : SecretCodec {
                override fun encrypt(plain: String) = plain

                override fun decrypt(encoded: String) = encoded
            }
        )
        settingsStore = PlaybackSettingsStore(
            PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(context.cacheDir, "settings-${System.nanoTime()}.preferences_pb") })
        )
        val api = Proxy.newProxyInstance(XtreamApi::class.java.classLoader, arrayOf(XtreamApi::class.java)) { _, _, _ ->
            error("no network in this test")
        } as XtreamApi
        repository = DefaultAccountRepository(
            api,
            accountStore,
            ActiveAccountHolder(accountStore, settingsStore, scope),
            database,
            settingsStore,
            Clock.systemUTC(),
            Dispatchers.Unconfined
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `signing out removes the account, every table and the playback settings`() = runTest {
        accountStore.save(Account("http://tv.example.com:8080", "ayoub", "secret"), AccountStatus(true, null, false, 1, 0, emptyList(), null, Instant.EPOCH))
        settingsStore.update { it.copy(audioLanguage = "ar", subtitleLanguage = "fr", userAgent = "Custom/1.0") }
        database.movieDao().insertAll(listOf(MovieEntity(1, "Film", "film", null, "10", null, null, null, "mkv", 0)))
        database.favoriteDao().insert(FavoriteEntity("vod:1", "VOD", 1))
        database.watchlistDao().insert(WatchlistEntity("vod:2", "VOD", 1))
        database.watchProgressDao().upsert(WatchProgressEntity("vod:1", "VOD", "Film", null, 1, 2, 3, "1"))
        database.recentChannelDao().upsert(RecentChannelEntity(5, 1))

        repository.signOut()

        assertThat(accountStore.account.first()).isNull()
        assertThat(settingsStore.settings.first()).isEqualTo(PlaybackSettings())
        assertThat(database.openHelper.writableDatabase.tableCounts()).isEmpty()
    }

    /** Every table that still holds a row, by name. */
    private fun androidx.sqlite.db.SupportSQLiteDatabase.tableCounts(): List<String> {
        val tables = query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'room_%' AND name != 'android_metadata'"
        ).use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }
        return tables.filter { name -> query("SELECT count(*) FROM `$name`").use { it.moveToFirst() && it.getInt(0) > 0 } }
    }
}
