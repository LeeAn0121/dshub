package com.jongwook.dshub.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jongwook.dshub.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val savedSpreadsheetId by viewModel.prefsRepo.spreadsheetId.collectAsState(initial = "")
    val savedSheetName by viewModel.prefsRepo.sheetName.collectAsState(initial = "기술지원")
    val spreadsheets by viewModel.spreadsheets.collectAsState()
    val sheetTabs by viewModel.sheetTabs.collectAsState()
    val isLoadingSheets by viewModel.isLoadingSheets.collectAsState()

    var showSpreadsheetSheet by remember { mutableStateOf(false) }
    var showTabSheet by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ── 스프레드시트 선택 바텀시트 ─────────────────────────────────────────
    if (showSpreadsheetSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSpreadsheetSheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "스프레드시트 선택",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.loadSpreadsheets() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                    }
                }
                HorizontalDivider()

                if (isLoadingSheets) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                } else if (spreadsheets.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Google Drive에서 스프레드시트를\n찾을 수 없습니다.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn {
                        items(spreadsheets) { item ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = item.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                trailingContent = if (item.id == savedSpreadsheetId) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else null,
                                modifier = Modifier.clickable {
                                    scope.launch {
                                        viewModel.prefsRepo.saveSpreadsheetId(item.id)
                                        viewModel.loadSheetTabs(item.id)
                                        showSpreadsheetSheet = false
                                        snackbarHostState.showSnackbar("\"${item.name}\" 선택됨. 시트(탭)를 선택해주세요.")
                                    }
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }

    // ── 시트 탭 선택 바텀시트 ───────────────────────────────────────────────
    if (showTabSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTabSheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text = "시트(탭) 선택",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
                HorizontalDivider()

                if (isLoadingSheets) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                } else if (sheetTabs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "먼저 스프레드시트를 선택해주세요.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn {
                        items(sheetTabs) { tab ->
                            ListItem(
                                headlineContent = { Text(tab) },
                                trailingContent = if (tab == savedSheetName) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else null,
                                modifier = Modifier.clickable {
                                    scope.launch {
                                        viewModel.prefsRepo.saveSheetName(tab)
                                        showTabSheet = false
                                        snackbarHostState.showSnackbar("\"$tab\" 시트 선택 완료!")
                                        viewModel.loadEntries()
                                    }
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Google Sheets 연동",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "내 Google Drive에서 스프레드시트와 시트(탭)를 선택하세요.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    SelectionRow(
                        label = "스프레드시트",
                        value = spreadsheets.find { it.id == savedSpreadsheetId }?.name
                            ?: if (savedSpreadsheetId.isNotBlank()) "불러오는 중…" else "선택 안 됨",
                        isSelected = savedSpreadsheetId.isNotBlank(),
                        onClick = {
                            if (spreadsheets.isEmpty()) viewModel.loadSpreadsheets()
                            showSpreadsheetSheet = true
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SelectionRow(
                        label = "시트 (탭)",
                        value = savedSheetName.ifBlank { "선택 안 됨" },
                        isSelected = savedSheetName.isNotBlank(),
                        onClick = {
                            if (savedSpreadsheetId.isBlank()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("먼저 스프레드시트를 선택해주세요.")
                                }
                                return@SelectionRow
                            }
                            if (sheetTabs.isEmpty()) viewModel.loadSheetTabs(savedSpreadsheetId)
                            showTabSheet = true
                        }
                    )

                    if (savedSpreadsheetId.isNotBlank() && savedSheetName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.loadEntries()
                                onNavigateBack()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("적용하고 목록 새로고침")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "계정",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Google 계정 로그아웃")
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionRow(
    label: String,
    value: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
