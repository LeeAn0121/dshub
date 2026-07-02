package com.jongwook.dshub.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jongwook.dshub.data.model.TechSupport
import com.jongwook.dshub.data.repository.PreferencesRepository
import com.jongwook.dshub.data.repository.SheetsRepository
import com.jongwook.dshub.data.repository.SpreadsheetItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOrder(val label: String) {
    NEWEST("등록일 최신순"),
    OLDEST("등록일 오래된순"),
    SITE_NAME("현장명 가나다순"),
    SEQ_ASC("순번 오름차순")
}

data class FilterState(
    val query: String        = "",
    val stage: String?       = null,
    val category: String?    = null,
    val assignee: String?    = null,
    val sortOrder: SortOrder = SortOrder.NEWEST
) {
    val activeCount: Int get() = listOfNotNull(stage, category, assignee).size
    val hasAnyFilter: Boolean get() = query.isNotBlank() || activeCount > 0
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val prefsRepo = PreferencesRepository(application)

    private var sheetsRepo: SheetsRepository? = null

    // ── 인증 상태 ────────────────────────────────────────────────────────────
    private val _isAuthChecking = MutableStateFlow(true)   // 앱 시작 시 자동 로그인 확인 중
    val isAuthChecking: StateFlow<Boolean> = _isAuthChecking.asStateFlow()

    fun setAuthCheckDone() { _isAuthChecking.value = false }

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // ── 데이터 ───────────────────────────────────────────────────────────────
    private val _entries = MutableStateFlow<List<TechSupport>>(emptyList())
    val entries: StateFlow<List<TechSupport>> = _entries.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ── 검색 / 필터 / 정렬 ───────────────────────────────────────────────────
    private val _filter = MutableStateFlow(FilterState())
    val filter: StateFlow<FilterState> = _filter.asStateFlow()

    /** 현재 데이터에 존재하는 담당자 목록 (필터 옵션용) */
    val assigneeOptions: StateFlow<List<String>> = _entries
        .combine(_entries) { entries, _ ->
            entries.map { it.assignee }.filter { it.isNotBlank() }.distinct().sorted()
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredEntries: StateFlow<List<TechSupport>> = combine(
        _entries, _filter
    ) { entries, f ->
        entries
            .filter { entry ->
                (f.stage    == null || entry.stage    == f.stage) &&
                (f.category == null || entry.category == f.category) &&
                (f.assignee == null || entry.assignee == f.assignee) &&
                (f.query.isBlank() ||
                    entry.siteName.contains(f.query, ignoreCase = true) ||
                    entry.assignee.contains(f.query, ignoreCase = true) ||
                    entry.requestDetails.contains(f.query, ignoreCase = true) ||
                    entry.category.contains(f.query, ignoreCase = true) ||
                    entry.notes.contains(f.query, ignoreCase = true) ||
                    entry.processDetails.contains(f.query, ignoreCase = true))
            }
            .let { list ->
                when (f.sortOrder) {
                    SortOrder.NEWEST    -> list.sortedByDescending { it.registrationDate }
                    SortOrder.OLDEST    -> list.sortedBy { it.registrationDate }
                    SortOrder.SITE_NAME -> list.sortedBy { it.siteName }
                    SortOrder.SEQ_ASC   -> list.sortedBy { it.sequenceNumber }
                }
            }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateFilter(update: FilterState.() -> FilterState) {
        _filter.value = _filter.value.update()
    }
    fun clearFilter() { _filter.value = FilterState() }

    // ── 레포지토리 초기화 ─────────────────────────────────────────────────────
    fun initRepository(repo: SheetsRepository) {
        sheetsRepo = repo
        _isInitialized.value = true
        _isAuthChecking.value = false
        loadEntries()
    }

    fun clearRepository() {
        sheetsRepo = null
        _isInitialized.value = false
        _entries.value = emptyList()
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────
    fun loadEntries() {
        viewModelScope.launch {
            val spreadsheetId = prefsRepo.spreadsheetId.first()
            if (spreadsheetId.isBlank()) return@launch
            val sheetName = prefsRepo.sheetName.first()
            val repo = sheetsRepo ?: return@launch

            _isLoading.value = true
            _error.value = null
            try {
                _entries.value = repo.getAllEntries(spreadsheetId, sheetName)
            } catch (e: Exception) {
                _error.value = "데이터 로드 실패: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addEntry(entry: TechSupport, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val spreadsheetId = prefsRepo.spreadsheetId.first()
            val sheetName = prefsRepo.sheetName.first()
            val repo = sheetsRepo ?: return@launch

            _isLoading.value = true
            try {
                repo.ensureHeader(spreadsheetId, sheetName)
                repo.addEntry(spreadsheetId, sheetName, entry)
                loadEntries()
                onSuccess()
            } catch (e: Exception) {
                _error.value = "저장 실패: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateEntry(entry: TechSupport, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val spreadsheetId = prefsRepo.spreadsheetId.first()
            val sheetName = prefsRepo.sheetName.first()
            val repo = sheetsRepo ?: return@launch

            _isLoading.value = true
            try {
                repo.updateEntry(spreadsheetId, sheetName, entry)
                loadEntries()
                onSuccess()
            } catch (e: Exception) {
                _error.value = "수정 실패: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteEntry(rowIndex: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val spreadsheetId = prefsRepo.spreadsheetId.first()
            val sheetName = prefsRepo.sheetName.first()
            val repo = sheetsRepo ?: return@launch

            _isLoading.value = true
            try {
                repo.deleteEntry(spreadsheetId, sheetName, rowIndex)
                loadEntries()
                onSuccess()
            } catch (e: Exception) {
                _error.value = "삭제 실패: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() { _error.value = null }

    // ── 스프레드시트 목록 ──────────────────────────────────────────────────
    private val _spreadsheets = MutableStateFlow<List<SpreadsheetItem>>(emptyList())
    val spreadsheets: StateFlow<List<SpreadsheetItem>> = _spreadsheets.asStateFlow()

    private val _sheetTabs = MutableStateFlow<List<String>>(emptyList())
    val sheetTabs: StateFlow<List<String>> = _sheetTabs.asStateFlow()

    private val _isLoadingSheets = MutableStateFlow(false)
    val isLoadingSheets: StateFlow<Boolean> = _isLoadingSheets.asStateFlow()

    fun loadSpreadsheets() {
        val repo = sheetsRepo ?: return
        viewModelScope.launch {
            _isLoadingSheets.value = true
            try {
                _spreadsheets.value = repo.listSpreadsheets()
                _error.value = null
            } catch (e: Exception) {
                _error.value = "시트 목록 로드 실패: ${e.message}"
            } finally {
                _isLoadingSheets.value = false
            }
        }
    }

    fun loadSheetTabs(spreadsheetId: String) {
        val repo = sheetsRepo ?: return
        viewModelScope.launch {
            _isLoadingSheets.value = true
            try {
                _sheetTabs.value = repo.listSheetTabs(spreadsheetId)
            } catch (e: Exception) {
                _sheetTabs.value = emptyList()
            } finally {
                _isLoadingSheets.value = false
            }
        }
    }
}
