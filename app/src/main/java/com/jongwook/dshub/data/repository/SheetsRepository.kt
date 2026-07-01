package com.jongwook.dshub.data.repository

import android.content.Context
import com.jongwook.dshub.data.model.Category
import com.jongwook.dshub.data.model.Stage
import com.jongwook.dshub.data.model.TechSupport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.CellData
import com.google.api.services.sheets.v4.model.CellFormat
import com.google.api.services.sheets.v4.model.Color
import com.google.api.services.sheets.v4.model.GridRange
import com.google.api.services.sheets.v4.model.RepeatCellRequest
import com.google.api.services.sheets.v4.model.Request
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
            .setIncludeItemsFromAllDrives(true)
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

    private suspend fun getSheetId(spreadsheetId: String, sheetName: String): Int =
        withContext(Dispatchers.IO) {
            val response = sheetsService.spreadsheets()
                .get(spreadsheetId)
                .setFields("sheets.properties(title,sheetId)")
                .execute()
            response.sheets
                ?.find { it.properties.title == sheetName }
                ?.properties?.sheetId ?: 0
        }

    suspend fun getAllEntries(spreadsheetId: String, sheetName: String): List<TechSupport> =
        withContext(Dispatchers.IO) {
            val response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, "$sheetName!A4:K")
                .execute()

            response.getValues()
                ?.mapIndexed { index, row -> TechSupport.fromRow(rowIndex = index + 4, row = row) }
                ?.filter { it.siteName.isNotBlank() || it.requestDetails.isNotBlank() }
                ?: emptyList()
        }

    suspend fun ensureHeader(spreadsheetId: String, sheetName: String) =
        withContext(Dispatchers.IO) {
            val response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, "$sheetName!A3:K3")
                .execute()

            if (response.getValues().isNullOrEmpty()) {
                val body = ValueRange().setValues(listOf(TechSupport.HEADER))
                sheetsService.spreadsheets().values()
                    .update(spreadsheetId, "$sheetName!A3:K3", body)
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
        val response = sheetsService.spreadsheets().values()
            .append(spreadsheetId, "$sheetName!A:K", body)
            .setValueInputOption("USER_ENTERED")
            .setInsertDataOption("INSERT_ROWS")
            .execute()

        // 추가된 행 번호 파싱 후 셀 색상 적용
        val updatedRange = response.updates?.updatedRange ?: return@withContext
        // 형식: 'SheetName'!A5:K5 → "5" 추출
        val rowIndex = updatedRange
            .substringAfter("!A")
            .substringBefore(":")
            .toIntOrNull() ?: return@withContext

        val sheetId = getSheetId(spreadsheetId, sheetName)
        applyRowColors(spreadsheetId, sheetId, rowIndex, entry)
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

        val sheetId = getSheetId(spreadsheetId, sheetName)
        applyRowColors(spreadsheetId, sheetId, entry.rowIndex, entry)
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

    // ── 셀 배경색 적용 (진행단계 B열, 구분 F열) ──────────────────────────────
    private suspend fun applyRowColors(
        spreadsheetId: String,
        sheetId: Int,
        rowIndex: Int,           // 1-based (Google Sheets 행 번호)
        entry: TechSupport
    ) = withContext(Dispatchers.IO) {
        val zeroRow = rowIndex - 1   // batchUpdate는 0-based

        val stageColor = Stage.fromDisplayName(entry.stage).sheetColorHex.toSheetsColor()
        val categoryColor = Category.fromDisplayName(entry.category).sheetColorHex.toSheetsColor()

        val requests = listOf(
            colorCellRequest(sheetId, zeroRow, colIndex = 1, color = stageColor),    // B열 = 진행단계
            colorCellRequest(sheetId, zeroRow, colIndex = 5, color = categoryColor)  // F열 = 구분
        )

        sheetsService.spreadsheets()
            .batchUpdate(spreadsheetId, BatchUpdateSpreadsheetRequest().setRequests(requests))
            .execute()
    }

    private fun colorCellRequest(
        sheetId: Int,
        zeroRow: Int,
        colIndex: Int,
        color: Color
    ): Request = Request().setRepeatCell(
        RepeatCellRequest()
            .setRange(
                GridRange()
                    .setSheetId(sheetId)
                    .setStartRowIndex(zeroRow)
                    .setEndRowIndex(zeroRow + 1)
                    .setStartColumnIndex(colIndex)
                    .setEndColumnIndex(colIndex + 1)
            )
            .setCell(
                CellData().setUserEnteredFormat(
                    CellFormat().setBackgroundColor(color)
                )
            )
            .setFields("userEnteredFormat.backgroundColor")
    )

    // Long ARGB → Google Sheets Color (R/G/B 0.0~1.0)
    private fun Long.toSheetsColor(): Color {
        val r = ((this shr 16) and 0xFF) / 255f
        val g = ((this shr 8) and 0xFF) / 255f
        val b = (this and 0xFF) / 255f
        return Color().setRed(r).setGreen(g).setBlue(b)
    }
}
