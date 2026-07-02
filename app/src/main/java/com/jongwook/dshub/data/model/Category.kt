package com.jongwook.dshub.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class Category(
    val displayName: String,
    val color: Color,
    val sheetColorHex: Long,
    val icon: ImageVector
) {
    FAULT_HANDLING(  "장애처리", Color(0xFFF44336), 0xFFF44336, Icons.Filled.Warning),
    TECH_SUPPORT(    "기술지원", Color(0xFF2196F3), 0xFF2196F3, Icons.Filled.Build),
    RELOCATION(      "이전설치", Color(0xFFFF9800), 0xFFFF9800, Icons.Filled.SwapHoriz),
    NEW_INSTALLATION("신규설치", Color(0xFF4CAF50), 0xFF4CAF50, Icons.Filled.AddCircle),
    INSPECTION(      "점검",    Color(0xFF9C27B0), 0xFF9C27B0, Icons.Filled.FactCheck),
    REMOVAL(         "철거",    Color(0xFF9E9E9E), 0xFF9E9E9E, Icons.Filled.Delete),
    NEW_DEVELOPMENT( "신규개발", Color(0xFF009688), 0xFF009688, Icons.Filled.Code);

    companion object {
        fun fromDisplayName(name: String) = entries.find { it.displayName == name } ?: TECH_SUPPORT
        fun displayNames() = entries.map { it.displayName }
    }
}
