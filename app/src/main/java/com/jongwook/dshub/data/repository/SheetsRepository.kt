package com.jongwook.dshub.data.repository

import android.content.Context
import com.jongwook.dshub.data.model.TechSupport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SpreadsheetItem(val id: String, val name: String)

class SheetsRepository(
    context: Context,
    private val credential: GoogleAccountCredential
) {
    private val transport = NetHttpTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()

    private val sheetsService: Sheets = Sheets.Builder(transport, jsonFactory, credential)
        .setApplicationName("DSHub")
        .build()

    private val driveService: Drive = Drive.Builder(transport, jsonFactory, credential)
        .setApplicationName("DSHub")
        .build()

    suspend fun listSpreadsheets(): List<SpreadsheetItem> = withContext(Dispatchers.IO) {
        val result = driveService.files().list()
            .setQ("mimeType='application/vnd.google-apps.spreadsheet' and trashed=false")
            .setFields("files(id, name)")
            .setOrderBy("modifiedTime desc")
            .setPageSize(100)
            .setIncludeItemsFromAllDrives(true)   // 공유 드라이브 포함
            .setSupportsAllDrives(true)
            .execute()
        result.files?.map { SpreadsheetItem(it.id, it.name) } ?: emptyList()
    }

    suspend fun listSheetTabs(spreadsheetId: String): List<String> = withContext(Dispatchers.IO) {
        val response = sheetsService.spreadsheets()
            .get(spreadsheetId)
            .setFields("sheets.properties.title")
            .execute()
        response.sheets?.map { it.properties.title } ?: emptyList()
    }

    suspend fun getAllEntries(spreadsheetId: String, sheetName: String): List<TechSupport> =
        withContext(Dispatchers.IO) {
            val response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, "$sheetName!A2:K")
                .execute()

            response.getValues()?.mapIndexed { index, row ->
                TechSupport.fromRow(rowIndex = index + 2, row = row)
            } ?: emptyList()
        }

    suspend fun ensureHeader(spreadsheetId: String, sheetName: String) =
        withContext(Dispatchers.IO) {
            val response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, "$sheetName!A1:K1")
                .execute()

            if (response.getValues().isNullOrEmpty()) {
                val body = ValueRange().setValues(listOf(TechSupport.HEADER))
                sheetsService.spreadsheets().values()
                    .update(spreadsheetId, "$sheetName!A1:K1", body)
                    .setValueInputOption("RAW")
                    .execute()
            }
        }

    suspend fun addEntry(
        spreadsheetId: String,
        sheetName: String,
        entry: TechSupport
    ) = withContext(Dispatchers.IO) {
        val body = ValueRange().setValues(listOf(entry.toRowValues()))
        sheetsService.spreadsheets().values()
            .append(spreadsheetId, "$sheetName!A:K", body)
            .setValueInputOption("USER_ENTERED")
            .setInsertDataOption("INSERT_ROWS")
            .execute()
    }

    suspend fun updateEntry(
        spreadsheetId: String,
        sheetName: String,
        entry: TechSupport
    ) = withContext(Dispatchers.IO) {
        val range = "$sheetName!A${entry.rowIndex}:K${entry.rowIndex}"
        val body = ValueRange().setValues(listOf(entry.toRowValues()))
        sheetsService.spreadsheets().values()
            .update(spreadsheetId, range, body)
            .setValueInputOption("USER_ENTERED")
            .execute()
    }

    suspend fun deleteEntry(
        spreadsheetId: String,
        sheetName: String,
        rowIndex: Int
    ) = withContext(Dispatchers.IO) {
        val range = "$sheetName!A${rowIndex}:K${rowIndex}"
        val body = ValueRange().setValues(listOf(List(11) { "" }))
        sheetsService.spreadsheets().values()
            .update(spreadsheetId, range, body)
            .setValueInputOption("RAW")
            .execute()
    }
}
