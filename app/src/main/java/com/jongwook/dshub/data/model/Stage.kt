package com.jongwook.dshub.data.model

import androidx.compose.ui.graphics.Color

enum class Stage(val displayName: String, val color: Color) {
    RECEIVED("접수", Color(0xFF9E9E9E)),
    ASSIGNED("담당지정", Color(0xFFFF9800)),
    IN_PROGRESS("처리중", Color(0xFF2196F3)),
    COMPLETED("완료", Color(0xFF4CAF50)),
    ON_HOLD("보류", Color(0xFFFFEB3B)),
    CANCELLED("취소", Color(0xFFF44336));

    companion object {
        fun fromDisplayName(name: String) = entries.find { it.displayName == name } ?: RECEIVED
        fun displayNames() = entries.map { it.displayName }
    }
}
