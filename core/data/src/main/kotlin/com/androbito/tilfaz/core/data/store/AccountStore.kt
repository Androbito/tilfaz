package com.androbito.tilfaz.core.data.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.androbito.tilfaz.core.model.Account
import com.androbito.tilfaz.core.model.AccountStatus
import com.androbito.tilfaz.core.model.StreamFormat
import java.time.Instant
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AccountDataStore

/** Persists the single active account (password encrypted) and the last known account status. */
@Singleton
class AccountStore @Inject constructor(
    @AccountDataStore private val dataStore: DataStore<Preferences>,
    private val secretCodec: SecretCodec
) {
    val account: Flow<Account?> = dataStore.data.map { prefs ->
        val server = prefs[Keys.SERVER] ?: return@map null
        val user = prefs[Keys.USERNAME] ?: return@map null
        val password = prefs[Keys.PASSWORD]?.let(secretCodec::decrypt) ?: return@map null
        Account(server, user, password)
    }.distinctUntilChanged()

    val status: Flow<AccountStatus?> = dataStore.data.map { prefs ->
        val checkedAt = prefs[Keys.STATUS_CHECKED_AT] ?: return@map null
        AccountStatus(
            isActive = prefs[Keys.STATUS_ACTIVE] ?: false,
            expiresAt = prefs[Keys.STATUS_EXPIRES_AT]?.let(Instant::ofEpochMilli),
            isTrial = prefs[Keys.STATUS_TRIAL] ?: false,
            maxConnections = prefs[Keys.STATUS_MAX_CONS] ?: 0,
            activeConnections = prefs[Keys.STATUS_ACTIVE_CONS] ?: 0,
            allowedOutputFormats = prefs[Keys.STATUS_FORMATS]?.split(',')?.mapNotNull(StreamFormat::fromExtension).orEmpty(),
            serverTimeZone = prefs[Keys.STATUS_TIMEZONE],
            checkedAt = Instant.ofEpochMilli(checkedAt)
        )
    }.distinctUntilChanged()

    suspend fun save(account: Account, status: AccountStatus) {
        dataStore.edit { prefs ->
            prefs[Keys.SERVER] = account.serverUrl
            prefs[Keys.USERNAME] = account.username
            prefs[Keys.PASSWORD] = secretCodec.encrypt(account.password)
            prefs.writeStatus(status)
        }
    }

    suspend fun saveStatus(status: AccountStatus) {
        dataStore.edit { it.writeStatus(status) }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.writeStatus(status: AccountStatus) {
        this[Keys.STATUS_ACTIVE] = status.isActive
        status.expiresAt?.let { this[Keys.STATUS_EXPIRES_AT] = it.toEpochMilli() } ?: remove(Keys.STATUS_EXPIRES_AT)
        this[Keys.STATUS_TRIAL] = status.isTrial
        this[Keys.STATUS_MAX_CONS] = status.maxConnections
        this[Keys.STATUS_ACTIVE_CONS] = status.activeConnections
        this[Keys.STATUS_FORMATS] = status.allowedOutputFormats.joinToString(",") { it.extension }
        status.serverTimeZone?.let { this[Keys.STATUS_TIMEZONE] = it } ?: remove(Keys.STATUS_TIMEZONE)
        this[Keys.STATUS_CHECKED_AT] = status.checkedAt.toEpochMilli()
    }

    private object Keys {
        val SERVER = stringPreferencesKey("server_url")
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD = stringPreferencesKey("password_enc")
        val STATUS_ACTIVE = booleanPreferencesKey("status_active")
        val STATUS_EXPIRES_AT = longPreferencesKey("status_expires_at")
        val STATUS_TRIAL = booleanPreferencesKey("status_trial")
        val STATUS_MAX_CONS = intPreferencesKey("status_max_cons")
        val STATUS_ACTIVE_CONS = intPreferencesKey("status_active_cons")
        val STATUS_FORMATS = stringPreferencesKey("status_formats")
        val STATUS_TIMEZONE = stringPreferencesKey("status_timezone")
        val STATUS_CHECKED_AT = longPreferencesKey("status_checked_at")
    }
}
