package com.jongwook.dshub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
    val entries         by viewModel.filteredEntries.collectAsState()
    val allEntries      by viewModel.entries.collectAsState()
    val isLoading       by viewModel.isLoading.collectAsState()
    val error           by viewModel.error.collectAsState()
    val filter          by viewModel.filter.collectAsState()
    val assigneeOptions by viewModel.assigneeOptions.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showFilterSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val isFabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    // 당겨서 새로고침
    val pullState = rememberPullToRefreshState()
    if (pullState.isRefreshing) {
        LaunchedEffect(Unit) { viewModel.loadEntries() }
    }
    LaunchedEffect(isLoading) {
        if (!isLoading && pullState.isRefreshing) pullState.endRefresh()
    }

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
                        Text(
                            "DSHub",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 21.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (allEntries.isNotEmpty()) {
                            val shown = entries.size
                            val total = allEntries.size
                            Text(
                                text = if (filter.hasAnyFilter) "${shown}/${total}건 표시" else "총 ${total}건",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
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
            ExtendedFloatingActionButton(
                onClick = onNavigateToForm,
                expanded = isFabExpanded,
                icon = { Icon(Icons.Default.Add, "새 항목 추가", tint = Color.White) },
                text = {
                    Text("신규 등록", color = Color.White, fontWeight = FontWeight.SemiBold)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 검색창
            OutlinedTextField(
                value = filter.query,
                onValueChange = { q -> viewModel.updateFilter { copy(query = q) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = {
                    Text(
                        "현장명, 담당, 요청사항 검색",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                },
                trailingIcon = {
                    if (filter.query.isNotBlank()) {
                        IconButton(onClick = { viewModel.updateFilter { copy(query = "") } }) {
                            Icon(Icons.Default.Clear, contentDescription = "지우기")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // 진행단계 칩 (빠른 필터)
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = filter.stage == null,
                        onClick = { viewModel.updateFilter { copy(stage = null) } },
                        label = { Text("전체", fontSize = 12.sp) }
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
                                Text(stage.displayName, fontSize = 12.sp)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = stage.color.copy(alpha = 0.18f),
                            selectedLabelColor = stage.color,
                            selectedLeadingIconColor = stage.color
                        )
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // 목록 (당겨서 새로고침 지원)
            Box(
                Modifier
                    .fillMaxSize()
                    .nestedScroll(pullState.nestedScrollConnection)
            ) {
                if (isLoading && allEntries.isEmpty()) {
                    // 최초 로딩만 전체 스피너
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (entries.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Build, null,
                                        Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(
                                text = if (!filter.hasAnyFilter) "등록된 기술지원 내역이 없습니다."
                                       else "검색 결과가 없습니다.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                            if (!filter.hasAnyFilter) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "'신규 등록' 버튼을 눌러 추가하세요.",
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 13.sp
                                )
                            } else {
                                Spacer(Modifier.height(12.dp))
                                TextButton(onClick = { viewModel.clearFilter() }) { Text("필터 초기화") }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(entries, key = { it.rowIndex }) { entry ->
                            TechSupportCard(entry = entry, onClick = { onNavigateToDetail(entry) })
                        }
                    }
                }

                // 새로고침 버튼/수정 등으로 갱신 중일 때 — 목록은 유지하고 상단 진행바만 표시
                if (isLoading && allEntries.isNotEmpty() && !pullState.isRefreshing) {
                    LinearProgressIndicator(
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    )
                }

                PullToRefreshContainer(
                    state = pullState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
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
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("검색 필터", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                TextButton(onClick = { local = FilterState(); onClear() }) { Text("초기화") }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

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

            Spacer(Modifier.height(14.dp))

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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(cat.icon, null, Modifier.size(12.dp))
                                    Text(cat.displayName, fontSize = 12.sp)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cat.color.copy(alpha = 0.18f),
                                selectedLabelColor = cat.color,
                                selectedLeadingIconColor = cat.color
                            )
                        )
                    }
                }
            }

            if (assigneeOptions.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
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

            Spacer(Modifier.height(24.dp))

            androidx.compose.material3.Button(
                onClick = {
                    onUpdate(local)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("필터 적용", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Text(
        title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    content()
}

// ── 카드 ───────────────────────────────────────────────────────────────────────
@Composable
fun TechSupportCard(entry: TechSupport, onClick: () -> Unit) {
    val stage = Stage.fromDisplayName(entry.stage)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 1.dp
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Left stage color bar
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(stage.color)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 13.dp)
            ) {
                // Title row
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
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "#${entry.sequenceNumber}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = entry.siteName.ifBlank { "현장명 미입력" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    StageBadge(stage = stage)
                }

                Spacer(Modifier.height(9.dp))

                // Meta row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        InfoChip(label = entry.category)
                        if (entry.assignee.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Default.Person, null,
                                    Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    entry.assignee,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Text(
                        text = entry.requestDate.ifBlank { entry.registrationDate }.ifBlank { "" },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Request details
                if (entry.requestDetails.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 0.5.dp
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = entry.requestDetails,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StageBadge(stage: Stage) {
    Surface(
        color = stage.color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(stage.icon, null, tint = stage.color, modifier = Modifier.size(12.dp))
            Text(
                stage.displayName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = stage.color
            )
        }
    }
}

@Composable
fun InfoChip(label: String) {
    if (label.isBlank()) return
    val category = Category.fromDisplayName(label)
    Surface(
        color = category.color.copy(alpha = 0.13f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(category.icon, null, tint = category.color, modifier = Modifier.size(11.dp))
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = category.color
            )
        }
    }
}
