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

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val prefsRepo = PreferencesRepository(application)

    private var sheetsRepo: SheetsRepository? = null

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _entries = MutableStateFlow<List<TechSupport>>(emptyList())
    val entries: StateFlow<List<TechSupport>> = _entries.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _stageFilter = MutableStateFlow<String?>(null)
    val stageFilter: StateFlow<String?> = _stageFilter.asStateFlow()

    val filteredEntries: StateFlow<List<TechSupport>> = combine(
        _entries, _searchQuery, _stageFilter
    ) { entries, query, stage ->
        entries.filter { entry ->
            val matchesStage = stage == null || entry.stage == stage
            val matchesQuery = query.isBlank() ||
                entry.siteName.contains(query, ignoreCase = true) ||
                entry.assignee.contains(query, ignoreCase = true) ||
                entry.requestDetails.contains(query, ignoreCase = true) ||
                entry.category.contains(query, ignoreCase = true)
            matchesStage && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun initRepository(repo: SheetsRepository) {
        sheetsRepo = repo
        _isInitialized.value = true
        loadEntries()
    }

    fun clearRepository() {
        sheetsRepo = null
        _isInitialized.value = false
        _entries.value = emptyList()
    }

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

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setStageFilter(stage: String?) { _stageFilter.value = stage }
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
