package com.jongwook.dshub.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jongwook.dshub.data.model.Category
import com.jongwook.dshub.data.model.Stage
import com.jongwook.dshub.data.model.TechSupport
import com.jongwook.dshub.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    initialEntry: TechSupport?,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val isEdit = initialEntry != null
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.KOREA) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    var registrationDate by remember { mutableStateOf(initialEntry?.registrationDate ?: dateFormatter.format(Date())) }
    var stage by remember { mutableStateOf(initialEntry?.stage ?: Stage.RECEIVED.displayName) }
    var requestDate by remember { mutableStateOf(initialEntry?.requestDate ?: "") }
    var scheduledDate by remember { mutableStateOf(initialEntry?.scheduledDate ?: "") }
    var completionDate by remember { mutableStateOf(initialEntry?.completionDate ?: "") }
    var category by remember { mutableStateOf(initialEntry?.category ?: Category.TECH_SUPPORT.displayName) }
    var siteName by remember { mutableStateOf(initialEntry?.siteName ?: "") }
    var assignee by remember { mutableStateOf(initialEntry?.assignee ?: "") }
    var requestDetails by remember { mutableStateOf(initialEntry?.requestDetails ?: "") }
    var processDetails by remember { mutableStateOf(initialEntry?.processDetails ?: "") }
    var notes by remember { mutableStateOf(initialEntry?.notes ?: "") }

    var showRegistrationDatePicker by remember { mutableStateOf(false) }
    var showRequestDatePicker by remember { mutableStateOf(false) }
    var showScheduledDatePicker by remember { mutableStateOf(false) }
    var showCompletionDatePicker by remember { mutableStateOf(false) }

    if (showRegistrationDatePicker) {
        DSDatePickerDialog(
            onDismiss = { showRegistrationDatePicker = false },
            onConfirm = { registrationDate = it; showRegistrationDatePicker = false }
        )
    }
    if (showRequestDatePicker) {
        DSDatePickerDialog(
            onDismiss = { showRequestDatePicker = false },
            onConfirm = { requestDate = it; showRequestDatePicker = false }
        )
    }
    if (showScheduledDatePicker) {
        DSDatePickerDialog(
            onDismiss = { showScheduledDatePicker = false },
            onConfirm = { scheduledDate = it; showScheduledDatePicker = false }
        )
    }
    if (showCompletionDatePicker) {
        DSDatePickerDialog(
            onDismiss = { showCompletionDatePicker = false },
            onConfirm = { completionDate = it; showCompletionDatePicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEdit) "기술지원 수정" else "기술지원 등록",
                        fontWeight = FontWeight.Bold
                    )
                },
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            SectionTitle("기본 정보")

            DateField(
                label = "등록일",
                value = registrationDate,
                onClick = { showRegistrationDatePicker = true }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    DSIconDropdownField(
                        label = "진행단계",
                        value = stage,
                        options = Stage.entries.map { Triple(it.displayName, it.icon, it.color) },
                        onSelect = { stage = it }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    DSIconDropdownField(
                        label = "구분",
                        value = category,
                        options = Category.entries.map { Triple(it.displayName, it.icon, it.color) },
                        onSelect = { category = it }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SectionTitle("현장 정보")

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = siteName,
                    onValueChange = { siteName = it },
                    label = { Text("현장명") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                if (siteName.isNotBlank()) {
                    IconButton(
                        onClick = {
                            val uri = Uri.parse("geo:0,0?q=${Uri.encode(siteName)}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "지도에서 보기",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            OutlinedTextField(
                value = assignee,
                onValueChange = { assignee = it },
                label = { Text("담당자") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SectionTitle("일정")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    DateField(
                        label = "요청일",
                        value = requestDate,
                        onClick = { showRequestDatePicker = true }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    DateField(
                        label = "예정일",
                        value = scheduledDate,
                        onClick = { showScheduledDatePicker = true }
                    )
                }
            }

            DateField(
                label = "완료일",
                value = completionDate,
                onClick = { showCompletionDatePicker = true }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SectionTitle("내용")

            OutlinedTextField(
                value = requestDetails,
                onValueChange = { requestDetails = it },
                label = { Text("요청사항") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            OutlinedTextField(
                value = processDetails,
                onValueChange = { processDetails = it },
                label = { Text("처리내역") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("비고") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val entry = TechSupport(
                        rowIndex = initialEntry?.rowIndex ?: -1,
                        registrationDate = registrationDate,
                        stage = stage,
                        requestDate = requestDate,
                        scheduledDate = scheduledDate,
                        completionDate = completionDate,
                        category = category,
                        siteName = siteName,
                        assignee = assignee,
                        requestDetails = requestDetails,
                        processDetails = processDetails,
                        notes = notes
                    )
                    if (isEdit) {
                        viewModel.updateEntry(entry) { onNavigateBack() }
                    } else {
                        viewModel.addEntry(entry) { onNavigateBack() }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isEdit) "수정 저장" else "등록하기",
                    modifier = Modifier.padding(vertical = 4.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 0.5.sp
    )
}

@Composable
fun DateField(label: String, value: String, onClick: () -> Unit) {
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = "날짜 선택",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        Box(modifier = Modifier.matchParentSize().clickable(onClick = onClick))
    }
}

@Composable
fun DSDropdownField(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = "선택",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        )
        Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}

@Composable
fun DSIconDropdownField(
    label: String,
    value: String,
    options: List<Triple<String, ImageVector, androidx.compose.ui.graphics.Color>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.find { it.first == value }

    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            leadingIcon = selected?.let { (_, icon, color) ->
                {
                    Surface(
                        color = color.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(16.dp)
                        )
                    }
                }
            },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = "선택",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        )
        Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (name, icon, color) ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = color.copy(alpha = 0.15f),
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(16.dp)
                                )
                            }
                            Text(
                                text = name,
                                fontWeight = if (name == value) FontWeight.Bold else FontWeight.Normal,
                                color = if (name == value) color else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    onClick = { onSelect(name); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DSDatePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val datePickerState = rememberDatePickerState()
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.KOREA) }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onConfirm(formatter.format(Date(millis)))
                    }
                }
            ) { Text("확인") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
