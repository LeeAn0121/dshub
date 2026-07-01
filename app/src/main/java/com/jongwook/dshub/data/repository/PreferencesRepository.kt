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
        val SPREADSHEET_ID_KEY = stringPreferencesKey("spreadsheet_id")
        val SHEET_NAME_KEY = stringPreferencesKey("sheet_name")
    }

    val spreadsheetId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SPREADSHEET_ID_KEY] ?: ""
    }

    val sheetName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SHEET_NAME_KEY] ?: "기술지원"
    }

    suspend fun saveSpreadsheetId(id: String) {
        context.dataStore.edit { prefs ->
            prefs[SPREADSHEET_ID_KEY] = id
        }
    }

    suspend fun saveSheetName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[SHEET_NAME_KEY] = name
        }
    }
}
