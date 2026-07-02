package com.jongwook.dshub.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class Stage(
    val displayName: String,
    val color: Color,
    val sheetColorHex: Long,
    val icon: ImageVector
) {
    RECEIVED(   "접수",   Color(0xFF9E9E9E), 0xFF9E9E9E, Icons.Filled.Assignment),
    ASSIGNED(   "담당지정", Color(0xFFFF9800), 0xFFFF9800, Icons.Filled.PersonAdd),
    IN_PROGRESS("처리중", Color(0xFF2196F3), 0xFF2196F3, Icons.Filled.Autorenew),
    COMPLETED(  "완료",   Color(0xFF4CAF50), 0xFF4CAF50, Icons.Filled.CheckCircle),
    ON_HOLD(    "보류",   Color(0xFFFFEB3B), 0xFFFFEB3B, Icons.Filled.PauseCircleFilled),
    CANCELLED(  "취소",   Color(0xFFF44336), 0xFFF44336, Icons.Filled.Cancel);

    companion object {
        fun fromDisplayName(name: String) = entries.find { it.displayName == name } ?: RECEIVED
        fun displayNames() = entries.map { it.displayName }
    }
}
