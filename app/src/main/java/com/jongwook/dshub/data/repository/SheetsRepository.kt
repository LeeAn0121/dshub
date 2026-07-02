package com.jongwook.dshub.data.repository

import android.content.Context
import com.jongwook.dshub.data.model.TechSupport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.Border
import com.google.api.services.sheets.v4.model.Borders
import com.google.api.services.sheets.v4.model.CellData
import com.google.api.services.sheets.v4.model.CellFormat
import com.google.api.services.sheets.v4.model.Color
import com.google.api.services.sheets.v4.model.CopyPasteRequest
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest
import com.google.api.services.sheets.v4.model.DimensionRange
import com.google.api.services.sheets.v4.model.GridRange
import com.google.api.services.sheets.v4.model.RepeatCellRequest
import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.UpdateBordersRequest
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

    suspend fun getSpreadsheetInfo(spreadsheetId: String): Pair<String, List<String>> =
        withContext(Dispatchers.IO) {
            val response = sheetsService.spreadsheets()
                .get(spreadsheetId)
                .setFields("properties.title,sheets.properties.title")
                .execute()
            val title = response.properties?.title ?: ""
            val tabs  = response.sheets?.map { it.properties.title } ?: emptyList()
            Pair(title, tabs)
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
        // A4부터 A열 전체를 읽어 첫 번째 빈 행 위치 결정
        val colA = sheetsService.spreadsheets().values()
            .get(spreadsheetId, "$sheetName!A4:A")
            .execute()
            .getValues()   // null = 데이터 없음

        val targetRow: Int = if (colA == null) {
            4  // 시트가 완전히 비어있으면 4행부터
        } else {
            val emptyIdx = colA.indexOfFirst { row ->
                row.isEmpty() || row.getOrNull(0)?.toString().isNullOrBlank()
            }
            if (emptyIdx == -1) {
                // 빈 행 없음 → 마지막 데이터 다음 행
                4 + colA.size
            } else {
                4 + emptyIdx
            }
        }

        val seqNum = targetRow - 3   // 행 4 → 순번 1, 행 5 → 순번 2 …
        val sheetId = getSheetId(spreadsheetId, sheetName)
        copyRowFormatting(spreadsheetId, sheetId, targetRow)

        val range = "$sheetName!A${targetRow}:K${targetRow}"
        val body = ValueRange().setValues(listOf(entry.toRowValues(seqNum)))
        sheetsService.spreadsheets().values()
            .update(spreadsheetId, range, body)
            .setValueInputOption("USER_ENTERED")
            .execute()

        applyTableBorders(spreadsheetId, sheetId, lastRow = targetRow)
    }

    suspend fun updateEntry(
        spreadsheetId: String,
        sheetName: String,
        entry: TechSupport
    ) = withContext(Dispatchers.IO) {
        val range = "$sheetName!A${entry.rowIndex}:K${entry.rowIndex}"
        val body = ValueRange().setValues(listOf(entry.toRowValues(entry.sequenceNumber)))
        sheetsService.spreadsheets().values()
            .update(spreadsheetId, range, body)
            .setValueInputOption("USER_ENTERED")
            .execute()

        val sheetId = getSheetId(spreadsheetId, sheetName)
        applyTableBorders(spreadsheetId, sheetId, lastRow = getLastTableRow(spreadsheetId, sheetName))
    }

    suspend fun deleteEntry(
        spreadsheetId: String,
        sheetName: String,
        rowIndex: Int
    ) = withContext(Dispatchers.IO) {
        val sheetId = getSheetId(spreadsheetId, sheetName)
        val zeroRow = rowIndex - 1
        val request = Request().setDeleteDimension(
            DeleteDimensionRequest().setRange(
                DimensionRange()
                    .setSheetId(sheetId)
                    .setDimension("ROWS")
                    .setStartIndex(zeroRow)
                    .setEndIndex(zeroRow + 1)
            )
        )

        sheetsService.spreadsheets()
            .batchUpdate(spreadsheetId, BatchUpdateSpreadsheetRequest().setRequests(listOf(request)))
            .execute()

        renumberEntries(spreadsheetId, sheetName)
        applyTableBorders(spreadsheetId, sheetId, lastRow = getLastTableRow(spreadsheetId, sheetName))
    }

    // ── 신규 행은 기존 시트 행의 서식/데이터 유효성을 그대로 복사 ────────
    private fun copyRowFormatting(spreadsheetId: String, sheetId: Int, targetRow: Int) {
        if (targetRow <= 4) return

        val sourceZeroRow = targetRow - 2
        val targetZeroRow = targetRow - 1
        val source = GridRange()
            .setSheetId(sheetId)
            .setStartRowIndex(sourceZeroRow)
            .setEndRowIndex(sourceZeroRow + 1)
            .setStartColumnIndex(0)
            .setEndColumnIndex(11)
        val destination = GridRange()
            .setSheetId(sheetId)
            .setStartRowIndex(targetZeroRow)
            .setEndRowIndex(targetZeroRow + 1)
            .setStartColumnIndex(0)
            .setEndColumnIndex(11)

        val requests = listOf(
            Request().setCopyPaste(
                CopyPasteRequest()
                    .setSource(source)
                    .setDestination(destination)
                    .setPasteType("PASTE_FORMAT")
                    .setPasteOrientation("NORMAL")
            ),
            Request().setCopyPaste(
                CopyPasteRequest()
                    .setSource(source)
                    .setDestination(destination)
                    .setPasteType("PASTE_DATA_VALIDATION")
                    .setPasteOrientation("NORMAL")
            )
        )

        sheetsService.spreadsheets()
            .batchUpdate(spreadsheetId, BatchUpdateSpreadsheetRequest().setRequests(requests))
            .execute()
    }

    private suspend fun renumberEntries(spreadsheetId: String, sheetName: String) {
        val colA = sheetsService.spreadsheets().values()
            .get(spreadsheetId, "$sheetName!A4:A")
            .execute()
            .getValues()
            ?: return

        if (colA.isEmpty()) return

        val body = ValueRange().setValues((1..colA.size).map { listOf(it) })
        sheetsService.spreadsheets().values()
            .update(spreadsheetId, "$sheetName!A4:A${colA.size + 3}", body)
            .setValueInputOption("RAW")
            .execute()
    }

    private suspend fun getLastTableRow(spreadsheetId: String, sheetName: String): Int {
        val rows = sheetsService.spreadsheets().values()
            .get(spreadsheetId, "$sheetName!A4:K")
            .execute()
            .getValues()
            ?: return 3

        val lastDataIndex = rows.indexOfLast { row ->
            row.any { cell -> cell?.toString()?.isNotBlank() == true }
        }
        return if (lastDataIndex == -1) 3 else lastDataIndex + 4
    }

    private fun applyTableBorders(spreadsheetId: String, sheetId: Int, lastRow: Int) {
        if (lastRow < 3) return

        val border = Border()
            .setStyle("SOLID")
            .setWidth(1)
            .setColor(Color().setRed(0.55f).setGreen(0.59f).setBlue(0.65f))
        val borders = Borders()
            .setTop(border)
            .setBottom(border)
            .setLeft(border)
            .setRight(border)

        val range = GridRange()
            .setSheetId(sheetId)
            .setStartRowIndex(2)      // row 3, header
            .setEndRowIndex(lastRow)  // exclusive; lastRow is 1-based
            .setStartColumnIndex(0)
            .setEndColumnIndex(11)

        val requests = listOf(
            Request().setUpdateBorders(
                UpdateBordersRequest()
                    .setRange(range)
                    .setTop(border)
                    .setBottom(border)
                    .setLeft(border)
                    .setRight(border)
                    .setInnerVertical(border)
                    .setInnerHorizontal(border)
            ),
            Request().setRepeatCell(
                RepeatCellRequest()
                    .setRange(range)
                .setCell(CellData().setUserEnteredFormat(CellFormat().setBorders(borders)))
                .setFields("userEnteredFormat.borders")
            )
        )

        sheetsService.spreadsheets()
            .batchUpdate(spreadsheetId, BatchUpdateSpreadsheetRequest().setRequests(requests))
            .execute()
    }
}
