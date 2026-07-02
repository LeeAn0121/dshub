package com.jongwook.dshub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.jongwook.dshub.data.model.Category
import com.jongwook.dshub.data.model.Stage
import com.jongwook.dshub.data.model.TechSupport
import com.jongwook.dshub.ui.viewmodel.FilterState
import com.jongwook.dshub.ui.viewmodel.MainViewModel
import com.jongwook.dshub.ui.viewmodel.SortOrder
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToDetail: (TechSupport) -> Unit,
    onNavigateToForm: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val entries    by viewModel.filteredEntries.collectAsState()
    val allEntries by viewModel.entries.collectAsState()
    val isLoading  by viewModel.isLoading.collectAsState()
    val error      by viewModel.error.collectAsState()
    val filter     by viewModel.filter.collectAsState()
    val assigneeOptions by viewModel.assigneeOptions.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showFilterSheet by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            current = filter,
            assigneeOptions = assigneeOptions,
            onUpdate = { viewModel.updateFilter { it } },
            onClear = { viewModel.clearFilter() },
            onDismiss = { showFilterSheet = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("DSHub", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                        if (allEntries.isNotEmpty()) {
                            val shown = entries.size
                            val total = allEntries.size
                            Text(
                                text = if (filter.hasAnyFilter) "${shown}/${total}건" else "총 ${total}건",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { viewModel.loadEntries() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                    }
                    BadgedBox(
                        badge = {
                            if (filter.activeCount > 0) {
                                Badge { Text("${filter.activeCount}") }
                            }
                        }
                    ) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "필터")
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "설정")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToForm,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "새 항목 추가", tint = Color.White)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── 검색창 ─────────────────────────────────────────────────────
            OutlinedTextField(
                value = filter.query,
                onValueChange = { q -> viewModel.updateFilter { copy(query = q) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("현장명, 담당, 요청사항, 처리내역, 비고 검색") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (filter.query.isNotBlank()) {
                        IconButton(onClick = { viewModel.updateFilter { copy(query = "") } }) {
                            Icon(Icons.Default.Clear, contentDescription = "지우기")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            // ── 진행단계 칩 (빠른 필터) ─────────────────────────────────────
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = filter.stage == null,
                        onClick = { viewModel.updateFilter { copy(stage = null) } },
                        label = { Text("전체") }
                    )
                }
                items(Stage.entries) { stage ->
                    FilterChip(
                        selected = filter.stage == stage.displayName,
                        onClick = {
                            viewModel.updateFilter {
                                copy(stage = if (filter.stage == stage.displayName) null else stage.displayName)
                            }
                        },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(stage.icon, null, Modifier.size(12.dp))
                                Text(stage.displayName)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = stage.color.copy(alpha = 0.18f),
                            selectedLabelColor = stage.color
                        )
                    )
                }
            }

            // ── 목록 ───────────────────────────────────────────────────────
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (entries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Build, null,
                            Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (!filter.hasAnyFilter) "등록된 기술지원 내역이 없습니다."
                                   else "검색 결과가 없습니다.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                        if (!filter.hasAnyFilter) {
                            Spacer(Modifier.height(6.dp))
                            Text("+ 버튼을 눌러 추가하세요.", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                        } else {
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.clearFilter() }) { Text("필터 초기화") }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(entries, key = { it.rowIndex }) { entry ->
                        TechSupportCard(entry = entry, onClick = { onNavigateToDetail(entry) })
                    }
                }
            }
        }
    }
}

// ── 필터 바텀시트 ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterBottomSheet(
    current: FilterState,
    assigneeOptions: List<String>,
    onUpdate: (FilterState) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var local by remember { mutableStateOf(current) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("검색 필터", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                TextButton(onClick = { local = FilterState(); onClear() }) { Text("초기화") }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── 정렬 ───────────────────────────────────────────────────────
            FilterSection("정렬") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SortOrder.entries.forEach { order ->
                        FilterChip(
                            selected = local.sortOrder == order,
                            onClick = { local = local.copy(sortOrder = order) },
                            label = { Text(order.label, fontSize = 12.sp) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── 구분 ───────────────────────────────────────────────────────
            FilterSection("구분") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = local.category == null,
                        onClick = { local = local.copy(category = null) },
                        label = { Text("전체", fontSize = 12.sp) }
                    )
                    Category.entries.forEach { cat ->
                        FilterChip(
                            selected = local.category == cat.displayName,
                            onClick = {
                                local = local.copy(
                                    category = if (local.category == cat.displayName) null else cat.displayName
                                )
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Icon(cat.icon, null, Modifier.size(12.dp))
                                    Text(cat.displayName, fontSize = 12.sp)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cat.color.copy(alpha = 0.18f),
                                selectedLabelColor = cat.color
                            )
                        )
                    }
                }
            }

            // ── 담당자 ─────────────────────────────────────────────────────
            if (assigneeOptions.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FilterSection("담당자") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = local.assignee == null,
                            onClick = { local = local.copy(assignee = null) },
                            label = { Text("전체", fontSize = 12.sp) }
                        )
                        assigneeOptions.forEach { name ->
                            FilterChip(
                                selected = local.assignee == name,
                                onClick = {
                                    local = local.copy(
                                        assignee = if (local.assignee == name) null else name
                                    )
                                },
                                label = { Text(name, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 적용 버튼
            androidx.compose.material3.Button(
                onClick = {
                    onUpdate(local)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("필터 적용", fontSize = 15.sp, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
    content()
}

// ── 카드 ───────────────────────────────────────────────────────────────────────
@Composable
fun TechSupportCard(entry: TechSupport, onClick: () -> Unit) {
    val stage = Stage.fromDisplayName(entry.stage)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(5.dp).fillMaxHeight().background(stage.color))
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (entry.sequenceNumber > 0) {
                            Text("#${entry.sequenceNumber}", fontSize = 11.sp,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        }
                        Text(
                            text = entry.siteName.ifBlank { "현장명 미입력" },
                            fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    StageBadge(stage = stage)
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        InfoChip(label = entry.category)
                        if (entry.assignee.isNotBlank()) {
                            Text(entry.assignee, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(
                        text = entry.requestDate.ifBlank { entry.registrationDate }.ifBlank { "" },
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.outline
                    )
                }

                if (entry.requestDetails.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = entry.requestDetails,
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun StageBadge(stage: Stage) {
    Surface(color = stage.color.copy(alpha = 0.15f), shape = MaterialTheme.shapes.extraSmall) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(stage.icon, null, tint = stage.color, modifier = Modifier.size(12.dp))
            Text(stage.displayName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = stage.color)
        }
    }
}

@Composable
fun InfoChip(label: String) {
    if (label.isBlank()) return
    val category = Category.fromDisplayName(label)
    Surface(color = category.color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.extraSmall) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(category.icon, null, tint = category.color, modifier = Modifier.size(11.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = category.color)
        }
    }
}
