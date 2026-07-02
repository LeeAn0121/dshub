package com.jongwook.dshub.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dshub_prefs")

class PreferencesRepository(private val context: Context) {

    companion object {
        val SPREADSHEET_ID_KEY   = stringPreferencesKey("spreadsheet_id")
        val SHEET_NAME_KEY       = stringPreferencesKey("sheet_name")
        val ACCOUNT_EMAIL_KEY    = stringPreferencesKey("account_email")
        val ACCOUNT_NAME_KEY     = stringPreferencesKey("account_display_name")
        val SPREADSHEET_NAME_KEY = stringPreferencesKey("spreadsheet_name")
    }

    val spreadsheetId: Flow<String>      = context.dataStore.data.map { it[SPREADSHEET_ID_KEY]   ?: "" }
    val sheetName: Flow<String>          = context.dataStore.data.map { it[SHEET_NAME_KEY]        ?: "" }
    val accountEmail: Flow<String>       = context.dataStore.data.map { it[ACCOUNT_EMAIL_KEY]     ?: "" }
    val accountDisplayName: Flow<String> = context.dataStore.data.map { it[ACCOUNT_NAME_KEY]      ?: "" }
    val spreadsheetName: Flow<String>    = context.dataStore.data.map { it[SPREADSHEET_NAME_KEY]  ?: "" }

    suspend fun saveSpreadsheetId(id: String) =
        context.dataStore.edit { it[SPREADSHEET_ID_KEY] = id }

    suspend fun saveSheetName(name: String) =
        context.dataStore.edit { it[SHEET_NAME_KEY] = name }

    suspend fun saveAccount(email: String, displayName: String) =
        context.dataStore.edit {
            it[ACCOUNT_EMAIL_KEY] = email
            it[ACCOUNT_NAME_KEY]  = displayName
        }

    suspend fun saveSpreadsheetName(name: String) =
        context.dataStore.edit { it[SPREADSHEET_NAME_KEY] = name }

    suspend fun clearAccount() =
        context.dataStore.edit {
            it.remove(ACCOUNT_EMAIL_KEY)
            it.remove(ACCOUNT_NAME_KEY)
        }
}
