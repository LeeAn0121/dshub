package com.jongwook.dshub.data.model

import androidx.compose.ui.graphics.Color

enum class Category(
    val displayName: String,
    val color: Color,
    val sheetColorHex: Long
) {
    FAULT_HANDLING("장애처리",  Color(0xFFF44336), 0xFFF44336),
    TECH_SUPPORT("기술지원",   Color(0xFF2196F3), 0xFF2196F3),
    RELOCATION("이전설치",     Color(0xFFFF9800), 0xFFFF9800),
    NEW_INSTALLATION("신규설치", Color(0xFF4CAF50), 0xFF4CAF50),
    INSPECTION("점검",         Color(0xFF9C27B0), 0xFF9C27B0),
    REMOVAL("철거",            Color(0xFF9E9E9E), 0xFF9E9E9E),
    NEW_DEVELOPMENT("신규개발", Color(0xFF009688), 0xFF009688);

    companion object {
        fun fromDisplayName(name: String) = entries.find { it.displayName == name } ?: TECH_SUPPORT
        fun displayNames() = entries.map { it.displayName }
    }
}
